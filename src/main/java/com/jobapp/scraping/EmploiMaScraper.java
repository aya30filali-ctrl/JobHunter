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
        int maxPages = 5; // Limité pour les tests

        for (int i = 0; i < maxPages; i++) {
            String url = "https://www.emploi.ma/recherche-jobs-maroc/" + keyword + "?page=" + i;

            try {
                System.out.println("   -> Connecting to Emploi.ma (Page " + (i+1) + ")...");
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(30000)
                        .get();

                Elements jobs = doc.select("div.card-job");
                if (jobs.isEmpty()) break;

                for (Element job : jobs) {
                    Element titleEl = job.select("h3 a").first();
                    if (titleEl == null) continue;

                    String title = titleEl.text().trim();
                    String link = titleEl.attr("href");
                    if (!link.startsWith("http")) link = "https://www.emploi.ma" + link;

                    String company = "Confidential";
                    Element compEl = job.select(".card-job-company").first();
                    if (compEl != null) company = compEl.text().trim();

                    String desc = job.select(".card-job-description").text();
                    String city = "Maroc";

                    // Logique d'extraction ville

                    if (desc.contains("Région de :")) {
                        try {
                            String[] parts = desc.split("Région de :");
                            if (parts.length > 1) {
                                city = parts[1].split("Compétences")[0].trim();
                                if (city.length() > 20) city = "Maroc"; // Fallback sécurité
                            }
                        } catch (Exception e) {}
                    }

                    desc = desc.replaceAll("\\d{2}\\.\\d{2}\\.\\d{4}", ""); // Nettoyage date
                    String category = classifier.predictCategory(title + " " + desc);


                    list.add(new Offre(title, company, city, desc, link, category));
                }

            } catch (Exception e) {
                System.err.println("Emploi.ma Error: " + e.getMessage());
            }
        }
        return list;
    }
}