package com.jobapp.ml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class UniversalCsvConverter {

    public static void main(String[] args) {

        String inputFile = "Resume.csv";

        String outputFile = "training_data.txt";

        // MAPPING : On traduit les 24 catégories du CSV en standards Marocains

        Map<String, String> categoryMap = new HashMap<>();

        // 1. Pôle IT & Digital

        categoryMap.put("INFORMATION-TECHNOLOGY", "Informatique_Dev");
        categoryMap.put("DIGITAL-MEDIA", "Informatique_Web");
        categoryMap.put("WEB-DESIGNING", "Informatique_Design");
        categoryMap.put("DATA-SCIENCE", "Informatique_Data");

        // 2. Pôle Business & Support

        categoryMap.put("BUSINESS-DEVELOPMENT", "Commercial_BizDev");
        categoryMap.put("SALES", "Commercial_Vente");
        categoryMap.put("HR", "Ressources_Humaines");
        categoryMap.put("FINANCE", "Finance_Analyse");
        categoryMap.put("BANKING", "Banque");
        categoryMap.put("ACCOUNTANT", "Comptabilite");
        categoryMap.put("CONSULTANT", "Consulting");
        categoryMap.put("PUBLIC-RELATIONS", "Communication");
        categoryMap.put("BPO", "Centre_Appel_Offshoring");
        categoryMap.put("ADVOCATE", "Juridique");

        // 3. Pôle Industrie & Ingénierie

        categoryMap.put("ENGINEERING", "Ingenierie_Industrie");
        categoryMap.put("AUTOMOBILE", "Industrie_Automobile");
        categoryMap.put("AVIATION", "Aeronautique");
        categoryMap.put("CONSTRUCTION", "BTP_GenieCivil");
        categoryMap.put("APPAREL", "Textile_Mode");
        categoryMap.put("AGRICULTURE", "Agriculture");

        // 4. Services & Santé

        categoryMap.put("TEACHER", "Enseignement");
        categoryMap.put("HEALTHCARE", "Sante_Medical");
        categoryMap.put("FITNESS", "Sport_Coach");
        categoryMap.put("CHEF", "Hotellerie_Restauration");
        categoryMap.put("DESIGNER", "Art_Design");
        categoryMap.put("ARTS", "Art_Culture");

        System.out.println(" Démarrage de la conversion du Dataset (2400+ CVs)...");

        try (Scanner scanner = new Scanner(new File(inputFile), StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {

            // On saute la première ligne (les titres des colonnes)

            if (scanner.hasNextLine()) scanner.nextLine();

            int count = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Le fichier est structuré : ID, Resume_Text, Resume_HTML, Category
                // La catégorie est tout à la fin. On cherche la dernière virgule.

                int lastCommaIndex = line.lastIndexOf(',');

                if (lastCommaIndex != -1) {
                    // Extraction
                    String categoryRaw = line.substring(lastCommaIndex + 1).trim().replace("\"", "");
                    String contentRaw = line.substring(0, lastCommaIndex); // Le reste du texte

                    // Traduction de la catégorie

                    if (categoryMap.containsKey(categoryRaw)) {
                        String targetCat = categoryMap.get(categoryRaw);

                        // Nettoyage radical du texte (on enlève le HTML, les sauts de ligne, les symboles bizarres)

                        String cleanContent = contentRaw
                                .replace("\"", "") // Enlever guillemets
                                .replaceAll("<.*?>", " ") // Enlever balises HTML
                                .replaceAll("\\r\\n|\\r|\\n", " ") // Enlever sauts de ligne
                                .replaceAll("[^a-zA-Z0-9éèàê .@+]", " ") // Garder lettres, chiffres, @ et + (C++)
                                .replaceAll("\\s+", " ") // Réduire les espaces multiples
                                .trim();

                        // On écrit seulement si le CV contient assez de texte (> 50 caractères)

                        if (cleanContent.length() > 50) {
                            writer.write(targetCat + " " + cleanContent);
                            writer.newLine();
                            count++;
                        }
                    }
                }
            }
            System.out.println(" Conversion Terminée !");
            System.out.println( + count + " CVs ont été nettoyés et traduits.");
            System.out.println(" Fichier prêt : " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(" ERREUR : Le fichier 'Resume.csv' est introuvable à la racine du projet.");
        }
    }
}