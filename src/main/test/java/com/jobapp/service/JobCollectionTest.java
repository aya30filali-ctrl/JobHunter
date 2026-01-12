package com.jobapp.service;

import com.jobapp.model.Offre;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class JobCollectionTest {
    @Test
    public void testCollectionHandling() {
        List<Offre> list = new ArrayList<>();
        list.add(new Offre("Java Dev", "OCP", "Safi", "Desc", "http://url.com", "IT"));

        // Vérification de la taille et du contenu (Critère Collection )
        assertFalse(list.isEmpty(), "La collection d'offres ne doit pas être vide.");
        assertEquals(1, list.size());
        assertEquals("OCP", list.get(0).getEntreprise());
    }
}