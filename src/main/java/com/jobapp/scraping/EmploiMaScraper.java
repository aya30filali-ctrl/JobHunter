package com.jobapp.scraping;

import com.jobapp.ml.JobClassifier;
import com.jobapp.model.Offre;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

public class EmploiMaScraper implements JobScraper {

    private JobClassifier classifier = new JobClassifier();

    @Override
    public List<Offre> scrape(String keyword) {
        List<Offre> list = new ArrayList<>();
        String url = "https://www.emploi.ma/recherche-jobs-maroc/" + keyword;

        try {
            System.out.println("   -> Connecting to: " + url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(30000)
                    .get();

            // STRATEGY: Look for the main table rows (Drupal View Structure)
            // 'div.view-content' usually holds the list in Drupal sites like Emploi.ma
            Elements jobs = doc.select("div.view-content div.views-row");

            // Backup: If that fails, just grab ANY row with a job title link
            if (jobs.isEmpty()) {
                System.out.println("   -> Standard selector failed. Trying 'Brute Force'...");
                // Select any div that has a class starting with 'row' and contains a link
                jobs = doc.select("div[class*=row]:has(h5)");
            }

            System.out.println("DEBUG (Emploi.ma): Found " + jobs.size() + " jobs.");

            for (Element job : jobs) {
                // Title is usually in h5 > a
                String title = job.select("h5").text();

                // If no h5, try just finding the first bold link
                if (title.isEmpty()) title = job.select("strong a").text();
                if (title.isEmpty()) continue; // Skip if no title

                String company = job.select(".company-name").text();
                if (company.isEmpty()) company = "Confidential";

                String desc = job.text();

                // Link extraction
                String link = job.select("a").attr("href");
                if (!link.startsWith("http")) link = "https://www.emploi.ma" + link;

                String city = "Maroc";
                // Attempt to find city in the text (e.g., "Rabat")
                if (desc.contains("Casablanca")) city = "Casablanca";
                else if (desc.contains("Rabat")) city = "Rabat";
                else if (desc.contains("Tanger")) city = "Tanger";

                String category = classifier.predictCategory(title + " " + desc);

                list.add(new Offre(title, desc, city, company, link, category));
            }

        } catch (Exception e) {
            System.err.println("Emploi.ma Error: " + e.getMessage());
        }
        return list;
    }
}