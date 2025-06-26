package com.bl;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane; // Para organizar o WebView dentro do painel JavaFX
import javafx.scene.web.WebView;

public class SwingBrowserAppOld extends JFrame {

    private JFXPanel fxPanel;
    private WebView webView;
    private JTextField urlBar;
    private JButton goButton;
    private JButton backButton;
    private JButton forwardButton;
    private JProgressBar progressBar;

    public SwingBrowserAppOld() {
        // --- Configuração Básica da Janela Swing ---
        setTitle("Meu Navegador Swing com JavaFX WebView");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null); // Centraliza a janela

        // --- Componentes Swing da Interface do Navegador ---
        urlBar = new JTextField("https://www.google.com");
        goButton = new JButton("Ir");
        backButton = new JButton("←");
        forwardButton = new JButton("→");
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true); // Exibe o texto da porcentagem

        // --- Painel da Barra de Navegação (Swing) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel navButtonsPanel = new JPanel(new GridLayout(1, 3)); // Para os botões Voltar/Avançar/Ir

        navButtonsPanel.add(backButton);
        navButtonsPanel.add(forwardButton);
        navButtonsPanel.add(goButton);

        topPanel.add(urlBar, BorderLayout.CENTER);
        topPanel.add(navButtonsPanel, BorderLayout.EAST);

        // --- Adiciona a barra de progresso abaixo da barra de URL ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(progressBar, BorderLayout.SOUTH);

        // --- JFXPanel para hospedar o WebView (JavaFX) ---
        fxPanel = new JFXPanel();

        // --- Adiciona os painéis ao JFrame principal ---
        add(headerPanel, BorderLayout.NORTH);
        add(fxPanel, BorderLayout.CENTER);

        // --- Inicializa o WebView na thread da aplicação JavaFX ---
        Platform.runLater(() -> {
            // Cria o WebView
            webView = new WebView();
            // Cria um BorderPane para organizar o WebView (prática comum em JavaFX)
            BorderPane webPane = new BorderPane(webView);
            // Define a cena JavaFX para o JFXPanel
            Scene scene = new Scene(webPane);
            fxPanel.setScene(scene);

            // --- Lógica de Eventos para o WebView ---

            // Atualiza a barra de URL quando uma nova página é carregada
            webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
                SwingUtilities.invokeLater(() -> urlBar.setText(newValue));
            });

            // Lógica para a barra de progresso
            webView.getEngine().getLoadWorker().progressProperty().addListener((obs, oldProgress, newProgress) -> {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue((int) (newProgress.doubleValue() * 100));
                });
            });

            // Lógica para lidar com erros de carregamento
            webView.getEngine().getLoadWorker().exceptionProperty().addListener((obs, oldException, newException) -> {
                if (newException != null) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(SwingBrowserAppOld.this,
                                "Erro ao carregar página: " + newException.getMessage(),
                                "Erro de Navegação",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            });

            // Carrega a URL inicial
            webView.getEngine().load(urlBar.getText());
        });

        // --- Listeners de Eventos para Componentes Swing ---
        // Ação do botão "Ir" e Enter na barra de URL
        ActionListener loadUrlAction = e -> {
            String urlText = urlBar.getText();
            if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
                urlText = "http://" + urlText; // Adiciona um protocolo padrão se não houver
            }
            final String finalUrl = urlText;
            Platform.runLater(() -> webView.getEngine().load(finalUrl));
        };

        goButton.addActionListener(loadUrlAction);
        urlBar.addActionListener(loadUrlAction); // Enter na barra de URL

        // Ação do botão "Voltar"
        backButton.addActionListener(e -> {
            Platform.runLater(() -> {
                if (webView.getEngine().getHistory().getCurrentIndex() > 0) {
                    webView.getEngine().getHistory().go(-1);
                }
            });
        });

        // Ação do botão "Avançar"
        forwardButton.addActionListener(e -> {
            Platform.runLater(() -> {
                if (webView.getEngine().getHistory()
                        .getCurrentIndex() < webView.getEngine().getHistory().getEntries().size() - 1) {
                    webView.getEngine().getHistory().go(1);
                }
            });
        });
    }

    public static void main(String[] args) {
        // Garante que a interface Swing seja criada e manipulada na Event Dispatch
        // Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new SwingBrowserAppOld().setVisible(true);
        });
    }
}