package com.jobapp.service;

import java.util.*;

public class NeuralMatchingEngine {

    /**
     * V4 DEBUG : Cette version affiche tout dans la console pour comprendre le "0".
     */
    public int calculateMatchScore(String cvText, String jobDescription) {
        // 1. VÉRIFICATION DES ENTRÉES
        if (cvText == null || cvText.trim().isEmpty()) {
            System.out.println("⚠️ ALERTE : Le texte du CV est VIDE ou NULL !");
            return 0;
        }
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            // C'est souvent ici le problème : le scraper ne remplit pas bien la description
            // On ne spamme pas la console pour chaque job vide, on retourne juste 0
            return 0;
        }

        // 2. DÉCOUPAGE SIMPLE (Sans Regex complexe pour éviter de tout supprimer)
        Set<String> cvWords = simpleTokenize(cvText);
        Set<String> jobWords = simpleTokenize(jobDescription);

        // 3. COMPARAISON
        long matches = 0;
        List<String> matchedWords = new ArrayList<>(); // Pour voir quels mots matchent

        for (String word : cvWords) {
            if (jobWords.contains(word)) {
                matches++;
                matchedWords.add(word);
            }
        }

        // 4. CALCUL DU SCORE (Formule simplifiée)
        // Score = (Mots trouvés / Racine carrée de la taille du CV) * 100
        // On multiplie par 3.0 pour gonfler artificiellement le score et voir si ça bouge.
        double rawScore = 0;
        if (cvWords.size() > 0) {
            rawScore = (double) matches / cvWords.size() * 100 * 3.0;
        }

        int finalScore = (int) Math.min(100, rawScore);

        // 5. DEBUGGING (S'affiche seulement si on trouve un début de match ou pour le premier job)
        // Pour ne pas spammer, on affiche seulement si le score > 0
        if (finalScore > 0) {
            System.out.println("✅ MATCH TROUVÉ ! Score: " + finalScore + "%");
            System.out.println("   Mots communs : " + matchedWords);
        }

        return finalScore;
    }

    /**
     * Nettoyage ultra-simple pour être sûr de ne rien perdre.
     */
    private Set<String> simpleTokenize(String text) {
        // Mettre en minuscule
        String lower = text.toLowerCase();

        // Remplacer les caractères non-lettres par des espaces
        // On garde les chiffres (c# -> c, java8 -> java8)
        String cleaned = lower.replaceAll("[^a-z0-9]", " ");

        // Couper aux espaces
        String[] parts = cleaned.split("\\s+");

        Set<String> tokens = new HashSet<>();
        for (String p : parts) {
            // On garde tout ce qui fait plus de 2 lettres
            if (p.length() > 2) {
                tokens.add(p);
            }
        }
        return tokens;
    }
}