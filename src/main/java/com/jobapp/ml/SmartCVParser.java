package com.jobapp.ml;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class SmartCVParser {

    public String extractSearchQuery(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "java";

        String predictedCategory = "java"; // Valeur par défaut

        try {
            // 1. On cherche le cerveau qu'on vient de créer
            File modelFile = new File("recruitment_model.bin");

            if (modelFile.exists()) {
                try (InputStream modelIn = new FileInputStream(modelFile)) {
                    // 2. On charge le modèle
                    DoccatModel model = new DoccatModel(modelIn);
                    DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);

                    // 3. On nettoie le texte du CV (minuscules, sans caractères spéciaux)
                    String[] tokens = rawText.toLowerCase()
                            .replaceAll("[^a-z0-9 ]", " ")
                            .split("\\s+");

                    // 4. L'IA devine la catégorie
                    double[] outcomes = myCategorizer.categorize(tokens);
                    predictedCategory = myCategorizer.getBestCategory(outcomes);

                    System.out.println("🧠 IA a prédit le profil : " + predictedCategory);
                }
            } else {
                System.out.println("⚠️ Fichier .bin introuvable, utilisation du fallback.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // On enlève les underscores pour la recherche (ex: Java_Dev -> Java Dev)
        return predictedCategory.replace("_", " ");
    }
}