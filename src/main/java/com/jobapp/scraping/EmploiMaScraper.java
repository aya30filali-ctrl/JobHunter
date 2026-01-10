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
        // Emploi.ma search URL
        String url = "https://www.emploi.ma/recherche-jobs-maroc/" + keyword;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            // Emploi.ma uses 'div.job-search-result' for job cards
            Elements jobs = doc.select("div.job-search-result");
            System.out.println("DEBUG (Emploi.ma): Found " + jobs.size() + " jobs.");

            for (Element job : jobs) {
                String title = job.select("h5").text();
                String company = job.select("a.company-name").text();
                if (company.isEmpty()) company = "Confidential";

                String desc = job.select("div.search-description").text();
                String link = "https://www.emploi.ma" + job.select("h5 a").attr("href");
                String city = "Maroc";

                // Classify the job
                String category = classifier.predictCategory(title + " " + desc);

                list.add(new Offre(title, desc, city, company, link, category));
            }

        } catch (Exception e) {
            System.err.println("Emploi.ma Error: " + e.getMessage());
        }
        return list;
    }
}