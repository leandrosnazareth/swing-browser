# 📐 DIAGRAMA DE ARQUITETURA PROPOSTA

## Estrutura Atual (Monolítica)

```
┌─────────────────────────────────────────┐
│    SwingBrowserApp.java (900+ linhas)   │
├─────────────────────────────────────────┤
│ • JFrame                                │
│ • WebView/WebEngine                     │
│ • Histórico                             │
│ • Favoritos                             │
│ • Preferências                          │
│ • UI Components                         │
│ • Event Listeners                       │
│ • File I/O                              │
└─────────────────────────────────────────┘
```

**Problemas:**
❌ Muito grande (responsabilidade única violada)\
❌ Difícil testar\
❌ Código duplicado\
❌ Difícil de estender

---

## Arquitetura Proposta (Modular)

### Camadas

```
┌──────────────────────────────────────────────────┐
│               UI Layer                           │
│  ┌────────────┐  ┌───────────┐  ┌────────────┐  │
│  │  Menu      │  │ Toolbars  │  │ Dialogs    │  │
│  │  Components│  │ (Nav Bar) │  │ (Settings) │  │
│  └────────────┘  └───────────┘  └────────────┘  │
└────────────┬───────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────┐
│           Controller/Manager Layer            │
│  ┌───────────────────────────────────────┐   │
│  │  BrowserManager (Orchestrator)        │   │
│  │  - navigateTo()                       │   │
│  │  - createTab()                        │   │
│  │  - closeTab()                         │   │
│  └───────────────────────────────────────┘   │
└────────────┬───────────────────────────────┘
             │
┌────▬───────┼───────────┬──────────────────┐
│    │       │           │                  │
│    ▼       ▼           ▼                  ▼
│ ┌──────────────────────────────────────────┐
│ │  Business Logic Layer (Managers)         │
│ ├──────────────────────────────────────────┤
│ │ ┌──────────────┐  ┌──────────────────┐  │
│ │ │ History      │  │ Bookmarks        │  │
│ │ │ Manager      │  │ Manager          │  │
│ │ └──────────────┘  └──────────────────┘  │
│ │ ┌──────────────┐  ┌──────────────────┐  │
│ │ │ Preferences  │  │ WebEngine        │  │
│ │ │ Manager      │  │ Manager          │  │
│ │ └──────────────┘  └──────────────────┘  │
│ └──────────────────────────────────────────┘
│
└──────────────────────────┬─────────────────┘
                           │
┌──────────────────────────▼──────────────────┐
│           Data/Persistence Layer            │
│  ┌──────────────┐  ┌──────────────────┐   │
│  │ JSON Files   │  │ Java Preferences │   │
│  │ (History,    │  │ (User Settings)  │   │
│  │  Bookmarks)  │  │                  │   │
│  └──────────────┘  └──────────────────┘   │
└─────────────────────────────────────────────┘
```

---

## Fluxo de Dados (Usuário clica em navegação)

```
User Action (clica URL)
        │
        ▼
   URL Bar ◄──── Event Handler
        │
        ▼
   SwingBrowserApp (UI)
        │
        ▼
   BrowserManager.loadUrl(url)
        │
        ├─────────────────┬─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
   WebEngineManager   HistoryManager   PreferencesManager
   .loadInWebView()   .addEntry(url)   (carregar setups)
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
                          ▼
                    WebView renderiza
                          │
                          ▼
                    Notifica listeners
                          │
                          ▼
                    UI atualiza URLs/status
```

---

## Dependências Entre Componentes

```
                    ┌─────────────────────┐
                    │  SwingBrowserApp    │
                    │  (Main JFrame)      │
                    └──────────┬──────────┘
                               │
                 ┌─────────────┼─────────────┐
                 │             │             │
                 ▼             ▼             ▼
        ┌─────────────────┐ ┌───────────────────┐
        │ NavigationPanel │ │   BrowserPanel    │
        │ (Toolbar UI)    │ │ (Tabbedpane)      │
        └────────┬────────┘ └─────────┬─────────┘
                 │                   │
                 └─────────┬─────────┘
                           │
                           ▼
                 ┌──────────────────────┐
                 │ BrowserManager       │
                 │ (Orchestrator)       │
                 └──────────┬───────────┘
                            │
        ┌───────────┬────────┼────────┬──────────┐
        │           │        │        │          │
        ▼           ▼        ▼        ▼          ▼
    ┌────────┐ ┌──────┐ ┌────────┐ ┌────────┐ ┌──────┐
    │History │ │Books │ │Prefs   │ │WebEng  │ │Utils │
    │Manager │ │marks │ │Manager │ │Manager │ │      │
    │        │ │Mgr   │ │        │ │        │ │      │
    └────────┘ └──────┘ └────────┘ └────────┘ └──────┘
        │           │        │        │
        └───────────┴────────┴────────┴──────────┐
                                                 │
                    ┌────────────────────────────┘
                    │
                    ▼
        ┌─────────────────────────┐
        │  Data Store             │
        │ (JSON Files,  Java      │
        │  Preferences)           │
        └─────────────────────────┘
```

