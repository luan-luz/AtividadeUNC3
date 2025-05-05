package dao;

import model.Contato;
import util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContatoDAO {
    public void adicionar(Contato contato) throws SQLException {
        String sql = "INSERT INTO contatos (nome, sobrenome, email, telefone) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, contato.getNome());
            stmt.setString(2, contato.getSobrenome());
            stmt.setString(3, contato.getEmail());
            stmt.setString(4, contato.getTelefone());
            stmt.executeUpdate();
        }
    }

    public List<Contato> listarTodos() throws SQLException {
        List<Contato> lista = new ArrayList<>();
        String sql = "SELECT * FROM contatos";
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Contato c = new Contato(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("sobrenome"),
                        rs.getString("email"),
                        rs.getString("telefone")
                );
                lista.add(c);
            }
        }
        return lista;
    }

    public void atualizar(Contato contato) throws SQLException {
        String sql = "UPDATE contatos SET nome=?, sobrenome=?, email=?, telefone=? WHERE id=?";
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, contato.getNome());
            stmt.setString(2, contato.getSobrenome());
            stmt.setString(3, contato.getEmail());
            stmt.setString(4, contato.getTelefone());
            stmt.setInt(5, contato.getId());
            stmt.executeUpdate();
        }
    }

    public void deletar(int id) throws SQLException {
        String sql = "DELETE FROM contatos WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
}