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

        // On parcourt les 5 premières pages pour obtenir environ 100 offres
        for (int page = 1; page <= 80; page++) {
            String url = "https://www.marocannonces.com/maroc/offres-emploi-b309.html?kw=" + keyword + "&pge=" + page;
            try {
                System.out.println("   -> Page " + page + " : Connexion à la liste...");
                Document listDoc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(30000)
                        .get();

                Elements ads = listDoc.select("ul.cars-list li");
                if (ads.isEmpty()) break;

                for (Element ad : ads) {
                    Element linkTag = ad.selectFirst("a");
                    if (linkTag == null) continue;

                    String title = ad.select("h3").text().trim();
                    String detailUrl = linkTag.attr("abs:href");

                    // --- ACCÈS À LA PAGE DE DÉTAIL POUR CHAQUE OFFRE ---
                    Document detailDoc = Jsoup.connect(detailUrl)
                            .userAgent("Mozilla/5.0")
                            .timeout(20000)
                            .get();

                    // 1. EXTRACTION DE L'ENTREPRISE (Basé sur ton code HTML <li>)
                    String company = detailDoc.select("li:contains(Entreprise) a").text().trim();
                    if (company.isEmpty()) {
                        company = "Entreprise non précisée";
                    }

                    // 2. EXTRACTION DE LA DESCRIPTION (Ciblage et Nettoyage)
                    // On cible la div.block dans le contenu principal
                    Element blockDiv = detailDoc.select("#main div.block").first();
                    String description = "";

                    if (blockDiv != null) {
                        // On clone pour ne pas modifier la page originale pendant le nettoyage
                        Element cleanDesc = blockDiv.clone();

                        // Suppression des éléments parasites (Photo, Questions, Google Ads, Scripts)
                        cleanDesc.select("#photo_column, .box1, #extra_questions, .google-anno-skip, script, style").remove();

                        // Récupération du texte de l'annonce uniquement
                        description = cleanDesc.text().trim();
                    }

                    if (description.isEmpty()) {
                        description = "Description détaillée disponible sur le site.";
                    }

                    // 3. EXTRACTION DU DOMAINE ET CATÉGORIE
                    String domaineSite = detailDoc.select("li:contains(Domaine) a").text().trim();

                    // On utilise le domaine du site + le titre pour aider l'IA
                    String category = classifier.predictCategory(domaineSite + " " + title);

                    // Si l'IA renvoie "Autre", on force la catégorie avec le Domaine du site
                    if ((category.equalsIgnoreCase("Autre") || category.isEmpty()) && !domaineSite.isEmpty()) {
                        category = domaineSite;
                    }

                    // 4. EXTRACTION DE LA VILLE
                    String city = detailDoc.select("li:contains(Ville)").text().replace("Ville :", "").trim();
                    if (city.isEmpty()) {
                        city = ad.select(".location").text().trim(); // Repli sur la ville de la liste
                    }
                    if (city.isEmpty()) city = "Maroc";

                    // Ajout à la liste finale
                    list.add(new Offre(title, description, city, company, detailUrl, category));

                    System.out.println("      [OK] " + title + " | " + company + " | " + category);

                    // Pause de 500ms pour être respectueux envers le serveur
                    Thread.sleep(500);
                }

            } catch (Exception e) {
                System.err.println("   -> Erreur sur une offre de la page " + page + " : " + e.getMessage());
            }
        }
        return list;
    }
}//test