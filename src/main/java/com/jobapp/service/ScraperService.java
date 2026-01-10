package com.jobapp.service;

import com.jobapp.dao.JobDAO;
import com.jobapp.ml.JobMatcher;
import com.jobapp.model.CV;
import com.jobapp.model.Offre;
import com.jobapp.scraping.RekruteScraper;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScraperService {
    private final RekruteScraper scraper = new RekruteScraper();
    private final JobDAO dao = new JobDAO();
    private final JobMatcher matcher = new JobMatcher();

    // Hardcoded user CV for automation (You can make this dynamic later)
    private final CV userCV = new CV("Développeur Java Spring Boot SQL Jsoup HTML CSS Stage PFE");

    public void startAutoUpdate() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        System.out.println(">>> Auto-Update Service Started. Runs every 1 hour.");

        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("\n--- Starting Scheduled Scrape ---");
                List<Offre> jobs = scraper.scrape("java"); // Search keyword

                for (Offre job : jobs) {
                    double score = matcher.calculateMatchScore(userCV.getTextContent(), job.getFullText());
                    dao.saveJobAndScore(job, score);
                }
                System.out.println("--- Scrape Complete ---");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.HOURS); // 0 delay to start now, then every 1 hour
    }
}