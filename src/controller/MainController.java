package controller;

import dao.ContatoDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Contato;

import java.sql.SQLException;

public class MainController {
    @FXML private TextField nomeField, sobrenomeField, emailField, telefoneField;
    @FXML private TableView<Contato> tabelaContatos;
    @FXML private TableColumn<Contato, String> colNome, colSobrenome, colEmail, colTelefone;

    private final ContatoDAO dao = new ContatoDAO();
    private final ObservableList<Contato> lista = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNome()));
        colSobrenome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSobrenome()));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colTelefone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTelefone()));

        colNome.setResizable(false);
        colSobrenome.setResizable(false);
        colEmail.setResizable(false);
        colTelefone.setResizable(false);

        colNome.prefWidthProperty().bind(tabelaContatos.widthProperty().multiply(0.25));
        colSobrenome.prefWidthProperty().bind(tabelaContatos.widthProperty().multiply(0.25));
        colEmail.prefWidthProperty().bind(tabelaContatos.widthProperty().multiply(0.25));
        colTelefone.prefWidthProperty().bind(tabelaContatos.widthProperty().multiply(0.25));

        telefoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                telefoneField.setText(newVal.replaceAll("[^\\d]", ""));
            } else if (newVal.length() > 11) {
                telefoneField.setText(oldVal);
            }
        });

        listarContatos();
    }

    @FXML
    public void adicionarContato() {
        if (nomeField.getText().isEmpty() || sobrenomeField.getText().isEmpty() || emailField.getText().isEmpty() || telefoneField.getText().isEmpty()) {
            showAlert("Preencha todos os campos antes de adicionar um contato.");
            return;
        }

        if (!emailField.getText().matches("^[\\w\\.-]+@[\\w\\.-]+\\.\\w{2,}$")) {
            showAlert("Email inválido. Por favor, insira um email válido.");
            return;
        }

        try {
            Contato c = new Contato(0, nomeField.getText(), sobrenomeField.getText(), emailField.getText(), telefoneField.getText());
            dao.adicionar(c);
            listarContatos();
            limparCampos();
        } catch (SQLException e) {
            showError(e);
        }
    }

    @FXML
    public void editarContato() {
        Contato selecionado = tabelaContatos.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/EditarContatoView.fxml"));
                Parent root = loader.load();

                EditarContatoController controller = loader.getController();
                controller.setContato(selecionado);
                controller.setOnSaved(this::listarContatos);

                Stage stage = new Stage();
                stage.setTitle("Editar Contato");
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();

            } catch (Exception e) {
                showError(e);
            }
        } else {
            showAlert("Selecione um contato para editar.");
        }
    }

    @FXML
    public void excluirContato() {
        Contato selecionado = tabelaContatos.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            try {
                dao.deletar(selecionado.getId());
                listarContatos();
            } catch (SQLException e) {
                showError(e);
            }
        } else {
            showAlert("Selecione um contato para excluir.");
        }
    }

    @FXML
    public void listarContatos() {
        try {
            lista.setAll(dao.listarTodos());
            tabelaContatos.setItems(lista);
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void limparCampos() {
        nomeField.clear();
        sobrenomeField.clear();
        emailField.clear();
        telefoneField.clear();
    }

    private void showError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Erro no aplicativo");
        alert.setContentText(e.getMessage());
        alert.show();
    }

    private void showAlert(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validação");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}