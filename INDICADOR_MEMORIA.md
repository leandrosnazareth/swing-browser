# 💾 Indicador de Memória - Java Browser

## O Que Foi Implementado

Adicionei um **monitor de memória em tempo real** na barra de navegação, logo ao lado do slider de zoom. O indicador atualiza a cada 2 segundos e muda de cor baseado no nível de uso.

---

## 🎨 Visual

```
┌─────────────────────────────────────────────────────────────────┐
│  ◀ ▶ ↻ ⌂  URL aqui                              Zoom: 100% │  Mem: 256/1024 MB (25%) │
└─────────────────────────────────────────────────────────────────┘
                                                  ↑ zoom                  ↑ memória
```

---

## 🔴 Cores de Status

| Uso | Cor | Significado |
|-----|-----|-------------|
| 0-70% | 🟢 **Verde** | Normal - Memória disponível |
| 70-85% | 🟠 **Laranja** | Alto - Atenção necessária |
| 85%+ | 🔴 **Vermelho** | Crítico - Pouca memória |

---

## 📝 Mudanças Implementadas

### 1. **Field de Label de Memória** ✅

```java
private JLabel statusLabel, zoomLabel, memoryLabel;
private Thread memoryUpdateThread;
```

Adicionado:
- `memoryLabel` - Para exibir informações de memória
- `memoryUpdateThread` - Thread daemon para atualizar continuamente

---

### 2. **UI - Painel de Memória** ✅

```java
// Painel de memória
JPanel memoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
memoryLabel = new JLabel("Memória: -- MB");
memoryLabel.setFont(deriveFont(memoryLabel.getFont()));
memoryLabel.setToolTipText("Uso de memória do navegador");
memoryPanel.add(memoryLabel);

// Layout combinado
JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
infoPanel.add(zoomPanel);      // Zoom à esquerda
infoPanel.add(memoryPanel);    // Memória à direita
rightPanel.add(infoPanel, BorderLayout.EAST);
```

**Benefício:** Ambos os indicadores (zoom e memória) ficam lado a lado na direita da barra de navegação.

---

### 3. **Método: startMemoryMonitor()** ✅

```java
private void startMemoryMonitor() {
    memoryUpdateThread = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                SwingUtilities.invokeLater(this::updateMemoryLabel);
                Thread.sleep(2000); // Atualizar a cada 2 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }, "MemoryMonitor");
    memoryUpdateThread.setDaemon(true);
    memoryUpdateThread.start();
}
```

**Como funciona:**
1. Cria uma thread daemon (termina com a app)
2. A cada 2 segundos, atualiza a label
3. Usa `SwingUtilities.invokeLater()` para thread-safety

---

### 4. **Método: updateMemoryLabel()** ✅

```java
private void updateMemoryLabel() {
    if (memoryLabel != null) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024); // Em MB
        long maxMemory = runtime.maxMemory() / (1024 * 1024); // Em MB
        
        // Calcular percentual de uso
        int percentUsed = (int) ((usedMemory * 100) / maxMemory);
        
        String memoryText = String.format("Mem: %d/%d MB (%d%%)", usedMemory, maxMemory, percentUsed);
        memoryLabel.setText(memoryText);
        
        // Mudar cor baseado no uso
        if (percentUsed > 85) {
            memoryLabel.setForeground(new Color(220, 20, 20)); // Vermelho - crítico
        } else if (percentUsed > 70) {
            memoryLabel.setForeground(new Color(255, 165, 0)); // Laranja - alto
        } else {
            memoryLabel.setForeground(new Color(100, 200, 100)); // Verde - normal
        }
    }
}
```

**Funcionalidade:**
- `runtime.totalMemory()` = Total alocado pela JVM
- `runtime.freeMemory()` = Libre disponível
- `usedMemory` = Diferença (em uso)
- `runtime.maxMemory()` = Máximo que a JVM pode usar
- Percentual = (usado / máximo) × 100
- Cor muda automaticamente conforme o uso

---

### 5. **Inicia Monitor no Construtor** ✅

```java
public SwingBrowserApp() {
    // ... outros códigos ...
    updateComponentSizes();
    startMemoryMonitor(); // ← Adicionado
}
```

Garantir que o monitor inicie quando a aplicação é criada.

---

## 📊 Exemplo de Output

```
Mem: 128/2048 MB (6%)     ← Verde (normal)
Mem: 1350/2048 MB (66%)   ← Verde (normal)
Mem: 1450/2048 MB (71%)   ← Laranja (alto)
Mem: 1750/2048 MB (85%)   ← Vermelho (crítico)
Mem: 1900/2048 MB (93%)   ← Vermelho (crítico)
```

---

## 🔧 Detalhes Técnicos

| Aspecto | Valor |
|---------|-------|
| **Taxa de atualização** | 2 segundos |
| **Tipo de thread** | Daemon (não bloqueia shutdown) |
| **Thread-safety** | `SwingUtilities.invokeLater()` ✅ |
| **Precisão** | MB (1/1024 da memória) |
| **Nome da thread** | "MemoryMonitor" |

---

## 🎯 Exemplos de Uso

### Quando usar o indicador

✅ **Bom para:**
- Detectar vazamento de memória
- Monitorar consumo de abas múltiplas
- Alertar quando memória está crítica
- Saber quando fechar abas/reiniciar

❌ **Não faz:**
- Liberar memória automaticamente
- Forçar garbage collection
- Limpar cache

---

## 🧹 Limpeza Automática

A thread é daemon, então desaparece quando a app encerra:

```java
memoryUpdateThread.setDaemon(true);  // ← Termina com a app
memoryUpdateThread.start();
```

Se precisar interromper manualmente:
```java
// Adicionar ao WindowAdapter:
addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(WindowEvent e) {
        if (memoryUpdateThread != null) {
            memoryUpdateThread.interrupt();
        }
        // ... outros cleanups ...
    }
});
```

---

## 📈 Possíveis Melhorias (Futuro)

1. **Gráfico de memória** - Mostrar histórico com JFreeChart
2. **Botão de Garbage Collection manual** - `System.gc()`
3. **Alertas** - Notificação quando crítico
4. **Limite configurável** - Permitir ajustar cores/limiares
5. **Log de memória** - Salvar histórico em arquivo

---

## ✅ Compilação

```
[INFO] BUILD SUCCESS
```

✅ **Sem erros!**

---

## 📁 Arquivos Modificados

- `SwingBrowserApp.java`
  - ✅ Adicionado: `memoryLabel` field
  - ✅ Adicionado: `memoryUpdateThread` field
  - ✅ Adicionado: `startMemoryMonitor()` método
  - ✅ Adicionado: `updateMemoryLabel()` método
  - ✅ Modificado: `setupLayout()` - adicionado painel de memória
  - ✅ Modificado: Construtor - chamada a `startMemoryMonitor()`
  - ✅ Adicionado: Import `java.awt.Color`

---

## 🚀 Como Testar

1. Compilar: `mvn clean compile`
2. Correr: `mvn exec:java -Dexec.mainClass="com.bl.SwingBrowserApp"`
3. Abrir várias abas
4. Observar o indicador de memória
5. Mudar cores conforme a memória aumenta

---

## 💡 Dica

Se a memória ficar vermelha por muito tempo, considere:
- Fechar abas não utilizadas
- Reiniciar o navegador
- Aumentar heap da JVM com `-Xmx2048m`

