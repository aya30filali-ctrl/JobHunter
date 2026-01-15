package com.jobapp.service;

import com.jobapp.dao.JobDAO;
import com.jobapp.model.Offre;
import com.jobapp.scraping.EmploiMaScraper;
import com.jobapp.scraping.JobScraper;
import com.jobapp.scraping.RekruteScraper;
import com.jobapp.model.DataList; //

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScraperService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JobDAO jobDAO = new JobDAO();

    // Liste des scrapers disponibles
    private final List<JobScraper> scrapers = new ArrayList<>();

    public ScraperService() {
        // Enregistrement des sources de données
        scrapers.add(new RekruteScraper());
        scrapers.add(new EmploiMaScraper());
    }

    public void startAutoUpdate() {
        Runnable task = () -> {
            System.out.println("--- 🕒 AUTO-UPDATE STARTED ---");



            DataList<Offre> jobList = new DataList<>();

            for (JobScraper scraper : scrapers) {
                System.out.println(">> Scraping source: " + scraper.getClass().getSimpleName() + "...");


                List<Offre> found = scraper.scrape("java");


                for (Offre job : found) {
                    jobList.add(job);
                    jobDAO.saveJob(job);
                }
            }


            System.out.println("--- UPDATE FINISHED : " + jobList.size() + " offres traitées ---");
        };


        scheduler.scheduleAtFixedRate(task, 0, 4, TimeUnit.HOURS);
    }
}