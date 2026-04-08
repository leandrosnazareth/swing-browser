# 🛠️ PLANO DE AÇÃO - Implementação de Melhorias

## Fase 1: Correções Críticas (1-2 dias)

### 1.1 - Remover Duplicação de Abas
**Arquivo:** `SwingBrowserApp.java` (linhas ~720)
**Status:** READY TO IMPLEMENT

```java
// ANTES (ERRADO):
private void setupWebView() {
    // ...
    addNewTab("Nova aba", fxPanel); // REMOVE ESTA LINHA
    addNewTab("Nova aba", fxPanel); // REMOVE ESTA LINHA
}

// DEPOIS (CORRETO):
private void setupWebView() {
    // ...
    addNewTab("Nova aba", fxPanel); // APENAS UMA VEZ
}
```

---

### 1.2 - Consolidar Listeners do WebEngine
**Arquivo:** `SwingBrowserApp.java` (linhas ~530-560 e ~738-780)
**Status:** READY TO IMPLEMENT

**Ação:**
- Remover listeners duplicados em `setupWebView()`
- Manter apenas em `setupWebEngineListeners()`
- Chamar `setupWebEngineListeners(webEngine)` uma única vez

---

### 1.3 - Throttle do Listener de Resize
**Arquivo:** `SwingBrowserApp.java` (linhas ~515-525)

```java
// ANTES:
addComponentListener(new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
        updateNavButtons();
    }
});

// DEPOIS:
private Timer resizeTimer;

addComponentListener(new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
        if (resizeTimer != null) {
            resizeTimer.stop();
        }
        resizeTimer = new Timer(500, ev -> updateNavButtons());
        resizeTimer.setRepeats(false);
        resizeTimer.start();
    }
});
```

---

### 1.4 - Armazenar Referência do WebEngine Atual
**Arquivo:** `SwingBrowserApp.java` (adicionar field)

```java
// Adicionar à classe
private WebEngine currentActiveEngine = null;

// Atualizar em createNewTab():
currentActiveEngine = newWebEngine;

// Atualizar em setupLayout() para tratar mudança de aba:
tabbedPane.addChangeListener(e -> {
    int selectedIndex = tabbedPane.getSelectedIndex();
    if (selectedIndex >= 0) {
        // Atualizar currentActiveEngine para a aba selecionada
        // (necessário refatorar estrutura de dados)
    }
});
```

---

## Fase 2: Melhorias de Usabilidade (3-5 dias)

### 2.1 - Pesquisa Direta na URL
**Arquivo:** `SwingBrowserApp.java` (método `ensureUrlProtocol`)

```java
private String ensureUrlProtocol(String input) {
    input = input.trim();
    
    // Se já tem protocolo
    if (input.startsWith("http://") || input.startsWith("https://")) {
        return input;
    }
    
    // Se parece um URL válido (tem ponto + sem espaços)
    if (input.contains(".") && !input.contains(" ") && 
        !input.contains("?") && isValidDomain(input)) {
        return "https://" + input;
    }
    
    // Senão, buscar no Google
    try {
        String encoded = java.net.URLEncoder.encode(input, "UTF-8");
        return "https://www.google.com/search?q=" + encoded;
    } catch (Exception e) {
        return "https://www.google.com/search?q=" + input;
    }
}

private boolean isValidDomain(String str) {
    String domainPattern = "^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,}$";
    return str.toLowerCase().matches(domainPattern);
}
```

---

### 2.2 - Atalhos de Teclado
**Arquivo:** Nova classe `KeyboardShortcuts.java` ou em `SwingBrowserApp`

```java
private void setupKeyboardShortcuts() {
    KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    
    focus.addKeyEventDispatcher(e -> {
        if (e.isControlDown()) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_T:
                    createNewTab();
                    return true;
                case KeyEvent.VK_W:
                    closeCurrentTab();
                    return true;
                case KeyEvent.VK_L:
                    urlBar.selectAll();
                    urlBar.requestFocus();
                    return true;
                case KeyEvent.VK_H:
                    showHistoryDialog();
                    return true;
                case KeyEvent.VK_B:
                    showBookmarksMenu();
                    return true;
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS:
                    zoomIn();
                    return true;
                case KeyEvent.VK_MINUS:
                    zoomOut();
                    return true;
            }
        }
        return false;
    });
}

private void closeCurrentTab() {
    int index = tabbedPane.getSelectedIndex();
    if (tabbedPane.getTabCount() > 1) {
        tabbedPane.remove(index);
    } else {
        setDefaultPage();
    }
}

private void zoomIn() {
    zoomSlider.setValue(Math.min(200, zoomSlider.getValue() + 10));
}

private void zoomOut() {
    zoomSlider.setValue(Math.max(50, zoomSlider.getValue() - 10));
}
```

---

