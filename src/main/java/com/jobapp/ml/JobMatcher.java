package com.jobapp.ml;

import java.util.*;

public class JobMatcher {

    public double calculateMatchScore(String cvText, String jobDescription) {
        if (cvText == null || jobDescription == null) return 0.0;

        Map<String, Integer> cvVector = getWordFrequency(cvText);
        Map<String, Integer> jobVector = getWordFrequency(jobDescription);

        return cosineSimilarity(cvVector, jobVector) * 100;
    }

    private Map<String, Integer> getWordFrequency(String text) {
        Map<String, Integer> frequencyMap = new HashMap<>();
        String[] words = text.toLowerCase().split("\\W+");
        for (String word : words) {
            if (word.length() > 2) {
                frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
            }
        }
        return frequencyMap;
    }

    private double cosineSimilarity(Map<String, Integer> vec1, Map<String, Integer> vec2) {
        Set<String> allWords = new HashSet<>(vec1.keySet());
        allWords.addAll(vec2.keySet());

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String word : allWords) {
            int val1 = vec1.getOrDefault(word, 0);
            int val2 = vec2.getOrDefault(word, 0);
            dotProduct += val1 * val2;
            normA += Math.pow(val1, 2);
            normB += Math.pow(val2, 2);
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}