package com.jobapp.dao;

import java.sql.*;

public class CVDAO {
    private static final String URL = "jdbc:mysql://localhost:3308/job_portal_db"; // Verify your port (3306 or 3308)
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Method to save the parsed CV into the database
    public int saveCV(String nom, String email, String rawText, String parsedText) {
        String sql = "INSERT INTO cvs (nom_candidat, email, contenu_texte, date_upload) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        int generatedId = -1;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nom);
            ps.setString(2, email);
            
            // We save the "Parsed/Cleaned" text because that's what the AI uses for matching
            ps.setString(3, parsedText); 

            ps.executeUpdate();

            // Get the ID of the new CV so we can use it for matching later
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            }
            System.out.println("[DB] CV saved for candidate: " + nom + " (ID: " + generatedId + ")");

        } catch (SQLException e) {
            System.err.println("CV DB Error: " + e.getMessage());
        }
        return generatedId;
    }
}