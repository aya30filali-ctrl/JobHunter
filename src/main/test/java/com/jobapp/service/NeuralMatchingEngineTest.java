package com.jobapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class NeuralMatchingEngineTest {

    private NeuralMatchingEngine engine;

    @BeforeEach
    public void setUp() {
        // Initialisation avant chaque test
        engine = new NeuralMatchingEngine();
    }

    @Test
    public void testPerfectMatch() {
        String cv = "Expert en Java et Spring Boot";
        String title = "Développeur Java Spring Boot";
        String desc = "Nous recherchons un expert en Java";

        int score = engine.calculateMatchScore(cv, title, desc);
        System.out.println("Test Perfect Match - Score obtenu : " + score);
        // On s'attend à un score élevé (proche de 100)
        assertTrue(score > 80, "Le score devrait être élevé pour un match parfait");
    }

    @Test
    public void testNoMatch() {
        String cv = "Boulanger pâtissier avec 10 ans d'expérience";
        String title = "Ingénieur Cloud Computing";
        String desc = "Expertise en AWS et Kubernetes requise";

        int score = engine.calculateMatchScore(cv, title, desc);

        // Le score doit être très bas ou nul
        assertEquals(0, score, "Le score devrait être nul pour des profils totalement différents");
    }
}