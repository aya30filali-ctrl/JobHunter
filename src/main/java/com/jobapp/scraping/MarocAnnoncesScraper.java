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
        int maxPages = 5;

        for (int i = 1; i <= maxPages; i++) {
            String url = "https://www.marocannonces.com/maroc/offres-emploi-b309.html?kw=" + keyword + "&pge=" + i;

            try {
                System.out.println("   -> Connecting to MarocAnnonces (Page " + i + ")...");
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(30000)
                        .get();

                Elements jobs = doc.select(".listing_set article, ul.cars-list li");
                if (jobs.isEmpty()) break;

                for (Element job : jobs) {
                    try {
                        Element titleEl = job.select("h3").first();
                        if (titleEl == null) continue;
                        String title = titleEl.text().trim();

                        Element linkEl = job.select("a").first();
                        String link = (linkEl != null) ? linkEl.attr("abs:href") : "";

                        String city = "Maroc";
                        Element cityEl = job.select(".location").first();
                        if (cityEl != null) city = cityEl.text().trim();

                        String company = "Confidential / MarocAnnonces";
                        Element imgEl = job.select("img").first();
                        if (imgEl != null && imgEl.hasAttr("alt")) {
                            String alt = imgEl.attr("alt").trim();
                            if (!alt.isEmpty() && !alt.equalsIgnoreCase(title)) company = alt;
                        }

                        // Description virtuelle

                        String desc = title + " - " + city;
                        Element extraInfo = job.select(".block em").first();
                        if (extraInfo != null) desc += " " + extraInfo.text();

                        String category = classifier.predictCategory(desc);



                        list.add(new Offre(title, company, city, desc, link, category));

                    } catch (Exception innerEx) {}
                }
            } catch (Exception e) {
                System.err.println("MarocAnnonces Error: " + e.getMessage());
            }
        }
        return list;
    }
}