package com.jobapp.main;

import com.jobapp.dao.CVDAO;
import com.jobapp.dao.JobDAO;
import com.jobapp.model.Offre;
import com.jobapp.scraping.EmploiMaScraper;
import com.jobapp.scraping.MarocAnnoncesScraper;
import com.jobapp.scraping.RekruteScraper;
import com.jobapp.service.NeuralMatchingEngine;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainApp {
    public static void main(String[] args) {
        System.out.println("=== JOB HUNTER CONSOLE V2 (DB CONNECTED) ===");

        // 1. Initialisation
        JobDAO jobDao = new JobDAO();
        CVDAO cvDao = new CVDAO();
        NeuralMatchingEngine matcher = new NeuralMatchingEngine();
        Scanner scanner = new Scanner(System.in);

        // 2. Scraping (ou chargement DB)
        System.out.println("1. Lancer le scraping ? (o/n)");
        if (scanner.nextLine().equalsIgnoreCase("o")) {
            List<Offre> newJobs = new ArrayList<>();
            newJobs.addAll(new RekruteScraper().scrape("informatique"));
            newJobs.addAll(new EmploiMaScraper().scrape("informatique"));
            newJobs.addAll(new MarocAnnoncesScraper().scrape("informatique"));

            for (Offre j : newJobs) jobDao.saveJob(j);
            System.out.println(newJobs.size() + " offres ajoutées/mises à jour.");
        }

        List<Offre> allJobs = jobDao.getAllJobs();
        System.out.println("Total offres en base : " + allJobs.size());

        // 3. Traitement CV
        System.out.print("Chemin du CV PDF : ");
        String path = scanner.nextLine();

        try (PDDocument doc = Loader.loadPDF(new File(path))) {
            String text = new PDFTextStripper().getText(doc);

            // --- CORRECTION ICI : ON PASSE JUSTE LE TEXTE ---
            int cvId = cvDao.saveCV(text);
            System.out.println("CV Sauvegardé avec ID : " + cvId);

            System.out.println("\n--- RÉSULTATS DU MATCHING ---");
            for (Offre job : allJobs) {
                int score = matcher.calculateMatchScore(text, job.getTitre(), job.getDescription());
                if (score > 40) {
                    System.out.printf("[%d%%] %s (%s)\n", score, job.getTitre(), job.getEntreprise());
                    // Sauvegarde du match
                    jobDao.saveMatch(cvId, job.getUrl(), score);
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}