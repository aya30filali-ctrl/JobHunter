package com.jobapp.ml;

public class CVParser {
    
    /**
     * Simulates scanning a raw CV and summarizing it into a clean paragraph.
     */
    public String parseAndRegroup(String rawCvText) {
        if (rawCvText == null) return "";

        // 1. Cleaning: Remove special characters and extra newlines
        String cleaned = rawCvText.replaceAll("[^a-zA-Z0-9éèàê\\s@.]", " ");
        
        // 2. Regrouping: Merge into a single clean paragraph
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        System.out.println(">>> CV Model: Scanned and regrouped CV text.");
        return cleaned;
    }
}