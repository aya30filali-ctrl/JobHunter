package com.jobapp.scraping;

import com.jobapp.ml.JobClassifier;
import com.jobapp.model.Offre;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

public class MarocAnnoncesScraper implements JobScraper {

    private JobClassifier classifier = new JobClassifier();

    @Override
    public List<Offre> scrape(String keyword) {
        List<Offre> list = new ArrayList<>();

        // MarocAnnonces allows 'pge' parameter.
        // We limit to 5 pages for speed (approx 100 jobs). Increase if needed.
        int maxPages = 60;

        for (int i = 1; i <= maxPages; i++) {
            // URL Construction
            String url = "https://www.marocannonces.com/maroc/offres-emploi-b309.html?kw=" + keyword + "&pge=" + i;

            try {
                System.out.println("   -> Connecting to MarocAnnonces (Page " + i + "): " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .timeout(30000)
                        .get();

                // SELECTOR BASED ON YOUR SCREENSHOT:
                // The list items are inside 'div.listing_set' and are usually '<article>' or '<li>'
                // We try a broad selector to catch both layouts
                Elements jobs = doc.select(".listing_set article, ul.cars-list li");

                System.out.println("      Found " + jobs.size() + " jobs on this page.");
                if (jobs.isEmpty()) break;

                for (Element job : jobs) {
                    try {
                        // 1. Extract Title (Inside <h3>)
                        Element titleEl = job.select("h3").first();
                        if (titleEl == null) continue;
                        String title = titleEl.text().trim();

                        // 2. Extract Link (The <a> tag usually wraps the image or title)
                        Element linkEl = job.select("a").first();
                        String link = (linkEl != null) ? linkEl.attr("abs:href") : "";

                        // 3. Extract City (Inside <span class="location">)
                        String city = "Maroc";
                        Element cityEl = job.select(".location").first();
                        if (cityEl != null) {
                            city = cityEl.text().trim();
                        }

                        // 4. Extract Company (Tricky on List View)
                        // MarocAnnonces often doesn't show the company name text on the list, only a logo.
                        // We set a default, or try to get it from the image Alt text if available.
                        String company = "Confidential / MarocAnnonces";
                        Element imgEl = job.select("img").first();
                        if (imgEl != null && imgEl.hasAttr("alt")) {
                            // Sometimes the alt text is the company, sometimes it's the title.
                            // We use it if it's different from the title.
                            String altText = imgEl.attr("alt").trim();
                            if (!altText.equalsIgnoreCase(title) && altText.length() < 50) {
                                company = altText;
                            }
                        }

                        // 5. Construct Description
                        // The list view DOES NOT have a full description.
                        // Strategy: We combine Title + City + any tags we find to create a "Virtual Description" for the AI.
                        String desc = title + " - " + city;

                        // Try to find extra info (like "Domaine" or "Fonction" if visible)
                        Element extraInfo = job.select(".block em").first();
                        if (extraInfo != null) {
                            desc += " " + extraInfo.text();
                        }

                        // 6. Classify
                        String category = classifier.predictCategory(desc);

                        // 7. Add to List
                        list.add(new Offre(title, desc, city, company, link, category));

                    } catch (Exception innerEx) {
                        System.err.println("      x Error parsing specific ad: " + innerEx.getMessage());
                    }
                }

            } catch (Exception e) {
                System.err.println("   -> MarocAnnonces Error on page " + i + ": " + e.getMessage());
            }
        }

        System.out.println(">>> MAROCANNONCES FINISHED. Total Jobs: " + list.size());
        return list;
    }
}