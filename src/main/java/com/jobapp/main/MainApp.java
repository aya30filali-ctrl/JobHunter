package com.jobapp.main;

import com.jobapp.dao.CVDAO;
import com.jobapp.dao.JobDAO;
import com.jobapp.ml.CVParser;
import com.jobapp.model.Offre;
import com.jobapp.scraping.RekruteScraper;
import java.util.List;

public class MainApp {
    public static void main(String[] args) {
        JobDAO jobDao = new JobDAO();
        CVDAO cvDao = new CVDAO();
        RekruteScraper scraper = new RekruteScraper();

        // ---------------------------------------------------------------
        // PHASE 1: STOCKING OFFERS (Independent)
        // ---------------------------------------------------------------
        System.out.println(">>> PHASE 1: Scraping and stocking jobs...");
        List<Offre> scrapedJobs = scraper.scrape("java"); // Scrape from web

        for (Offre job : scrapedJobs) {
            jobDao.saveJob(job); // <--- This now ONLY updates the 'offres' table
        }

        // ---------------------------------------------------------------
        // PHASE 2: USER UPLOAD (Independent)
        // ---------------------------------------------------------------
        System.out.println("\n>>> PHASE 2: User uploads CV...");
        String rawCV = "Je suis Développeur Java Spring Boot. Je cherche un stage PFE.";

        CVParser parser = new CVParser();
        String cleanCV = parser.parseAndRegroup(rawCV);

        // Save CV and get the ID (e.g., ID = 5)
        int myCvId = cvDao.saveCV("Etudiant Test", "test@email.com", rawCV, cleanCV);


        // ---------------------------------------------------------------
        // PHASE 3: MATCHING (The Link)
        // ---------------------------------------------------------------
        if (myCvId != -1) {
            System.out.println("\n>>> PHASE 3: Matching CV ID " + myCvId + " against existing jobs...");

            // In a real app, you would retrieve ALL jobs from DB here.
            // For now, we compare against the list we just scraped.
            for (Offre job : scrapedJobs) {

                // Simple Match Logic (The "Brain")
                double score = 0;
                if (cleanCV.toLowerCase().contains("java")) score += 50;
                if (job.getDescription().toLowerCase().contains("java")) score += 20;

                // Save ONLY if there is a match
                if (score > 0) {
                    jobDao.saveMatch(myCvId, job.getUrl(), score); // <--- Updates 'matching_results'
                }
            }
        }
    }
}