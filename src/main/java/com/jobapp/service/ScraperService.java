package com.jobapp.service;

import com.jobapp.dao.JobDAO;
import com.jobapp.model.Offre;
import com.jobapp.scraping.EmploiMaScraper;
import com.jobapp.scraping.JobScraper;
import com.jobapp.scraping.RekruteScraper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScraperService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JobDAO jobDAO = new JobDAO();

    // List to hold multiple websites
    private final List<JobScraper> scrapers = new ArrayList<>();

    public ScraperService() {
        // 1. Add Rekrute
        scrapers.add(new RekruteScraper());
        // 2. Add Emploi.ma
        scrapers.add(new EmploiMaScraper());
    }

    public void startAutoUpdate() {
        Runnable task = () -> {
            System.out.println("--- 🕒 AUTO-UPDATE STARTED ---");

            for (JobScraper scraper : scrapers) {
                System.out.println(">> Scraping source: " + scraper.getClass().getSimpleName() + "...");

                // Scrape jobs
                List<Offre> jobs = scraper.scrape("java");

                // Save jobs using the NEW correct method
                for (Offre job : jobs) {
                    // ERROR FIX: We use saveJob() instead of saveJobAndScore()
                    jobDAO.saveJob(job);
                }
            }
            System.out.println("--- ✅ UPDATE FINISHED ---");
        };

        // Run every 10 SECONDS (for testing)
        // Change back to TimeUnit.HOURS when you are done testing
        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }
}