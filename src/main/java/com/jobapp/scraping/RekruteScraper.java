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
        int maxPages = 20; // Réduit pour éviter les blocages, augmentez si nécessaire

        for (int i = 1; i <= maxPages; i++) {
            String url = "https://www.rekrute.com/offres-emploi-maroc.html?s=" + keyword + "&p=" + i;

            try {
                System.out.println("   -> Connecting to Page " + i + "...");
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .timeout(60000)
                        .get();

                Elements jobs = doc.select("li.post-id");
                if (jobs.isEmpty()) jobs = doc.select("div.job-listing");
                if (jobs.isEmpty()) break;

                for (Element job : jobs) {
                    String title = job.select("h2").text();
                    if (title.isEmpty()) title = job.select("span.rekh3").text();
                    if (title.isEmpty()) continue;

                    String company = "Confidential";
                    String city = "Maroc";

                    if (title.contains("|")) {
                        String[] parts = title.split("\\|");
                        title = parts[0].trim();
                        city = parts[1].replace("(Maroc)", "").trim();
                    }

                    String imgTitle = job.select("img").attr("title");
                    if (!imgTitle.isEmpty()) company = imgTitle;

                    String link = job.select("a").attr("href");
                    if (!link.startsWith("http")) link = "https://www.rekrute.com" + link;
                    String desc = job.text();

                    String category = classifier.predictCategory(title + " " + desc);

                    // --- CORRECTION MAJEURE ICI ---
                    // Ordre : Titre, ENTREPRISE, Ville, DESCRIPTION, Url, Categorie
                    list.add(new Offre(title, company, city, desc, link, category));
                }

            } catch (Exception e) {
                System.err.println("Error on page " + i + ": " + e.getMessage());
            }
        }
        return list;
    }
}