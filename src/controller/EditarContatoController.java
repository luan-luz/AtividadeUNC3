package controller;

import dao.ContatoDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Contato;

public class EditarContatoController {
    @FXML private TextField nomeField, sobrenomeField, emailField, telefoneField;

    private Contato contato;
    private final ContatoDAO dao = new ContatoDAO();
    private Runnable onSaved;

    public void setContato(Contato contato) {
        this.contato = contato;
        nomeField.setText(contato.getNome());
        sobrenomeField.setText(contato.getSobrenome());
        emailField.setText(contato.getEmail());
        telefoneField.setText(contato.getTelefone());
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    public void salvarEdicao() {
        if (nomeField.getText().isEmpty() || sobrenomeField.getText().isEmpty() || emailField.getText().isEmpty() || telefoneField.getText().isEmpty()) {
            showAlert("Preencha todos os campos.");
            return;
        }

        if (!emailField.getText().matches("^[\\w\\.-]+@[\\w\\.-]+\\.\\w{2,}$")) {
            showAlert("Email inválido.");
            return;
        }

        try {
            contato.setNome(nomeField.getText());
            contato.setSobrenome(sobrenomeField.getText());
            contato.setEmail(emailField.getText());
            contato.setTelefone(telefoneField.getText());
            dao.atualizar(contato);
            if (onSaved != null) onSaved.run();
            ((Stage) nomeField.getScene().getWindow()).close();
        } catch (Exception e) {
            showAlert("Erro ao atualizar: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}