### 2.3 - Sugestões na Barra de URL
**Arquivo:** Nova classe `URLSuggestionField.java`

```java
public class URLSuggestionField extends JTextField {
    private JPopupMenu suggestionsMenu;
    private List<String> suggestions;
    
    public URLSuggestionField(List<String> history, Set<String> bookmarks) {
        this.suggestions = new ArrayList<>(history);
        this.suggestions.addAll(bookmarks);
        
        getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                showSuggestions();
            }
            public void removeUpdate(DocumentEvent e) {
                showSuggestions();
            }
            public void changedUpdate(DocumentEvent e) {
                showSuggestions();
            }
        });
    }
    
    private void showSuggestions() {
        String text = getText().toLowerCase();
        if (text.length() < 2) return;
        
        suggestionsMenu = new JPopupMenu();
        
        suggestions.stream()
            .filter(s -> s.toLowerCase().contains(text))
            .limit(10)
            .forEach(s -> {
                JMenuItem item = new JMenuItem(s);
                item.addActionListener(e -> setText(s));
                suggestionsMenu.add(item);
            });
        
        if (suggestionsMenu.getComponentCount() > 0) {
            suggestionsMenu.show(this, 0, getHeight());
        }
    }
}
```

---

### 2.4 - Menu Contextual (Clique Direito)
**Arquivo:** Nova classe `WebViewContextMenu.java`

```java
public class WebViewContextMenu extends JPopupMenu {
    private WebEngine engine;
    private String clickedUrl;
    
    public WebViewContextMenu(WebEngine engine) {
        this.engine = engine;
        
        // JavaScript para capturar URL clicada
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                engine.executeScript(
                    "document.body.addEventListener('contextmenu', function(e) {" +
                    "  var href = e.target.href || e.target.src || '';" +
                    "  console.log('CONTEXT_URL:' + href);" +
                    "});"
                );
            }
        });
        
        setupMenu();
    }
    
    private void setupMenu() {
        JMenuItem openNewTab = new JMenuItem("Abrir em nova aba");
        openNewTab.addActionListener(e -> {
            // TODO: Implementar
        });
        add(openNewTab);
        
        JMenuItem copyLink = new JMenuItem("Copiar link");
        copyLink.addActionListener(e -> {
            // TODO: Implementar
        });
        add(copyLink);
        
        JMenuItem saveImage = new JMenuItem("Salvar imagem");
        saveImage.addActionListener(e -> {
            // TODO: Implementar
        });
        add(saveImage);
    }
}
```

---

## Fase 3: Refatoração da Arquitetura (1-2 semanas)

### 3.1 - Estrutura de Pasta Proposta

```
src/main/java/com/bl/
├── SwingBrowserApp.java           (Main frame)
├── config/
│   └── ApplicationConfig.java      (Gerenciar configurações)
├── managers/
│   ├── BrowserManager.java         (Orquestração)
│   ├── HistoryManager.java         (Histórico + persistência)
│   ├── BookmarkManager.java        (Favoritos + persistência)
│   ├── PreferencesManager.java     (Preferências do usuário)
│   └── WebEngineManager.java       (Gerenciar WebEngines)
├── ui/
│   ├── components/
│   │   ├── NavigationPanel.java    (Barra de navegação)
│   │   ├── BrowserPanel.java       (Painel com abas)
│   │   ├── StatusBar.java          (Barra de status)
│   │   └── URLBar.java             (Campo de URL com sugestões)
│   ├── dialogs/
│   │   ├── SettingsDialog.java
│   │   ├── HistoryDialog.java
│   │   └── BookmarksDialog.java
│   └── menu/
│       ├── WebViewContextMenu.java
│       └── MenuBar.java
├── utils/
│   ├── UrlUtils.java
│   ├── FontUtils.java
│   ├── ImageUtils.java
│   └── PlatformUtils.java
└── model/
    └── BrowserState.java           (POJO com dados da app)
```

### 3.2 - Criar `BrowserManager.java`

```java
public class BrowserManager {
    private HistoryManager historyManager;
    private BookmarkManager bookmarkManager;
    private PreferencesManager preferencesManager;
    private WebEngineManager webEngineManager;
    
    private BrowserCallback callback; // Para notificar UI
    
    public BrowserManager() {
        this.historyManager = new HistoryManager();
        this.bookmarkManager = new BookmarkManager();
        this.preferencesManager = new PreferencesManager();
        this.webEngineManager = new WebEngineManager();
    }
    
    public void loadUrl(String url) {
        String formattedUrl = UrlUtils.formatUrl(url);
        webEngineManager.loadUrl(formattedUrl);
        historyManager.addEntry(formattedUrl);
    }
    
    public List<String> getHistory() {
        return historyManager.getAll();
    }
    
    public void addBookmark(String url, String title) {
        bookmarkManager.add(url, title);
    }
    
    public void removeBookmark(String url) {
        bookmarkManager.remove(url);
    }
    
    public void shutdown() {
        historyManager.save();
        bookmarkManager.save();
        preferencesManager.save();
    }
}
```

