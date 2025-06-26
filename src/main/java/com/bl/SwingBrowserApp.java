package com.bl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class SwingBrowserApp extends JFrame {
    private static final String APP_NAME = "Java Browser";
    private static final String DEFAULT_HOME_PAGE = "https://www.google.com";
    private static final String HISTORY_FILE = "browser_history.json";
    private static final String BOOKMARKS_FILE = "browser_bookmarks.json";
    private static final String PREFERENCES_NODE = "com.bl.advancedbrowser";

    // Componentes da UI
    private JFXPanel fxPanel;
    private WebView webView;
    private WebEngine webEngine;
    private JTextField urlBar;
    private JButton goButton, backButton, forwardButton, refreshButton;
    private JButton homeButton, bookmarksButton, historyButton, settingsButton;
    private JProgressBar progressBar;
    private JLabel statusLabel, zoomLabel;
    private JTabbedPane tabbedPane;
    private JSlider zoomSlider;

    // Dados
    private List<String> history = new ArrayList<>();
    private Set<String> bookmarks = new LinkedHashSet<>();
    private Preferences prefs;

    // Configurações de dimensionamento
    private float scalingFactor = 1.0f;
    private double currentZoom = 1.0;

    public SwingBrowserApp() {
        loadPreferences();
        configureScaling();
        configureWindow();
        loadData();
        setupLookAndFeel();
        createComponents();
        setupLayout();
        setupListeners();
        setupWebView();
        loadInitialPage();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        updateComponentSizes();
    }

    private void configureScaling() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        scalingFactor = Math.max(0.8f, Math.min(1.5f, screenWidth / 1920f));

        if (screenWidth > 3000) {
            scalingFactor = 1.3f;
        }

        // Carrega o zoom salvo nas preferências, só se prefs já foi inicializado
        if (prefs != null) {
            currentZoom = prefs.getDouble("zoomLevel", 1.0);
        } else {
            currentZoom = 1.0; // Valor padrão se prefs ainda não estiver disponível
        }
    }

    private void configureWindow() {
        setTitle(APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Tamanho inicial baseado no fator de escala
        int initialWidth = (int) (1200 * scalingFactor);
        int initialHeight = (int) (800 * scalingFactor);
        setSize(initialWidth, initialHeight);

        setLocationRelativeTo(null);
        setMinimumSize(new Dimension((int) (800 * scalingFactor), (int) (600 * scalingFactor)));

        // Tenta carregar o ícone
        try {
            setIconImage(loadAppIcon());
        } catch (Exception e) {
            System.err.println("Erro ao carregar ícone: " + e.getMessage());
        }
    }

    private java.awt.Image loadAppIcon() throws IOException {
        try (InputStream iconStream = getClass().getResourceAsStream("/image/iconeb2.png")) {
            if (iconStream != null) {
                return ImageIO.read(iconStream);
            }
        }
        return null;
    }

    private void loadPreferences() {
        prefs = Preferences.userRoot().node(PREFERENCES_NODE);
    }

    private void loadData() {
        loadHistory();
        loadBookmarks();
    }

    private void loadHistory() {
        if (Files.exists(Paths.get(HISTORY_FILE))) {
            try (FileReader reader = new FileReader(HISTORY_FILE)) {
                history = new Gson().fromJson(reader, new TypeToken<List<String>>() {
                }.getType());
            } catch (IOException e) {
                System.err.println("Erro ao carregar histórico: " + e.getMessage());
            }
        }
        if (history == null) {
            history = new ArrayList<>();
        }
    }

    private void loadBookmarks() {
        if (Files.exists(Paths.get(BOOKMARKS_FILE))) {
            try (FileReader reader = new FileReader(BOOKMARKS_FILE)) {
                bookmarks = new Gson().fromJson(reader, new TypeToken<LinkedHashSet<String>>() {
                }.getType());
            } catch (IOException e) {
                System.err.println("Erro ao carregar favoritos: " + e.getMessage());
            }
        }
        if (bookmarks == null) {
            bookmarks = new LinkedHashSet<>();
        }
    }

    private void setupLookAndFeel() {
        try {
            FlatDarkLaf.setup();
            updateUIFonts();
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
    }

    private void updateUIFonts() {
        Font baseFont = UIManager.getFont("Button.font");
        Font scaledFont = deriveFont(baseFont);

        UIManager.put("Button.font", scaledFont);
        UIManager.put("Label.font", scaledFont);
        UIManager.put("TextField.font", scaledFont);
        UIManager.put("TabbedPane.font", scaledFont);
        UIManager.put("ComboBox.font", scaledFont);
        UIManager.put("List.font", scaledFont);
        UIManager.put("MenuItem.font", scaledFont);
        UIManager.put("PopupMenu.font", scaledFont);
    }

    private Font deriveFont(Font baseFont) {
        return baseFont.deriveFont(baseFont.getSize() * scalingFactor);
    }

    private void createComponents() {
        // Barra de URL com fonte dimensionada
        urlBar = new JTextField();
        urlBar.setFont(deriveFont(urlBar.getFont()));
        urlBar.setToolTipText("Digite a URL e pressione Enter");

        // Botões de navegação com ícones dimensionados
        backButton = createScaledButton("◀", "Voltar");
        forwardButton = createScaledButton("▶", "Avançar");
        refreshButton = createScaledButton("↻", "Recarregar");
        homeButton = createScaledButton("⌂", "Página inicial");
        goButton = createScaledButton("Ir", "Ir para a URL");

        // Botões de menu
        bookmarksButton = createScaledButton("★", "Favoritos");
        historyButton = createScaledButton("≡", "Histórico");
        settingsButton = createScaledButton("⚙", "Configurações");

        // Barra de progresso e status
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setFont(deriveFont(progressBar.getFont()));

        statusLabel = new JLabel("Pronto");
        statusLabel.setFont(deriveFont(statusLabel.getFont()));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // Painel de abas
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(deriveFont(tabbedPane.getFont()));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    private JButton createScaledButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusable(false);
        button.setFont(deriveFont(button.getFont()));

        // Aumenta o tamanho preferido baseado no fator de escala
        Dimension size = new Dimension(
                (int) (button.getPreferredSize().width * scalingFactor * 1.2),
                (int) (button.getPreferredSize().height * scalingFactor * 1.5));
        button.setPreferredSize(size);

        return button;
    }

    private void setupLayout() {
        // Painel superior com barra de navegação
        JPanel navPanel = new JPanel(new BorderLayout(5, 5));
        navPanel.setBorder(BorderFactory.createEmptyBorder(
                (int) (5 * scalingFactor),
                (int) (5 * scalingFactor),
                (int) (5 * scalingFactor),
                (int) (5 * scalingFactor)));

        // Painel de botões de navegação
        JPanel navButtons = new JPanel(new GridLayout(1, 8, (int) (5 * scalingFactor), 0)); // Alterado para 8 colunas
        navButtons.add(backButton);
        navButtons.add(forwardButton);
        navButtons.add(refreshButton);
        navButtons.add(homeButton);

        // Botão de nova aba
        JButton newTabButton = createScaledButton("+", "Nova Aba");
        newTabButton.addActionListener(e -> createNewTab());
        navButtons.add(newTabButton);

        navButtons.add(bookmarksButton);
        navButtons.add(historyButton);
        navButtons.add(settingsButton);

        navPanel.add(navButtons, BorderLayout.WEST);
        navPanel.add(urlBar, BorderLayout.CENTER);
        navPanel.add(goButton, BorderLayout.EAST);

        // Painel de zoom - movido para dentro da barra de navegação
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        zoomLabel = new JLabel("Zoom: 100%");
        zoomLabel.setFont(deriveFont(zoomLabel.getFont()));

        zoomSlider = new JSlider(50, 200, 100);
        zoomSlider.setMajorTickSpacing(25);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setFont(deriveFont(zoomSlider.getFont()));
        zoomSlider.addChangeListener(e -> {
            int value = zoomSlider.getValue();
            currentZoom = value / 100.0;
            updateZoom();
        });

        zoomPanel.add(zoomLabel);
        zoomPanel.add(zoomSlider);

        // Adiciona o painel de zoom à direita da barra de navegação
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(zoomPanel, BorderLayout.EAST);
        navPanel.add(rightPanel, BorderLayout.EAST);

        // Painel de status
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(statusLabel, BorderLayout.EAST);

        // Layout principal
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(navPanel, BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        contentPanel.add(statusPanel, BorderLayout.SOUTH);

        add(contentPanel);
    }

    private void createNewTab() {
        JFXPanel newFxPanel = new JFXPanel();

        Platform.runLater(() -> {
            try {
                WebView newWebView = new WebView();
                WebEngine newWebEngine = newWebView.getEngine();

                // Configurações do WebView com zoom inicial
                newWebView.setZoom(currentZoom);
                newWebView.setFontScale(currentZoom * scalingFactor);
                newWebEngine.setJavaScriptEnabled(isJavaScriptEnabled());
                newWebEngine.setUserAgent(getUserAgent());

                // Configura listeners para a nova aba
                setupWebEngineListeners(newWebEngine);

                // Cria a cena JavaFX
                BorderPane webPane = new BorderPane(newWebView);
                Scene scene = new Scene(webPane);
                newFxPanel.setScene(scene);

                // Adiciona a nova aba
                SwingUtilities.invokeLater(() -> {
                    addNewTab("Nova aba", newFxPanel);
                    // Carrega a página inicial na nova aba
                    newWebEngine.load(getHomePage());
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(SwingBrowserApp.this,
                            "Erro ao criar nova aba: " + e.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void setupWebEngineListeners(WebEngine engine) {
        // Listener para atualizar a URL na barra de endereço
        engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            SwingUtilities.invokeLater(() -> {
                // Atualiza apenas se for a aba selecionada
                if (tabbedPane.getSelectedComponent() != null &&
                        tabbedPane.getSelectedComponent() instanceof JPanel &&
                        ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0) instanceof JFXPanel &&
                        ((JFXPanel) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getScene() != null &&
                        ((JFXPanel) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getScene()
                                .getRoot() instanceof BorderPane
                        &&
                        ((BorderPane) ((JFXPanel) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0))
                                .getScene()
                                .getRoot()).getCenter() instanceof WebView
                        &&
                        ((WebView) ((BorderPane) ((JFXPanel) ((JPanel) tabbedPane.getSelectedComponent())
                                .getComponent(0))
                                .getScene().getRoot()).getCenter()).getEngine() == engine) {

                    urlBar.setText(newUrl);
                    addToHistory(newUrl);
                    updateNavButtons();
                }
            });
        });

        // Listener para atualizar o título da aba e da janela
        engine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            SwingUtilities.invokeLater(() -> {
                // Atualiza o título da aba correspondente
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component tabComponent = tabbedPane.getComponentAt(i);
                    if (tabComponent instanceof JPanel) {
                        Component webComponent = ((JPanel) tabComponent).getComponent(0);
                        if (webComponent instanceof JFXPanel) {
                            JFXPanel fxPanel = (JFXPanel) webComponent;
                            if (fxPanel.getScene() != null &&
                                    fxPanel.getScene().getRoot() instanceof BorderPane &&
                                    ((BorderPane) fxPanel.getScene().getRoot()).getCenter() instanceof WebView &&
                                    ((WebView) ((BorderPane) fxPanel.getScene().getRoot()).getCenter())
                                            .getEngine() == engine) {

                                updateTabTitle(i, newTitle);
                                break;
                            }
                        }
                    }
                }
            });
        });

        // Listener para a barra de progresso
        engine.getLoadWorker().progressProperty().addListener((obs, oldProgress, newProgress) -> {
            SwingUtilities.invokeLater(() -> {
                int progress = (int) (newProgress.doubleValue() * 100);
                progressBar.setValue(progress);
                statusLabel.setText(progress == 100 ? "Carregamento completo" : "Carregando... " + progress + "%");
            });
        });

        // Listener para erros de carregamento
        engine.getLoadWorker().exceptionProperty().addListener((obs, oldException, newException) -> {
            if (newException != null) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erro ao carregar página: " + newException.getMessage());
                    JOptionPane.showMessageDialog(SwingBrowserApp.this,
                            "Erro ao carregar página: " + newException.getMessage(),
                            "Erro de Navegação", JOptionPane.ERROR_MESSAGE);
                });
            }
        });

        // Listener para mensagens de status
        engine.setOnStatusChanged(event -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText(event.getData());
            });
        });

        // Listener para atualizar botões de navegação
        engine.getHistory().currentIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            SwingUtilities.invokeLater(() -> {
                updateNavButtons();
            });
        });
    }

    private void updateTabTitle(int tabIndex, String title) {
        if (tabIndex >= 0 && tabIndex < tabbedPane.getTabCount()) {
            Component tabComponent = tabbedPane.getTabComponentAt(tabIndex);
            if (tabComponent instanceof JPanel) {
                for (Component c : ((JPanel) tabComponent).getComponents()) {
                    if (c instanceof JLabel) {
                        ((JLabel) c).setText(title);
                        break;
                    }
                }
            }

            // Atualiza o título da janela se for a aba selecionada
            if (tabbedPane.getSelectedIndex() == tabIndex) {
                setTitle(title + " - " + APP_NAME);
            }
        }
    }

    private void updateZoom() {
        Platform.runLater(() -> {
            if (webView != null) {
                webView.setZoom(currentZoom);
                zoomLabel.setText(String.format("Zoom: %d%%", (int) (currentZoom * 100)));

                // Ajusta o tamanho da fonte baseado no zoom
                webView.setFontScale(currentZoom * scalingFactor);

                // Salva o zoom nas preferências
                prefs.putDouble("zoomLevel", currentZoom);
            }
        });
    }

    private void setupListeners() {
        // Ação para carregar URL
        ActionListener loadUrlAction = e -> {
            String url = urlBar.getText().trim();
            if (!url.isEmpty()) {
                loadUrl(ensureUrlProtocol(url));
            }
        };

        goButton.addActionListener(loadUrlAction);
        urlBar.addActionListener(loadUrlAction);

        // Botões de navegação
        backButton.addActionListener(e -> Platform.runLater(() -> {
            if (webEngine.getHistory().getCurrentIndex() > 0) {
                webEngine.getHistory().go(-1);
            }
        }));

        forwardButton.addActionListener(e -> Platform.runLater(() -> {
            if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
                webEngine.getHistory().go(1);
            }
        }));

        refreshButton.addActionListener(e -> Platform.runLater(() -> webEngine.reload()));
        homeButton.addActionListener(e -> loadUrl(getHomePage()));

        // Menu de favoritos
        bookmarksButton.addActionListener(e -> showBookmarksMenu());
        historyButton.addActionListener(e -> showHistoryDialog());
        settingsButton.addActionListener(e -> showSettingsDialog());

        // Listener para redimensionamento
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Apenas atualiza o que realmente precisa
                updateNavButtons();
            }
        });

        // Listener para fechamento da janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveHistory();
                saveBookmarks();
            }
        });
    }

    private void updateComponentSizes() {
        Font scaledFont = deriveFont(UIManager.getFont("Button.font"));

        // Apenas componentes que realmente precisam de ajuste
        urlBar.setFont(scaledFont);
        statusLabel.setFont(scaledFont);

        // Tamanho fixo baseado no scalingFactor inicial
        int btnSize = (int) (30 * scalingFactor);
        Dimension buttonSize = new Dimension(btnSize, btnSize);

        backButton.setPreferredSize(buttonSize);
        forwardButton.setPreferredSize(buttonSize);
        refreshButton.setPreferredSize(buttonSize);
        homeButton.setPreferredSize(buttonSize);
    }

    private void setupWebView() {
        fxPanel = new JFXPanel();
        createNewTab(); // Substitui o código antigo por uma chamada ao novo método

        // Configurações específicas para Linux
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
        }

        Platform.runLater(() -> {
            try {
                webView = new WebView();
                webEngine = webView.getEngine();

                // Configurações do WebView com zoom inicial
                webView.setZoom(currentZoom);
                webView.setFontScale(currentZoom * scalingFactor);

                webEngine.setJavaScriptEnabled(isJavaScriptEnabled());
                webEngine.setUserAgent(getUserAgent());

                // Cria a cena JavaFX
                BorderPane webPane = new BorderPane(webView);
                Scene scene = new Scene(webPane);
                fxPanel.setScene(scene);

                // Listeners do WebEngine (mantidos como antes)
                webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
                    SwingUtilities.invokeLater(() -> {
                        urlBar.setText(newUrl);
                        addToHistory(newUrl);
                        updateNavButtons();
                    });
                });

                webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
                    SwingUtilities.invokeLater(() -> {
                        setTitle(newTitle + " - " + APP_NAME);
                        updateTabTitle(newTitle);
                    });
                });

                webEngine.getLoadWorker().progressProperty().addListener((obs, oldProgress, newProgress) -> {
                    SwingUtilities.invokeLater(() -> {
                        int progress = (int) (newProgress.doubleValue() * 100);
                        progressBar.setValue(progress);
                        statusLabel
                                .setText(progress == 100 ? "Carregamento completo" : "Carregando... " + progress + "%");
                    });
                });

                webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldException, newException) -> {
                    if (newException != null) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Erro ao carregar página: " + newException.getMessage());
                            JOptionPane.showMessageDialog(SwingBrowserApp.this,
                                    "Erro ao carregar página: " + newException.getMessage(),
                                    "Erro de Navegação", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                });

                webEngine.setOnStatusChanged(event -> {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(event.getData());
                    });
                });

                // Adiciona o WebView a uma nova aba
                addNewTab("Nova aba", fxPanel);
                // Adiciona o WebView a uma nova aba
                addNewTab("Nova aba", fxPanel);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(SwingBrowserApp.this,
                            "Erro ao inicializar WebView: " + e.getMessage(),
                            "Erro de Renderização", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void updateTabTitle(String title) {
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0) {
            Component tabComponent = tabbedPane.getTabComponentAt(index);
            if (tabComponent instanceof JPanel) {
                for (Component c : ((JPanel) tabComponent).getComponents()) {
                    if (c instanceof JLabel) {
                        ((JLabel) c).setText(title);
                        break;
                    }
                }
            }
        }
    }

    private void addNewTab(String title, Component component) {
        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.add(component, BorderLayout.CENTER);

        // Adiciona um botão de fechar à aba
        JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabHeader.setOpaque(false);

        JLabel tabTitle = new JLabel(title);
        tabTitle.setFont(deriveFont(tabTitle.getFont()));

        JButton closeButton = new JButton("×");
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setFocusable(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setContentAreaFilled(false);
        closeButton.setRolloverEnabled(true);
        closeButton.setFont(deriveFont(closeButton.getFont()));

        closeButton.addActionListener(e -> {
            if (tabbedPane.getTabCount() > 1) {
                tabbedPane.remove(tabbedPane.indexOfComponent(tabPanel));
            } else {
                loadUrl(getHomePage());
            }
        });

        tabHeader.add(tabTitle);
        tabHeader.add(Box.createHorizontalStrut((int) (5 * scalingFactor)));
        tabHeader.add(closeButton);

        tabbedPane.addTab(null, tabPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabHeader);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    private void loadInitialPage() {
        loadUrl(getHomePage());
    }

    private void loadUrl(String url) {
        Platform.runLater(() -> {
            try {
                webEngine.load(url);
            } catch (Exception e) {
                statusLabel.setText("Erro ao carregar URL: " + e.getMessage());
            }
        });
    }

    private String ensureUrlProtocol(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "http://" + url;
        }
        return url;
    }

    private void updateNavButtons() {
        Platform.runLater(() -> {
            boolean canGoBack = webEngine.getHistory().getCurrentIndex() > 0;
            boolean canGoForward = webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size()
                    - 1;

            SwingUtilities.invokeLater(() -> {
                backButton.setEnabled(canGoBack);
                forwardButton.setEnabled(canGoForward);
            });
        });
    }

    private void addToHistory(String url) {
        if (!history.isEmpty() && history.get(history.size() - 1).equals(url)) {
            return;
        }
        history.add(url);

        // Limita o histórico a 100 itens
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    private void showBookmarksMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setFont(deriveFont(menu.getFont()));

        // Adiciona item para adicionar/remover favorito
        String currentUrl = urlBar.getText();
        boolean isBookmarked = bookmarks.contains(currentUrl);

        JMenuItem toggleBookmarkItem = new JMenuItem(
                isBookmarked ? "Remover dos favoritos" : "Adicionar aos favoritos");
        toggleBookmarkItem.setFont(deriveFont(toggleBookmarkItem.getFont()));
        toggleBookmarkItem.addActionListener(e -> {
            if (isBookmarked) {
                bookmarks.remove(currentUrl);
            } else {
                bookmarks.add(currentUrl);
            }
            saveBookmarks();
        });
        menu.add(toggleBookmarkItem);
        menu.addSeparator();

        // Adiciona favoritos existentes
        if (bookmarks.isEmpty()) {
            JMenuItem noBookmarksItem = new JMenuItem("Nenhum favorito salvo");
            noBookmarksItem.setEnabled(false);
            noBookmarksItem.setFont(deriveFont(noBookmarksItem.getFont()));
            menu.add(noBookmarksItem);
        } else {
            for (String bookmark : bookmarks) {
                JMenuItem bookmarkItem = new JMenuItem(shortenUrl(bookmark, 40));
                bookmarkItem.setFont(deriveFont(bookmarkItem.getFont()));
                bookmarkItem.addActionListener(e -> loadUrl(bookmark));
                menu.add(bookmarkItem);
            }
        }

        menu.show(bookmarksButton, 0, bookmarksButton.getHeight());
    }

    private void showHistoryDialog() {
        JDialog historyDialog = new JDialog(this, "Histórico de Navegação", false);
        historyDialog.setSize((int) (600 * scalingFactor), (int) (400 * scalingFactor));
        historyDialog.setLocationRelativeTo(this);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String url : history) {
            listModel.addElement(shortenUrl(url, 80));
        }

        JList<String> historyList = new JList<>(listModel);
        historyList.setFont(deriveFont(historyList.getFont()));
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = historyList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        loadUrl(history.get(index));
                        historyDialog.dispose();
                    }
                }
            }
        });

        JButton clearButton = new JButton("Limpar Histórico");
        clearButton.setFont(deriveFont(clearButton.getFont()));
        clearButton.addActionListener(e -> {
            history.clear();
            listModel.clear();
            saveHistory();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        historyDialog.add(contentPanel);
        historyDialog.setVisible(true);
    }

    private void showSettingsDialog() {
        JDialog settingsDialog = new JDialog(this, "Configurações", true);
        settingsDialog.setSize((int) (500 * scalingFactor), (int) (350 * scalingFactor)); // Aumentado para 350
        settingsDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(5, 2, (int) (10 * scalingFactor), (int) (10 * scalingFactor))); // Alterado
                                                                                                                 // para
                                                                                                                 // 5
                                                                                                                 // linhas
        panel.setBorder(BorderFactory.createEmptyBorder(
                (int) (20 * scalingFactor),
                (int) (20 * scalingFactor),
                (int) (20 * scalingFactor),
                (int) (20 * scalingFactor)));

        // Página inicial
        JLabel homePageLabel = new JLabel("Página inicial:");
        homePageLabel.setFont(deriveFont(homePageLabel.getFont()));
        panel.add(homePageLabel);

        JTextField homePageField = new JTextField(getHomePage());
        homePageField.setFont(deriveFont(homePageField.getFont()));
        panel.add(homePageField);

        // Habilitar JavaScript
        JLabel enableJsLabel = new JLabel("Habilitar JavaScript:");
        enableJsLabel.setFont(deriveFont(enableJsLabel.getFont()));
        panel.add(enableJsLabel);

        JCheckBox enableJsCheckbox = new JCheckBox("", isJavaScriptEnabled());
        enableJsCheckbox.setFont(deriveFont(enableJsCheckbox.getFont()));
        panel.add(enableJsCheckbox);

        // User Agent
        JLabel userAgentLabel = new JLabel("User Agent:");
        userAgentLabel.setFont(deriveFont(userAgentLabel.getFont()));
        panel.add(userAgentLabel);

        JTextField userAgentField = new JTextField(getUserAgent());
        userAgentField.setFont(deriveFont(userAgentField.getFont()));
        panel.add(userAgentField);

        // Zoom padrão
        JLabel defaultZoomLabel = new JLabel("Zoom padrão (%):");
        defaultZoomLabel.setFont(deriveFont(defaultZoomLabel.getFont()));
        panel.add(defaultZoomLabel);

        JTextField defaultZoomField = new JTextField(String.valueOf((int) (currentZoom * 100)));
        defaultZoomField.setFont(deriveFont(defaultZoomField.getFont()));
        panel.add(defaultZoomField);

        // Botões
        JButton saveButton = new JButton("Salvar");
        saveButton.setFont(deriveFont(saveButton.getFont()));
        saveButton.addActionListener(e -> {
            setHomePage(homePageField.getText().trim());
            setJavaScriptEnabled(enableJsCheckbox.isSelected());
            setUserAgent(userAgentField.getText().trim());

            try {
                int zoomValue = Integer.parseInt(defaultZoomField.getText().trim());
                zoomValue = Math.max(50, Math.min(200, zoomValue));
                currentZoom = zoomValue / 100.0;
                zoomSlider.setValue(zoomValue);
                updateZoom();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Valor de zoom inválido. Use um número entre 50 e 200.");
            }

            settingsDialog.dispose();
        });

        JButton cancelButton = new JButton("Cancelar");
        cancelButton.setFont(deriveFont(cancelButton.getFont()));
        cancelButton.addActionListener(e -> settingsDialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        settingsDialog.add(contentPanel);
        settingsDialog.setVisible(true);
    }

    private String shortenUrl(String url, int maxLength) {
        if (url.length() <= maxLength) {
            return url;
        }
        return url.substring(0, maxLength / 2) + "..." + url.substring(url.length() - maxLength / 2);
    }

    private void saveHistory() {
        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
            new Gson().toJson(history, writer);
        } catch (IOException e) {
            System.err.println("Erro ao salvar histórico: " + e.getMessage());
        }
    }

    private void saveBookmarks() {
        try (FileWriter writer = new FileWriter(BOOKMARKS_FILE)) {
            new Gson().toJson(bookmarks, writer);
        } catch (IOException e) {
            System.err.println("Erro ao salvar favoritos: " + e.getMessage());
        }
    }

    // Métodos de preferências
    private String getHomePage() {
        return prefs.get("homePage", DEFAULT_HOME_PAGE);
    }

    private void setHomePage(String url) {
        prefs.put("homePage", url);
    }

    private boolean isJavaScriptEnabled() {
        return prefs.getBoolean("javaScriptEnabled", true);
    }

    private void setJavaScriptEnabled(boolean enabled) {
        prefs.putBoolean("javaScriptEnabled", enabled);
        Platform.runLater(() -> webEngine.setJavaScriptEnabled(enabled));
    }

    private String getUserAgent() {
        return prefs.get("userAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
    }

    private void setUserAgent(String userAgent) {
        prefs.put("userAgent", userAgent);
        Platform.runLater(() -> webEngine.setUserAgent(userAgent));
    }

    public static void main(String[] args) {
        // Configurações para Linux antes de iniciar a aplicação
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.lcdtext", "false");

        SwingUtilities.invokeLater(() -> {
            try {
                new SwingBrowserApp().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Erro ao iniciar o navegador: " + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}