---

## Comparação: Antes vs Depois

### ANTES (Monolítica)

```java
public class SwingBrowserApp extends JFrame {
    // 900+ linhas tudo junto
    
    // UI criação
    private void createComponents() { ... } // 50 linhas
    
    // History logic
    private void loadHistory() { ... }      // 15 linhas
    private void addToHistory() { ... }     // 8 linhas
    private void saveHistory() { ... }      // 8 linhas
    
    // Bookmarks logic
    private void loadBookmarks() { ... }    // 15 linhas
    private void showBookmarksMenu() { ... } // 30 linhas
    
    // WebView logic
    private void setupWebView() { ... }     // 60 linhas
    private void setupWebEngineListeners() { ... } // 100 linhas
    
    // Preferences
    private void loadPreferences() { ... }  // 5 linhas
    private String getHomePage() { ... }    // 3 linhas
    
    // TODO: Duplicação, difícil testar, acoplado, ...
}
```

**Teste de uma funcionalidade:**
```java
// ❌ Impossível testar sem criar JFrame inteira
public void testAddToHistory() {
    // Precisa instanciar SwingBrowserApp completa
    SwingBrowserApp app = new SwingBrowserApp(); // ❌ Cria UI também!
    // ... testes lentos e frágeis
}
```

---

### DEPOIS (Modular)

```java
// Componentes separados e testáveis

public class HistoryManager {
    private List<String> history;
    
    public void addEntry(String url) { ... }
    public List<String> getAll() { ... }
    public void clear() { ... }
}

public class BookmarkManager {
    private Set<String> bookmarks;
    
    public void add(String url) { ... }
    public boolean contains(String url) { ... }
    public void remove(String url) { ... }
}

public class BrowserManager {
    private HistoryManager history;
    private BookmarkManager bookmarks;
    
    public void loadUrl(String url) { ... }
    public void createNewTab() { ... }
}
```

**Teste de uma funcionalidade (muito melhor!):**
```java
public class HistoryManagerTest {
    private HistoryManager manager;
    
    @Before
    public void setup() {
        manager = new HistoryManager(); // ✅ Instância simples!
    }
    
    @Test
    public void testAddEntry() {
        manager.addEntry("https://google.com");
        assertEquals(1, manager.getAll().size());
    }
    
    @Test
    public void testNoDuplicates() {
        manager.addEntry("https://google.com");
        manager.addEntry("https://google.com");
        assertEquals(1, manager.getAll().size()); // ✅ Rápido e confiável!
    }
}
```

---

## Exemplo de Migração Prática

### ANTES: Tudo em SwingBrowserApp

```java
public class SwingBrowserApp extends JFrame {
    private List<String> history;
    private Set<String> bookmarks;
    private Preferences prefs;
    
    public SwingBrowserApp() {
        loadHistory();        // Código misturado
        loadBookmarks();      // com setupUI
        loadPreferences();    
        setupUI();            // 
        setupWebView();       
    }
    
    private void loadHistory() {
        try (FileReader reader = new FileReader("browser_history.json")) {
            history = new Gson().fromJson(reader, new TypeToken<List<String>>(){}.getType());
        } catch (IOException e) {
            System.err.println("Erro ao carregar histórico");
        }
    }
    
    private void addToHistory(String url) {
        if (!history.isEmpty() && history.get(history.size() - 1).equals(url)) {
            return;
        }
        history.add(url);
        if (history.size() > 100) {
            history.remove(0);
        }
        // DUPLICAÇÃO: esse código pode estar em outro também!
    }
}
```

### DEPOIS: Separado em Managers

