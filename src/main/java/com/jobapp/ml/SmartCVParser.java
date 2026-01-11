package com.jobapp.ml;

import java.util.*;
import java.util.stream.Collectors;

public class SmartCVParser {

    // 1. Liste des mots à ignorer (Stop Words) - J'ai ajouté 'driven', 'morocco', 'summary'
    private static final Set<String> STOP_WORDS = Set.of(
            "le", "la", "les", "de", "des", "du", "un", "une", "et", "ou", "à", "en", "pour", "par",
            "je", "tu", "il", "nous", "vous", "ils", "mon", "ma", "mes", "sur", "dans",
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "with", "by", "for",
            "is", "are", "was", "were", "be", "been", "have", "has", "had",
            "email", "tel", "phone", "address", "adresse", "contact", "curriculum", "vitae", "resume",
            "page", "né", "date", "naissance", "nationality", "nationalité", "competences", "skills",
            "education", "formation", "experience", "professionnelle", "langues", "languages",
            "soft", "hard", "loisirs", "interets", "ref", "reference",
            "morocco", "maroc", "kenitra", "casablanca", "rabat", "driven", "passionate", "motivated"
    );

    // 2. Dictionnaire de mots "GOLD" (Donne un gros bonus de score)
    private static final Set<String> TECH_KEYWORDS = Set.of(
            "java", "python", "c++", "c#", "php", "javascript", "typescript", "kotlin", "swift",
            "spring", "laravel", "symfony", "django", "flask", "react", "angular", "vue", "flutter",
            "sql", "mysql", "postgresql", "oracle", "mongodb", "docker", "kubernetes", "aws", "azure",
            "developer", "développeur", "engineer", "ingénieur", "fullstack", "backend", "frontend",
            "data", "scientist", "analyst", "devops", "mobile", "web", "software", "informatique"
    );

    public String extractSearchQuery(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "java";

        // Nettoyage
        String cleanText = rawText.toLowerCase()
                .replaceAll("[^a-zA-Z0-9éèàêâûîô+]", " "); // On garde le '+' pour C++

        String[] words = cleanText.split("\\s+");
        Map<String, Integer> wordScores = new HashMap<>();

        for (String word : words) {
            if (word.length() > 2 && !STOP_WORDS.contains(word)) {

                int score = 1; // Score de base

                // Si c'est un mot technique, on multiplie son score par 10 !
                if (TECH_KEYWORDS.contains(word)) {
                    score = 10;
                }

                wordScores.put(word, wordScores.getOrDefault(word, 0) + score);
            }
        }

        // On prend les 3 meilleurs scores
        return wordScores.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Tri décroissant
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(" "));
    }
}