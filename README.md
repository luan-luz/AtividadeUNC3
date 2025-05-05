# Sistema de Gerenciamento de Contatos com JavaFX e SQLite

## Visão Geral

Este sistema foi desenvolvido com a finalidade de gerenciar contatos pessoais, utilizando Java como linguagem de programação, JavaFX como framework para construção da interface gráfica e SQLite como banco de dados local relacional. O sistema implementa operações básicas de CRUD (Create, Read, Update, Delete) e segue princípios de separação de responsabilidades via arquitetura MVC (Model-View-Controller).

## Estrutura do Projeto

```
GerenciadorContatosJavaFX/
├── src/
│   ├── app/
│   │   └── MainApp.java
│   ├── controller/
│   │   ├── MainController.java
│   │   └── EditarContatoController.java
│   ├── dao/
│   │   └── ContatoDAO.java
│   ├── model/
│   │   └── Contato.java
│   ├── util/
│   │   └── Database.java
│   └── view/
│       ├── MainView.fxml
│       └── EditarContatoView.fxml
├── contatos.db
└── README.md
```

## Componentes e Funcionalidades

### 1. MainApp.java
Classe principal que inicializa a aplicação JavaFX, carregando a `MainView.fxml` como interface principal.

### 2. MainView.fxml
Arquivo FXML que define a interface principal do usuário, contendo campos de entrada, botões de ação e uma `TableView` para exibição dos contatos armazenados.

### 3. MainController.java
Controlador da interface principal. Responsável por:
- Validar campos antes de adicionar ou atualizar contatos.
- Restringir entrada no campo telefone para apenas números com no máximo 11 dígitos.
- Impedir que o campo email aceite entradas inválidas.
- Controlar a tabela e ações de adicionar, excluir, abrir a janela de edição e atualizar a visualização.

### 4. EditarContatoView.fxml
Arquivo FXML da janela modal usada para edição de contatos. Fornece campos de entrada para os dados de contato previamente carregados.

### 5. EditarContatoController.java
Controlador da janela de edição. Executa a validação e persistência de dados atualizados do contato selecionado, e executa um callback para atualizar a tabela na tela principal após a edição.

### 6. Contato.java
Classe modelo representando a entidade `Contato`. Contém atributos `id`, `nome`, `sobrenome`, `email`, `telefone` com seus respectivos métodos getters e setters.

### 7. ContatoDAO.java
Classe responsável pelo acesso aos dados do banco. Implementa métodos para:
- Inserção (`adicionar`)
- Listagem (`listarTodos`)
- Atualização (`atualizar`)
- Exclusão (`deletar`)
  Utiliza JDBC puro para interação com o SQLite.

### 8. Database.java
Classe utilitária para conexão com o banco de dados SQLite. Expõe o método `connect()` que retorna uma instância `Connection`.

## Banco de Dados

O banco `contatos.db` é criado automaticamente ao iniciar o sistema. A estrutura da tabela é:

```sql
CREATE TABLE contatos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  sobrenome TEXT NOT NULL,
  email TEXT,
  telefone TEXT
);
```

## Funcionalidade dos Botões

- **Adicionar**: Insere novo contato no banco após validação de todos os campos.
- **Editar**: Abre uma nova janela (`EditarContatoView.fxml`) com os dados do contato selecionado. A edição ocorre exclusivamente nesta janela.
- **Atualizar (na janela modal)**: Persiste alterações no banco de dados e fecha a janela.
- **Excluir**: Remove o contato selecionado da tabela e do banco de dados.
- **Atualizar (na tela principal)**: Recarrega todos os dados do banco e atualiza a `TableView`.

## Validações Implementadas

- O campo telefone aceita apenas números e no máximo 11 caracteres.
- O campo email é validado via expressão regular.
- Todos os campos são obrigatórios para operações de inserção e edição.
- Nenhuma operação é executada sem seleção prévia de um item da tabela, onde aplicável.

## Funcionalidades Não Implementadas

- O sistema não realiza paginação ou ordenação nos dados exibidos.
- A edição não pode ser feita diretamente na `TableView`, apenas via janela modal.
- O sistema não possui funcionalidade de busca ou filtro por nome/email.
- Não há exportação de dados (ex: CSV, Excel).
- O sistema não trata conflitos concorrentes de múltiplos acessos simultâneos ao banco.

## Considerações Finais

O sistema foi projetado para ser funcional, modular e extensível. Ele serve como base para sistemas CRUD com interface gráfica em Java, oferecendo controle completo do ciclo de vida dos dados e mantendo separação lógica entre a camada de apresentação, controle e persistência.
## Instruções para Configuração e Execução no IntelliJ IDEA

### Requisitos

- IntelliJ IDEA (Community ou Ultimate)
- Java JDK 17 ou superior
- JavaFX SDK (mesma versão do JDK ou compatível)
- SQLite JDBC Driver incluído

### Etapas de Configuração

1. **Importação do Projeto**
    - Extraia o arquivo `.zip` do projeto para uma pasta local.
    - No IntelliJ, selecione `File > Open` e escolha a pasta do projeto.

2. **Configuração do SDK**
    - Acesse `File > Project Structure > Project`.
    - Em `Project SDK`, selecione o JDK 17 ou superior. Se não estiver configurado, adicione manualmente.

3. **Adição das Bibliotecas do JavaFX**
    - Vá em `File > Project Structure > Libraries`.
    - Clique no botão `+`, selecione `Java`, e adicione o diretório `lib` da pasta do JavaFX SDK baixado.

4. **Criação da Configuração de Execução**
    - Vá em `Run > Edit Configurations`.
    - Crie uma nova configuração do tipo `Application`.
        - Classe principal: `app.MainApp`
        - Use classpath do módulo: selecione o módulo atual do projeto.
    - Em `VM Options`, adicione:

      ```
      --module-path "C:\caminho\para\javafx-sdk-XX\lib" --add-modules javafx.controls,javafx.fxml
      ```

      Substitua o caminho conforme a localização real do seu JavaFX SDK.

5. **Verificação de Recursos**
    - Certifique-se de que os arquivos `.fxml` e `.db` estejam acessíveis no classpath.
    - A estrutura `src/view` e `contatos.db` deve estar na raiz do projeto.

6. **Execução**
    - Execute o projeto clicando em `Run > Run 'MainApp'`.

### Observações Importantes

- Caso veja erros como `Invalid URL` ou `LoadException`, verifique se os caminhos dos arquivos `.fxml` estão corretos e se estão incluídos nos `resources`.
- Certifique-se de não mover o `contatos.db` da raiz do projeto, pois o caminho é relativo.