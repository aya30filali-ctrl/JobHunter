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

        // Emploi.ma uses strict pagination: ?page=0, ?page=1, etc.
        // You found 700 jobs (28 pages), so let's scrape the first 5 pages to be safe and fast.
        int maxPages = 28;

        for (int i = 0; i < maxPages; i++) {
            String url = "https://www.emploi.ma/recherche-jobs-maroc/" + keyword + "?page=" + i;

            try {
                System.out.println("   -> Connecting to Emploi.ma (Page " + (i+1) + "): " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .timeout(30000)
                        .get();

                // 1. Target the exact card class from your screenshot
                Elements jobs = doc.select("div.card-job");

                System.out.println("      Found " + jobs.size() + " jobs on this page.");
                if (jobs.isEmpty()) break;

                for (Element job : jobs) {
                    // 2. Title is inside h3 > a
                    Element titleEl = job.select("h3 a").first();
                    if (titleEl == null) continue;

                    String title = titleEl.text().trim();
                    String link = titleEl.attr("href");
                    if (!link.startsWith("http")) link = "https://www.emploi.ma" + link;

                    // 3. Company is inside a.card-job-company
                    String company = "Confidential";
                    Element compEl = job.select(".card-job-company").first();
                    if (compEl != null) company = compEl.text().trim();

                    // 4. Description & City Extraction (The Logic Upgrade)
                    String desc = job.select(".card-job-description").text();
                    String city = "Maroc"; // Default

                    // Look for "Région de :" which is clearly visible in your screenshot
                    if (desc.contains("Région de :")) {
                        try {
                            // Split the text to find the region
                            // Example: "Contrat proposé : CDI Région de : Agadir Compétences clés..."
                            String[] parts = desc.split("Région de :");
                            if (parts.length > 1) {
                                // Take the part AFTER "Région de :"
                                String afterRegion = parts[1].trim();
                                // The city is usually the first few words before the next sentence or date
                                // We split by " " and take the first 2 words (e.g., "Casablanca-Mohammedia" or "Rabat Salé")
                                int stopIndex = Math.min(afterRegion.length(), 20); // Safety limit
                                city = afterRegion.substring(0, stopIndex).split("Compétences")[0].trim();

                                // Cleanup: Remove trailing dates or dots
                                city = city.replaceAll("[0-9]", "").replace("-", " ").trim();
                            }
                        } catch (Exception e) {
                            // Keep default if parsing fails
                        }
                    }
                    // Fallback: Check Title for City (e.g. "Développeur - Agadir")
                    else if (title.contains("-")) {
                        String[] titleParts = title.split("-");
                        String potentialCity = titleParts[titleParts.length - 1].trim();
                        // Ensure it's a city and not a job code
                        if (potentialCity.length() > 3 && potentialCity.length() < 15) {
                            city = potentialCity;
                        }
                    }

                    // 5. Clean the date from description (Optional)
                    // Your screenshot shows dates like "10.01.2026" at the end.
                    // We remove it to keep the description clean.
                    desc = desc.replaceAll("\\d{2}\\.\\d{2}\\.\\d{4}", "");

                    // 6. Classify
                    String category = classifier.predictCategory(title + " " + desc);

                    list.add(new Offre(title, desc, city, company, link, category));
                }

            } catch (Exception e) {
                System.err.println("Emploi.ma Error on page " + i + ": " + e.getMessage());
            }
        }

        System.out.println(">>> EMPLOI.MA FINISHED. Total Jobs: " + list.size());
        return list;
    }
}