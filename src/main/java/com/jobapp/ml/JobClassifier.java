package com.jobapp.ml;

public class JobClassifier {
    
    public String predictCategory(String text) {
        String lower = text.toLowerCase();

        // Rule-based Classification Model
        if (lower.contains("java") || lower.contains("développeur") || lower.contains("data") || lower.contains("informatique") || lower.contains("full stack")) {
            return "IT / Informatique";
        }
        else if (lower.contains("manager") || lower.contains("directeur") || lower.contains("chef de projet") || lower.contains("responsable")) {
            return "Management";
        }
        else if (lower.contains("comptable") || lower.contains("finance") || lower.contains("auditeur")) {
            return "Finance";
        }
        else if (lower.contains("rh") || lower.contains("recrutement")) {
            return "Ressources Humaines";
        }
        
        return "Autre"; // Default if no keywords match
    }
}