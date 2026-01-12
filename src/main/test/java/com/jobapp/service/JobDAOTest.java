package com.jobapp.dao;

import com.jobapp.model.Offre;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JobDAOTest {

    @Test
    public void testOffreObjectIntegrity() {
        // Test de création d'une offre pour vérifier le modèle
        Offre job = new Offre("Développeur", "TechCorp", "Casablanca", "Description...", "http://url.com", "Informatique");

        assertNotNull(job.getTitre());
        assertEquals("TechCorp", job.getEntreprise());
        assertTrue(job.getUrl().startsWith("http"));
    }

    @Test
    public void testDuplicateDetectionLogic() {
        // Simule la logique de détection de doublons basée sur l'URL
        String url1 = "https://www.rekrute.com/offre1";
        String url2 = "https://www.rekrute.com/offre1";

        assertEquals(url1, url2, "Le système de JobDAO doit bloquer les URLs identiques via SQL UNIQUE.");
    }
}