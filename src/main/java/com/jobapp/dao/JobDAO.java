package com.jobapp.dao;

import com.jobapp.model.Offre;
import java.sql.*;

public class JobDAO {
    private static final String URL = "jdbc:mysql://localhost:3308/job_portal_db"; // Check your port
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // METHOD A: Only saves the Job (Independent of any user)
    public void saveJob(Offre job) {
        // FIX: We added 'ville=VALUES(ville)' and 'entreprise=VALUES(entreprise)'
        String sql = "INSERT INTO offres (titre, description, entreprise, ville, url, category) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "description=VALUES(description), " +
                "category=VALUES(category), " +
                "ville=VALUES(ville), " +             // <--- CRITICAL FIX
                "entreprise=VALUES(entreprise), " +    // <--- CRITICAL FIX
                "date_scrapped=CURRENT_TIMESTAMP";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, job.getTitre());
            ps.setString(2, job.getDescription());
            ps.setString(3, job.getEntreprise());
            ps.setString(4, job.getVille());
            ps.setString(5, job.getUrl());
            ps.setString(6, job.getCategory());

            ps.executeUpdate();
            System.out.println("[DB] Job saved: " + job.getTitre());

        } catch (SQLException e) {
            System.err.println("DB Error (saveJob): " + e.getMessage());
        }
    }
    // METHOD B: Only saves the Match Result (Links a specific CV to a Job)
    public void saveMatch(int cvId, String jobUrl, double score) {
        String sql = "INSERT INTO matching_results (cv_id, job_url, match_score) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cvId);
            ps.setString(2, jobUrl);
            ps.setDouble(3, score);

            ps.executeUpdate();
            System.out.println("   -> [DB] Match saved! Score: " + score + "%");

        } catch (SQLException e) {
            System.err.println("DB Error (saveMatch): " + e.getMessage());
        }
    }
}