package com.jobapp.dao;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CVDAO {
    private static final String URL = "jdbc:mysql://localhost:3307/job_portal_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public int saveCV(String fullText) {
        String sql = "INSERT INTO cvs (nom_candidat, email, contenu_texte, date_upload) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        int generatedId = -1;

        String extractedEmail = extractEmail(fullText);
        String extractedName = extractName(fullText);

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, extractedName);
            ps.setString(2, extractedEmail);
            ps.setString(3, fullText);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            }
            System.out.println("[DB] CV saved: " + extractedName + " (ID: " + generatedId + ")");

        } catch (SQLException e) {
            System.err.println("CV DB Error: " + e.getMessage());
        }
        return generatedId;
    }

    private String extractEmail(String text) {
        if (text == null) return "Non détecté";
        Matcher m = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}").matcher(text);
        return m.find() ? m.group() : "Non détecté";
    }

    private String extractName(String text) {
        if (text == null || text.isEmpty()) return "Candidat Inconnu";
        String[] lines = text.split("\n");
        String firstLine = lines[0].trim();
        return firstLine.length() > 50 ? firstLine.substring(0, 50) : firstLine;
    }
}