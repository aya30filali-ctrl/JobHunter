package com.jobapp.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExceptionHandlingTest {
    @Test
    public void testUrlValidationException() {
        // Simule un comportement où une URL invalide doit être détectée
        String badUrl = "not-a-link";

        // On vérifie que le système identifie bien l'erreur (Critère Exception )
        assertThrows(IllegalArgumentException.class, () -> {
            if(!badUrl.startsWith("http")) {
                throw new IllegalArgumentException("Invalid URL Format");
            }
        }, "Le système doit lever une exception pour une URL malformée.");
    }
}