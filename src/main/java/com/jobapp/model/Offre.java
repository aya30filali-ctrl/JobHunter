package com.jobapp.model;

public class Offre {
    private String titre;
    private String description;
    private String ville;
    private String entreprise;
    private String url;
    private String category; // <--- NEW FIELD

    public Offre(String titre, String description, String ville, String entreprise, String url, String category) {
        this.titre = titre;
        this.description = description;
        this.ville = ville;
        this.entreprise = entreprise;
        this.url = url;
        this.category = category;
    }

    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getVille() { return ville; }
    public String getEntreprise() { return entreprise; }
    public String getUrl() { return url; }
    public String getCategory() { return category; } // <--- NEW GETTER

    public String getFullText() {
        return (titre + " " + description + " " + category).toLowerCase();
    }
}