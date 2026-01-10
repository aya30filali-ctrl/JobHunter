package com.jobapp.scraping;

import com.jobapp.model.Offre;
import com.jobapp.ml.JobClassifier; // <--- 1. NEW IMPORT
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

public class RekruteScraper implements JobScraper {

    // 2. Instantiate the Classifier (The "Brain")
    private JobClassifier classifier = new JobClassifier();

    public List<Offre> scrape(String keyword) {
        List<Offre> list = new ArrayList<>();
        String url = "https://www.rekrute.com/offres-emploi-maroc.html?s=" + keyword + "&p=1";
        
        try {
            // Connect to the website
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(60000) // <--- CHANGE TO 60000 (60 Seconds)
                    .get();

            // Select the Job Card
            Elements jobs = doc.select("div.job-listing");
            
            System.out.println("DEBUG: Found " + jobs.size() + " job cards.");

            for (Element job : jobs) {
                // --- A. Extract Title ---
                String title = job.select("span.rekh3").text();
                if (title.isEmpty()) continue;

                // --- B. Extract Company & City ---
                String company = "Confidential";
                String city = "Maroc";
                String rawInfo = "";

                Element icon = job.select("i.la-building").first();
                if (icon != null) {
                    rawInfo = icon.parent().text();
                }

                if (rawInfo.contains("|")) {
                    String[] parts = rawInfo.split("\\|");
                    company = parts[0].trim();
                    if (parts.length > 1) {
                        city = parts[1].trim();
                    }
                } else if (!rawInfo.isEmpty()) {
                    company = rawInfo.trim();
                }

                // --- C. Extract URL ---
                String link = job.parent().attr("href");
                if (!link.startsWith("http")) {
                    link = "https://www.rekrute.com" + link;
                }

                // --- D. Extract Description ---
                String desc = job.text(); 

                // --- E. NEW STEP: CLASSIFY THE JOB ---
                // We send the Title + Description to the AI model to guess the category
                String category = classifier.predictCategory(title + " " + desc);

                // --- F. Create Object (Now with 6 arguments) ---
                list.add(new Offre(title, desc, city, company, link, category));
            }
        } catch (Exception e) {
            System.err.println("Scraping Error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}