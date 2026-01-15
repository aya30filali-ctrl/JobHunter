package com.jobapp.service;

import java.util.*;

/**
 * SectorWeighter - Le Juge Sectoriel.
 * Son rôle : Identifier le domaine d'activité dominant d'un texte.
 * Utilisé pour bannir les offres hors-sujet (Ex: Chauffeur pour un Ingénieur).
 */
public class SectorWeighter {

    private static final Map<String, Set<String>> SECTORS = new HashMap<>();

    static {
        // 1. SECTEUR IT / TECH / DATA
        SECTORS.put("TECH", new HashSet<>(Arrays.asList(
                "java", "python", "c++", "c#", "php", "javascript", "typescript", "html", "css",
                "sql", "mysql", "oracle", "mongodb", "postgresql", "big", "data", "machine", "learning",
                "ai", "artificial", "intelligence", "flutter", "react", "angular", "spring", "laravel",
                "docker", "kubernetes", "linux", "git", "github", "devops", "cloud", "aws", "azure",
                "scikit", "pandas", "numpy", "tensorflow", "keras", "pytorch", "algorithm", "network",
                "security", "fullstack", "backend", "frontend", "mobile", "web", "developer", "engineer",
                "informatique", "numerique", "digital", "systeme", "reseaux", "analyse", "donnees"
        )));

        // 2. SECTEUR VENTE / COMMERCE
        SECTORS.put("SALES", new HashSet<>(Arrays.asList(
                "commercial", "vente", "vendeur", "vendeuse", "sales", "client", "prospection", "negoce",
                "negociation", "magasin", "boutique", "caissier", "rayon", "merchandiser", "retail",
                "achat", "buyer", "crm", "fidelisation", "chiffre", "affaires", "business", "manager",
                "marketing", "call", "center", "teleconseiller"
        )));

        // 3. SECTEUR OPÉRATIONS / MANUEL
        SECTORS.put("OPS", new HashSet<>(Arrays.asList(
                "chauffeur", "conducteur", "transport", "logistique", "magasinier", "entrepot", "stock",
                "usine", "production", "ouvrier", "technicien", "maintenance", "mecanique", "electricien",
                "btp", "chantier", "agent", "securite", "nettoyage", "gardien", "livreur", "coursier",
                "permis", "conduite", "vehicule"
        )));

        // 4. SECTEUR FINANCE / ADMIN
        SECTORS.put("FINANCE", new HashSet<>(Arrays.asList(
                "comptabilite", "comptable", "finance", "financier", "audit", "tresorerie", "banque",
                "assurance", "juridique", "rh", "ressources", "humaines", "payroll", "paie", "gestion",
                "assistant", "administratif", "secretaire", "accueil", "facturation", "office"
        )));
    }

    /**
     * Détermine le secteur dominant d'un texte.
     */
    public String getDominantSector(String text) {
        if (text == null || text.isEmpty()) return "UNKNOWN";
        String lower = text.toLowerCase();

        Map<String, Integer> scores = new HashMap<>();
        scores.put("TECH", 0);
        scores.put("SALES", 0);
        scores.put("OPS", 0);
        scores.put("FINANCE", 0);

        // Analyse mot par mot
        // On remplace la ponctuation par des espaces pour isoler les mots

        String cleaned = lower.replaceAll("[^a-z0-9]", " ");
        String[] words = cleaned.split("\\s+");

        for (String w : words) {
            if (w.length() < 3) continue;

            if (SECTORS.get("TECH").contains(w)) scores.put("TECH", scores.get("TECH") + 1);
            if (SECTORS.get("SALES").contains(w)) scores.put("SALES", scores.get("SALES") + 1);
            if (SECTORS.get("OPS").contains(w)) scores.put("OPS", scores.get("OPS") + 1);
            if (SECTORS.get("FINANCE").contains(w)) scores.put("FINANCE", scores.get("FINANCE") + 1);
        }

        // Trouver le gagnant

        String winner = "UNKNOWN";
        int maxScore = 0;

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                winner = entry.getKey();
            }
        }

        // Il faut un minimum de confiance (au moins 1 mot clé fort trouvé)

        return (maxScore >= 1) ? winner : "UNKNOWN";
    }
}