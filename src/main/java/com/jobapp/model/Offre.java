package com.jobapp.model;

public class Offre {
    private int id;
    private String titre;
    private String entreprise;
    private String ville;
    private String description;
    private String url;
    private String datePublication;
    private String category; // Ex: "Informatique", "Commercial"

    public Offre() {}

    public Offre(String titre, String entreprise, String ville, String description, String url, String category) {
        this.titre = titre;
        this.entreprise = entreprise;
        this.ville = ville;
        this.description = description;
        this.url = url;
        this.category = category;
    }

    // GETTERS & SETTERS

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getEntreprise() { return entreprise; }
    public void setEntreprise(String entreprise) { this.entreprise = entreprise; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDatePublication() { return datePublication; }
    public void setDatePublication(String datePublication) { this.datePublication = datePublication; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    /*
     Combine toutes les infos pour que l'IA ait de la matière à analyser.
     */
    public String getFullText() {
        //  On concatène une chaîne vide "" au début pour éviter le NullPointerException
        return "" + titre + " " + description + " " + entreprise + " " + category + " " + ville;
    }
}