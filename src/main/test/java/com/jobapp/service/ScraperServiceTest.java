package com.jobapp.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ScraperServiceTest {
    @Test
    public void testServiceInitialization() {
        ScraperService service = new ScraperService();
        assertNotNull(service, "Le ScraperService doit pouvoir être instancié.");
    }
}