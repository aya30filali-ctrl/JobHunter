package com.jobapp.service;

import java.util.*;
import java.text.Normalizer;


public class NeuralMatchingEngine {

    // 1. LISTE DE MOTS "POISON" (BANNIS)
    // Si l'un de ces mots est dans le TITRE du job, le score est forcé à 0.
    // Cela élimine radicalement les métiers manuels ou hors-sujet.
    private static final Set<String> POISON_TITLES = new HashSet<>(Arrays.asList(
            "chauffeur", "conducteur", "coursier", "livreur", "transport", "permis",
            "operateur", "operatrice", "usine", "ouvrier", "manoeuvre", "technicien maintenance",
            "vendeur", "vendeuse", "caissier", "commercial", "teleconseiller", "call center",
            "magasinier", "receptionniste", "assistant", "secretaire", "menage", "gardien",
            "agent", "securite", "animatrice", "coiffeur", "estheticienne", "serveur", "cuisine"
    ));

    // 2. LISTE DE MOTS TECHNIQUES OBLIGATOIRES (WHITELIST)
    // Une offre DOIT contenir au moins UN de ces mots pour être considérée.
    private static final Set<String> MANDATORY_TECH_TOKENS = new HashSet<>(Arrays.asList(
            "java", "python", "javascript", "typescript", "php", "c#", "c++", "ruby", "swift", "kotlin",
            "sql", "mysql", "postgresql", "oracle", "mongodb", "nosql",
            "react", "angular", "vue", "node", "django", "spring", "laravel", "symfony", "flutter",
            "docker", "kubernetes", "aws", "azure", "cloud", "devops", "ci/cd", "git",
            "machine learning", "big data", "data scientist", "data analyst", "ai", "intelligence",
            "fullstack", "backend", "frontend", "developer", "developpeur", "ingenieur", "engineer",
            "web", "mobile", "software", "informatique", "reseaux", "systeme"
    ));

    // 3. STOP WORDS (Mots vides à ignorer)
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "le", "la", "les", "de", "du", "des", "un", "une", "et", "est", "pour", "dans", "par",
            "avec", "sur", "job", "offre", "poste", "profil", "recherche", "entreprise", "maroc",
            "stage", "experience", "niveau", "date", "ville", "secteur", "mission", "equipe",
            "client", "gestion", "projet", "motivation", "nous", "vous", "annee", "bac"
    ));

    public int calculateMatchScore(String cvText, String jobTitle, String jobDescription) {
        if (cvText == null || jobTitle == null) return 0;

        // PHASE 1 : DÉTECTION DU PROFIL CANDIDAT
        // On vérifie si le CV est "IT/Tech". Si oui, on active le filtrage agressif.
        boolean isCandidateTech = isTechProfile(cvText);

        String titleLower = normalize(jobTitle);
        String descLower = normalize(jobDescription != null ? jobDescription : "");
        String fullJobText = titleLower + " " + descLower;

        //  PHASE 2 : LE "KILL SWITCH" (POISON)
        // Si le candidat est Tech, on tue toute offre contenant des mots poison dans le titre.
        if (isCandidateTech) {
            for (String poison : POISON_TITLES) {
                // On cherche le mot exact (ex: éviter de bannir "Operateur" si c'est "Operateur Saisie" mais ici c'est strict)
                if (titleLower.contains(poison)) {
                    // Exception : Si le titre contient aussi "Développeur" ou "Ingénieur", on pardonne
                    // (Ex: "Ingénieur Commercial" -> Acceptable ? Non, on bannit aussi pour un dev)
                    return 0;
                }
            }

            // PHASE 3 : LE "MUST HAVE" (WHITELIST)
            // L'offre contient-elle au moins 1 mot technique majeur ?
            // Cela élimine "Opérateur de machine" car il ne contient pas "Java", "Python", "Data", etc.
            boolean hasTechKeyword = false;
            for (String token : MANDATORY_TECH_TOKENS) {
                if (fullJobText.contains(token)) {
                    hasTechKeyword = true;
                    break;
                }
            }
            // Si l'offre ne parle d'aucune techno connue, poubelle.
            if (!hasTechKeyword) return 0;
        }

        //PHASE 4 : CALCUL DU SCORE CLASSIQUE
        // Si on arrive ici, l'offre est "propre". On calcule la pertinence.

        return calculateStandardScore(cvText, fullJobText, titleLower);
    }

    private int calculateStandardScore(String cvText, String jobText, String jobTitle) {
        Set<String> cvWords = extractWords(cvText);
        Set<String> jobWords = extractWords(jobText);

        double score = 0;
        double totalWeight = 0;

        for (String word : jobWords) {
            double weight = 1.0;

            // Les mots techniques valent 10 fois plus

            if (MANDATORY_TECH_TOKENS.contains(word)) weight = 10.0;

            // Les mots du titre valent 3 fois plus

            if (jobTitle.contains(word)) weight += 3.0;

            totalWeight += weight;

            if (cvWords.contains(word)) {
                score += weight;
            }
        }

        if (totalWeight == 0) return 0;

        double percentage = (score / totalWeight) * 100;

        // Boost final pour les titres exacts

        if (jobTitle.contains("developpeur") || jobTitle.contains("developer") || jobTitle.contains("data")) {
            percentage += 15;
        }

        return (int) Math.min(100, percentage);
    }

    /**
     * Vérifie si le CV contient assez de mots clés Tech pour être considéré comme un profil IT.
     */
    private boolean isTechProfile(String cvText) {
        int techCount = 0;
        String normalizedCV = normalize(cvText);
        for (String token : MANDATORY_TECH_TOKENS) {
            if (normalizedCV.contains(token)) techCount++;
        }
        return techCount >= 3; // Si le CV a au moins 3 mots techniques (ex: Java, SQL, Web)
    }

    private Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();
        if (text == null) return words;
        String normalized = normalize(text);
        for (String w : normalized.split("\\s+")) {
            if (w.length() > 2 && !STOP_WORDS.contains(w)) {
                words.add(w);
            }
        }
        return words;
    }

    private String normalize(String text) {
        if (text == null) return "";
        // Enlève les accents et garde uniquement lettres et chiffres (et + # pour C++ C#)
        return Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9+#]", " ");
    }

    // Compatibilité

    public int calculateMatchScore(String cvText, String fullText) {
        return calculateMatchScore(cvText, fullText, fullText);
    }
}