package com.jobapp.main;

import com.jobapp.dao.CVDAO;
import com.jobapp.dao.JobDAO;
import com.jobapp.service.NeuralMatchingEngine; // <--- ON UTILISE LE NOUVEAU MOTEUR
import com.jobapp.model.Offre;
import com.jobapp.scraping.EmploiMaScraper;
import com.jobapp.scraping.JobScraper;
import com.jobapp.scraping.RekruteScraper;

import java.util.ArrayList;
import java.util.List;

public class MainApp {
    public static void main(String[] args) {
        JobDAO jobDao = new JobDAO();
        CVDAO cvDao = new CVDAO();

        // 1. Initialiser le NOUVEAU Moteur (NeuralMatchingEngine)
        // On remplace l'ancien JobMatcher
        NeuralMatchingEngine neuralMatcher = new NeuralMatchingEngine();

        // --- PHASE 1: SCRAPING ---
        System.out.println(">>> PHASE 1: Scraping des jobs en cours...");
        List<JobScraper> myRobots = new ArrayList<>();
        myRobots.add(new RekruteScraper());
        myRobots.add(new EmploiMaScraper());

        List<Offre> allScrapedJobs = new ArrayList<>();

        // On lance les robots
        for (JobScraper robot : myRobots) {
            // Tu peux changer "" par "java" ou "finance" pour cibler mieux le test
            try {
                List<Offre> jobs = robot.scrape("");
                for (Offre job : jobs) {
                    try {
                        jobDao.saveJob(job); // Sauvegarde sécurisée
                    } catch (Exception e) {
                        System.out.println("Doublon ou erreur DB ignorée.");
                    }
                }
                allScrapedJobs.addAll(jobs);
            } catch (Exception e) {
                System.out.println("Erreur de robot : " + e.getMessage());
            }
        }
        System.out.println(">>> Total jobs récupérés : " + allScrapedJobs.size());

        // --- PHASE 2 & 3: SIMULATION AVEC 3 PROFILS ---

        String[] userNames = {"Ahmed IT", "Sarah Finance", "Karim Sales"};
        String[] rawCVs = {
                // CV 1: Expert IT
                "Je suis Ingénieur Informatique expert en Java, Spring Boot, SQL et React. " +
                        "J'ai 5 ans d'expérience en développement web full stack. Passionné par le code et l'architecture logicielle.",

                // CV 2: Finance & Audit
                "Je suis Auditeur Financier. Expert en comptabilité, gestion de trésorerie, audit externe et finance d'entreprise. " +
                        "Maitrise de Excel, SAP et analyse financière. Diplômé en économie.",

                // CV 3: Sales / Commerce
                "Commercial terrain dynamique. Expert en vente, négociation client, développement de portefeuille et prospection. " +
                        "J'aime le contact client et atteindre les objectifs de vente. Secteur automobile et assurance."
        };

        System.out.println("\n>>> PHASE 3: Analyse de Matching (Moteur Neural) <<<");

        for (int i = 0; i < userNames.length; i++) {
            String name = userNames[i];
            String cvText = rawCVs[i];

            // On simule la sauvegarde du CV
            int cvId = 0;
            try {
                cvId = cvDao.saveCV(name, "test" + i + "@email.com", cvText, cvText);
            } catch (Exception e) { System.out.println("Erreur sauvegarde CV (pas grave pour le test)"); }

            System.out.println("\n--- Test pour : " + name + " ---");

            // On compare ce CV avec TOUS les jobs
            for (Offre job : allScrapedJobs) {

                // *** ICI LA MAGIE OPÈRE AVEC LE NOUVEAU MOTEUR ***
                // On utilise job.getFullText() qu'on a ajouté tout à l'heure
                int score = neuralMatcher.calculateMatchScore(cvText, job.getFullText());

                // On n'affiche que les matchs pertinents (> 10%)
                if (score > 10) {
                    // Sauvegarde du match en DB
                    try { jobDao.saveMatch(cvId, job.getUrl(), score); } catch (Exception e) {}

                    // Affichage console propre
                    // On utilise %d pour le score car c'est un entier maintenant
                    System.out.printf("   [Match: %d%%] Job: %s (Cat: %s)\n", score, job.getTitre(), job.getCategory());
                }
            }
        }
    }
}