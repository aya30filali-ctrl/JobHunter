package com.jobapp.scraping;

import com.jobapp.ml.JobClassifier;
import com.jobapp.model.Offre;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

public class RekruteScraper implements JobScraper {

    private JobClassifier classifier = new JobClassifier();

    @Override
    public List<Offre> scrape(String keyword) {
        List<Offre> list = new ArrayList<>();

        // --- UPDATE: INCREASED TO 20 PAGES ---
        int maxPages = 70;
        // -------------------------------------

        for (int i = 1; i <= maxPages; i++) {
            String url = "https://www.rekrute.com/offres-emploi-maroc.html?s=" + keyword + "&p=" + i;

            try {
                System.out.println("   -> Connecting to Page " + i + "...");
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .timeout(60000)
                        .get();

                // Select jobs (Fallback logic included)
                Elements jobs = doc.select("li.post-id");
                if (jobs.isEmpty()) jobs = doc.select("div.job-listing");

                System.out.println("      Found " + jobs.size() + " jobs on page " + i);

                if (jobs.isEmpty()) break; // Stop if no more jobs

                for (Element job : jobs) {
                    // 1. Title
                    String title = job.select("h2").text();
                    if (title.isEmpty()) title = job.select("span.rekh3").text();
                    if (title.isEmpty()) continue;

                    String company = "Confidential";
                    String city = "Maroc";

                    // 2. Intelligent Extraction (Title Splitter for City)
                    if (title.contains("|")) {
                        String[] parts = title.split("\\|");
                        title = parts[0].trim();
                        city = parts[1].replace("(Maroc)", "").trim();
                    }

                    // 3. Company Extraction
                    String imgTitle = job.select("img").attr("title");
                    if (!imgTitle.isEmpty()) company = imgTitle;

                    // 4. Link & Desc
                    String link = job.select("a").attr("href");
                    if (!link.startsWith("http")) link = "https://www.rekrute.com" + link;
                    String desc = job.text();

                    // 5. Classify
                    String category = classifier.predictCategory(title + " " + desc);

                    list.add(new Offre(title, desc, city, company, link, category));
                }

            } catch (Exception e) {
                System.err.println("Error on page " + i + ": " + e.getMessage());
            }
        }

        System.out.println(">>> REKRUTE FINISHED. Total Jobs Scraped: " + list.size());
        return list;
    }
}