### 3.3 - Criar `HistoryManager.java`

```java
public class HistoryManager {
    private static final String HISTORY_FILE = "browser_history.json";
    private static final int MAX_HISTORY_SIZE = 100;
    
    private List<String> history;
    private Gson gson = new Gson();
    
    public HistoryManager() {
        load();
    }
    
    public void addEntry(String url) {
        // Não adicionar duplicatas consecutivas
        if (!history.isEmpty() && history.get(history.size() - 1).equals(url)) {
            return;
        }
        
        history.add(url);
        
        // Manter limite
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }
    
    public List<String> getAll() {
        return new ArrayList<>(history);
    }
    
    public void clear() {
        history.clear();
    }
    
    public void load() {
        if (Files.exists(Paths.get(HISTORY_FILE))) {
            try (FileReader reader = new FileReader(HISTORY_FILE)) {
                history = gson.fromJson(reader, new TypeToken<List<String>>(){}.getType());
                if (history == null) history = new ArrayList<>();
            } catch (IOException e) {
                logger.error("Erro ao carregar histórico", e);
                history = new ArrayList<>();
            }
        } else {
            history = new ArrayList<>();
        }
    }
    
    public void save() {
        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
            gson.toJson(history, writer);
        } catch (IOException e) {
            logger.error("Erro ao salvar histórico", e);
        }
    }
}
```

---

## Fase 4: Adicionar Logging e Testes (3-5 dias)

### 4.1 - Adicionar Dependência SLF4J

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.7</version>
</dependency>

<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.8</version>
</dependency>
```

### 4.2 - Criar `logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/browser.log</file>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 4.3 - Adicionar Testes JUnit

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.9.2</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.2.1</version>
    <scope>test</scope>
</dependency>
```

**Exemplo de teste:**

```java
public class HistoryManagerTest {
    private HistoryManager manager;
    
    @BeforeEach
    public void setUp() {
        manager = new HistoryManager();
    }
    
    @Test
    public void testAddEntry() {
        manager.addEntry("https://google.com");
        assertEquals(1, manager.getAll().size());
    }
    
    @Test
    public void testNoDuplicateConsecutive() {
        manager.addEntry("https://google.com");
        manager.addEntry("https://google.com");
        assertEquals(1, manager.getAll().size());
    }
}
```

---

## Fase 5: Preparação para Produção (1 semana)

### 5.1 - Maven Assembly Plugin

```xml
<pluginManagement>
    <plugins>
        <plugin>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.4.2</version>
            <configuration>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
                <archive>
                    <manifest>
                        <mainClass>com.bl.SwingBrowserApp</mainClass>
                    </manifest>
                </archive>
                <finalName>SwingBrowser</finalName>
                <appendAssemblyId>false</appendAssemblyId>
            </configuration>
        </plugin>
    </plugins>
</pluginManagement>
```

### 5.2 - GitHub Actions CI/CD

```yaml
# .github/workflows/build.yml
name: Build and Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          distribution: 'adopt'
      
      - name: Build with Maven
        run: mvn clean package
      
      - name: Create Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
```

---

## 📊 Cronograma Estimado

| Fase | Duração | Prioridade | Versão |
|------|---------|-----------|---------|
| Fase 1 (Críticas) | 1-2 dias | 🔴 ALTA | v1.1 |
| Fase 2 (Usabilidade) | 3-5 dias | 🟠 MÉDIA | v1.2 |
| Fase 3 (Refatoração) | 1-2 semanas | 🟡 MÉDIA | v2.0 |
| Fase 4 (Testes) | 3-5 dias | 🟡 BAIXA | v2.0 |
| Fase 5 (Produção) | 1 semana | 🟡 BAIXA | v2.0 |

---

## ✅ Checklist de Implementação

### Fase 1
- [ ] Remover duplicação de abas
- [ ] Consolidar listeners
- [ ] Implementar throttle de resize
- [ ] Testar inicialização

### Fase 2
- [ ] Pesquisa Google na URL
- [ ] Atalhos de teclado
- [ ] Sugestões na URLbar
- [ ] Menu contextual
- [ ] Testar usabilidade

### Fase 3
- [ ] Criar estrutura de pastas
- [ ] Refatorar em classes menores
- [ ] Implementar managers
- [ ] Atualizar SwingBrowserApp

### Fase 4
- [ ] Adicionar SLF4J
- [ ] Implementar testes
- [ ] Atingir 80%+ cobertura

### Fase 5
- [ ] Configurar Assembly Plugin
- [ ] Criar CI/CD
- [ ] Fazer release v2.0

