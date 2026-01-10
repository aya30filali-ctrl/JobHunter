package com.jobapp.ml;

public class JobClassifier {

    public String predictCategory(String text) {
        if (text == null) return "Autre";
        String lower = text.toLowerCase();

        // 1. IT / Informatique (Expanded)
        if (matches(lower, "java", "spring", "sql", "data", "développeur", "developer", "full stack",
                "informatique", "système", "réseau", "cloud", "devops", "engineer", "ingénieur", "technician", "support it")) {
            return "IT / Informatique";
        }

        // 2. Finance / Compta
        if (matches(lower, "finance", "comptable", "audit", "trésorerie", "contrôleur de gestion", "economie")) {
            return "Finance & Compta";
        }

        // 3. Commerce / Vente (New)
        if (matches(lower, "commercial", "vente", "sales", "account manager", "business developer", "vendeur", "export")) {
            return "Commerce / Vente";
        }

        // 4. Marketing / Communication
        if (matches(lower, "marketing", "communication", "digital", "brand", "média", "community manager")) {
            return "Marketing / Com";
        }

        // 5. Management (Stricter - to avoid grabbing "Engineer" as Manager)
        if (matches(lower, "directeur", "manager", "chef de projet", "responsable", "lead", "head of", "coordinateur")) {
            return "Management";
        }

        // 6. RH
        if (matches(lower, "rh", "ressources humaines", "recrutement", "talent", "paie")) {
            return "Ressources Humaines";
        }

        return "Autre"; // Default
    }

    // Helper method to check multiple keywords
    private boolean matches(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }
}