package com.jobapp.dao;

import com.jobapp.model.Offre;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobDAO {
    // Port 3307 conservé
    private static final String URL = "jdbc:mysql://localhost:3307/job_portal_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // 1. Save Job (Sauvegarde les offres scrapées)
    public boolean saveJob(Offre job) {
        String sql = "INSERT INTO offres (titre, description, entreprise, ville, url, category) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "description=VALUES(description), " +
                "category=VALUES(category), " +
                "ville=VALUES(ville), " +
                "entreprise=VALUES(entreprise), " +
                "date_scrapped=CURRENT_TIMESTAMP";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String safeTitre = job.getTitre();
            if (safeTitre.length() > 250) safeTitre = safeTitre.substring(0, 250);

            String safeEntreprise = job.getEntreprise();
            if (safeEntreprise.length() > 250) safeEntreprise = safeEntreprise.substring(0, 250);

            ps.setString(1, safeTitre);
            ps.setString(2, job.getDescription());
            ps.setString(3, safeEntreprise);
            ps.setString(4, job.getVille());
            ps.setString(5, job.getUrl());
            ps.setString(6, job.getCategory());

            int result = ps.executeUpdate();

            return result == 1;

        } catch (SQLException e) {
            System.err.println("DB Error (saveJob): " + e.getMessage());
            return false;
        }
    }

    // 2. Save Match (Sauvegarde le lien CV <-> Offre <-> Score)
    public void saveMatch(int cvId, String jobUrl, int score) {
        // Insertion dans matching_results.
        // La colonne 'status' prendra sa valeur par défaut (ex: NULL ou 'pending')
        String sql = "INSERT INTO matching_results (cv_id, job_url, match_score) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE match_score = VALUES(match_score)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cvId);
            ps.setString(2, jobUrl);
            ps.setInt(3, score); // Stocké en INT comme dans votre table

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("DB Error (saveMatch): " + e.getMessage());
        }
    }

    // 3. Get All Jobs (Récupération pour l'affichage)
    public List<Offre> getAllJobs() {
        List<Offre> jobs = new ArrayList<>();
        String sql = "SELECT * FROM offres ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Offre job = new Offre(
                        rs.getString("titre"),
                        rs.getString("entreprise"),
                        rs.getString("ville"),
                        rs.getString("description"),
                        rs.getString("url"),
                        rs.getString("category")
                );
                jobs.add(job);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jobs;
    }
}