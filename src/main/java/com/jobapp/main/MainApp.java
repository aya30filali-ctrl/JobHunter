package com.jobapp.main;

import com.jobapp.dao.CVDAO;
import com.jobapp.dao.JobDAO;
import com.jobapp.ml.CVParser;
import com.jobapp.ml.JobMatcher; // <--- NEW IMPORT
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

        // 1. Initialize the NEW Matcher
        JobMatcher intelligentMatcher = new JobMatcher();

        // --- PHASE 1: SCRAPING (Keeping your working code) ---
        System.out.println(">>> PHASE 1: Scraping jobs...");
        List<JobScraper> myRobots = new ArrayList<>();
        myRobots.add(new RekruteScraper());
        myRobots.add(new EmploiMaScraper()); // Uncomment if you want both

        List<Offre> allScrapedJobs = new ArrayList<>();
        for (JobScraper robot : myRobots) {
            List<Offre> jobs = robot.scrape(""); // Or scrape "finance", "commercial" to test more
            for (Offre job : jobs) jobDao.saveJob(job);
            allScrapedJobs.addAll(jobs);
        }

        // --- PHASE 2 & 3: SIMULATING 3 DIFFERENT USERS ---
        // We will create 3 users to test if the "Brain" works correctly.

        String[] userNames = {"Ahmed IT", "Sarah Finance", "Karim Sales"};
        String[] rawCVs = {
                // CV 1: IT Expert
                "Je suis Ingénieur Informatique expert en Java, Spring Boot, SQL et React. " +
                        "J'ai 5 ans d'expérience en développement web full stack. Passionné par le code et l'architecture logicielle.",

                // CV 2: Finance & Audit
                "Je suis Auditeur Financier. Expert en comptabilité, gestion de trésorerie, audit externe et finance d'entreprise. " +
                        "Maitrise de Excel, SAP et analyse financière. Diplômé en économie.",

                // CV 3: Sales / Commerce
                "Commercial terrain dynamique. Expert en vente, négociation client, développement de portefeuille et prospection. " +
                        "J'aime le contact client et atteindre les objectifs de vente. Secteur automobile et assurance."
        };

        System.out.println("\n>>> PHASE 3: Matching Analysis (Cosine Similarity) <<<");

        for (int i = 0; i < userNames.length; i++) {
            String name = userNames[i];
            String rawText = rawCVs[i];

            // Clean CV
            CVParser parser = new CVParser();
            String cleanCV = parser.parseAndRegroup(rawText);

            // Save to DB
            int cvId = cvDao.saveCV(name, "test" + i + "@email.com", rawText, cleanCV);

            System.out.println("\n--- Testing Matches for: " + name + " (ID: " + cvId + ") ---");

            // Check against ALL jobs
            for (Offre job : allScrapedJobs) {
                // Combine Job Title + Description for better matching
                String jobFullText = job.getTitre() + " " + job.getDescription();

                // *** THE MAGIC HAPPENS HERE ***
                double score = intelligentMatcher.calculateScore(cleanCV, jobFullText);

                // Only print/save significant matches
                if (score > 10.0) {
                    jobDao.saveMatch(cvId, job.getUrl(), score);
                    // Print nicely to console to verify accuracy
                    System.out.printf("   [Match: %.1f%%] Job: %s (%s)\n", score, job.getTitre(), job.getCategory());
                }
            }
        }
    }
}