# Java Browser

![Java Browser Screenshot](src\main\resources\image\iconeb2.ico)

Um navegador web desenvolvido em Java utilizando Swing e JavaFX WebView, com interface responsiva.

## 📌 Sobre o Projeto

Desenvolvido por **Leandro Nazareth**, o Java Browser é um navegador web leve que combina a interface Swing com o motor de renderização WebView do JavaFX. Principais características:

- Navegação por abas
- Histórico de navegação
- Gerenciamento de favoritos
- Interface responsiva que se adapta a diferentes tamanhos de tela
- Configurações personalizáveis

## 🚀 Tecnologias Utilizadas

- Java 11+
- Swing (Interface gráfica)
- JavaFX WebView (Renderização web)
- FlatLaf (Tema moderno para Swing)
- Gson (Persistência de dados)

## ⚙️ Pré-requisitos

- JDK 17 ou superior
- Maven 3.6+
- JavaFX SDK (incluído nas dependências do Maven)

## 🛠️ Como Executar

### 1. Clonar o repositório

```bash
git clone https://github.com/seu-usuario/java-browser.git
cd java-browser
```

### 2. Compilar e executar com Maven
```bash
mvn clean compile
mvn exec:java
```

### 3. Executando o JAR
```bash
mvn package
java -jar target/java-browser.jar
```

### 📦 Estrutura do Projeto
```bash
java-browser/
├── src/
│   ├── main/
│   │   ├── java/com/bl/
│   │   │   ├── SwingBrowserApp.java      # Classe principal
│   │   │   └── WebViewCookieManager.java # Gerenciador de cookies
│   │   └── resources/
│   │       └── image/
│   │           └── iconeb2.png           # Ícone do aplicativo
├── pom.xml                              # Configuração do Maven
└── README.md
```
## 🤝 Como Contribuir

Contribuições são bem-vindas! Siga estes passos:

- Faça um fork do projeto

- Crie uma branch para sua feature (git checkout -b feature/AmazingFeature)

- Commit suas mudanças (git commit -m 'Add some AmazingFeature')

- Push para a branch (git push origin feature/AmazingFeature)

- Abra um Pull Request

## 📄 Licença

Distribuído sob licença MIT. Veja LICENSE para mais informações.

Leandro Nazareth - leandrosnazareth@gmail.com