```java
// Classe independente
public class HistoryManager {
    private static final String FILE = "browser_history.json";
    private static final int MAX_SIZE = 100;
    private List<String> history;
    private Gson gson = new Gson();
    
    public HistoryManager() {
        load(); // Carrega automaticamente
    }
    
    public void addEntry(String url) {
        if (!history.isEmpty() && history.get(history.size() - 1).equals(url)) {
            return; // ✅ Lógica centralizada
        }
        history.add(url);
        if (history.size() > MAX_SIZE) {
            history.remove(0); // ✅ Limite aplicado aqui
        }
    }
    
    public List<String> getAll() {
        return new ArrayList<>(history);
    }
    
    public void load() {
        // Lógica de carregamento isolada
    }
    
    public void save() {
        // Lógica de salvamento isolada
    }
}

// SwingBrowserApp agora simples!
public class SwingBrowserApp extends JFrame {
    private HistoryManager historyManager;
    private BookmarkManager bookmarkManager;
    private BrowserManager browserManager;
    
    public SwingBrowserApp() {
        // Injeta dependências
        historyManager = new HistoryManager();
        bookmarkManager = new BookmarkManager();
        browserManager = new BrowserManager(historyManager, bookmarkManager);
        
        setupUI();
        registerListeners();
    }
    
    private void onNavigate(String url) {
        browserManager.loadUrl(url); // ✅ Lógica delegada!
    }
}
```

---

## Padrões de Design Aplicados

| Padrão | Onde | Benefício |
|--------|------|-----------|
| **MVC/MVP** | BrowserManager + Managers | Separação UI/Lógica |
| **Dependency Injection** | BrowserManager recebe managers | Testável, flexível |
| **Facade** | BrowserManager expõe interface simples | Menos acoplamento |
| **Observer** | Listeners de eventos | Reatividade |
| **Singleton** | HistoryManager, etc | Uma instância por app |
| **Strategy** | Persistência (JSON/XML) | Extensível |

---

## Impacto da Refatoração

### Tamanho dos Arquivos

```
ANTES:
SwingBrowserApp.java ─ 900+ linhas ❌

DEPOIS:
SwingBrowserApp.java ─ 150 linhas ✅
NavigationPanel.java ─ 100 linhas ✅
BrowserPanel.java ─ 150 linhas ✅
BrowserManager.java ─ 80 linhas ✅
HistoryManager.java ─ 60 linhas ✅
BookmarkManager.java ─ 60 linhas ✅
... outros ...
```

### Testabilidade

```
ANTES:
// Impossível testar sem criar UI
❌ 0% testável

DEPOIS:
// Cada manager testável isoladamente
HistoryManagerTest.java ✅
BookmarkManagerTest.java ✅
BrowserManagerTest.java ✅
...
✅ ~80% testável
```

### Manutenibilidade

```
ANTES:
Modificar histórico: Procurar em 900 linhas ⏱️⏱️⏱️

DEPOIS:
Modificar histórico: Abrir HistoryManager.java ⏱️
```

---

## Timeline Recomendada

### Sprint 1 (Correções)
- [ ] Remover bugs críticos
- [ ] Duplicação de abas
- [ ] Listeners redundantes

### Sprint 2 (Usabilidade)
- [ ] Pesquisa Google na URL
- [ ] Atalhos de teclado
- [ ] Sugestões de URL

### Sprint 3 (Refatoração Fase 1)
- [ ] Criar HistoryManager
- [ ] Criar BookmarkManager
- [ ] Atualizar SwingBrowserApp

### Sprint 4 (Refatoração Fase 2)
- [ ] Criar BrowserManager
- [ ] Refatorar componentes UI
- [ ] Adicionar injeção de dependência

### Sprint 5 (QA & Deploy)
- [ ] Testes unitários
- [ ] Testes integração
- [ ] Release v2.0

---

## Como Começar a Refatoração

### Passo 1: Crie uma Branch

```bash
git checkout -b refactor/modular-architecture
```

### Passo 2: Crie os Managers

```bash
src/main/java/com/bl/
├── managers/
│   ├── HistoryManager.java  (✅ NOVO)
│   ├── BookmarkManager.java (✅ NOVO)
│   ├── PreferencesManager.java (✅ NOVO)
│   └── BrowserManager.java  (✅ NOVO)
```

### Passo 3: Migre o Código

```bash
1. Copiar lógica de SwingBrowserApp → managers/
2. Criar testes para cada manager
3. Integrar managers em SwingBrowserApp
4. Testar funcionamento completo
5. Fazer commit
```

### Passo 4: Refatore UI

```bash
src/main/java/com/bl/
├── ui/
│   ├── components/
│   │   ├── NavigationPanel.java
│   │   ├── BrowserPanel.java
│   │   └── StatusBar.java
```

### Passo 5: Fazer Merge

```bash
git checkout main
git merge --ff-only refactor/modular-architecture
git tag v2.0
```

