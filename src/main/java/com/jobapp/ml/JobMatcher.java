/*package com.jobapp.ml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JobMatcher {

    // Main method to calculate similarity (0% to 100%)
    public double calculateScore(String cvText, String jobText) {
        // 1. Tokenize (Clean and split text into words)
        Map<String, Integer> cvVector = getTermFrequency(cvText);
        Map<String, Integer> jobVector = getTermFrequency(jobText);

        // 2. Get all unique words from both texts
        Set<String> allWords = new HashSet<>();
        allWords.addAll(cvVector.keySet());
        allWords.addAll(jobVector.keySet());

        // 3. Calculate Cosine Similarity
        double dotProduct = 0.0;
        double normCV = 0.0;
        double normJob = 0.0;

        for (String word : allWords) {
            int cvFreq = cvVector.getOrDefault(word, 0);
            int jobFreq = jobVector.getOrDefault(word, 0);

            dotProduct += cvFreq * jobFreq;
            normCV += Math.pow(cvFreq, 2);
            normJob += Math.pow(jobFreq, 2);
        }

        double magnitude = (Math.sqrt(normCV) * Math.sqrt(normJob));

        if (magnitude == 0.0) return 0.0; // Avoid division by zero

        double cosine = dotProduct / magnitude;

        // Return percentage (e.g., 0.85 -> 85.0)
        return cosine * 100;
    }

    // Helper: Turns text into a list of word counts
    private Map<String, Integer> getTermFrequency(String text) {
        Map<String, Integer> vector = new HashMap<>();
        if (text == null) return vector;

        // Clean text: lowercase, remove weird chars, split by spaces
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9à-ÿ ]", "").split("\\s+");

        for (String word : words) {
            if (word.length() > 2) { // Ignore tiny words like "le", "de", "a"
                vector.put(word, vector.getOrDefault(word, 0) + 1);
            }
        }
        return vector;
    }
}*/