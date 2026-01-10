package com.jobapp.scraping;

import com.jobapp.model.Offre;
import java.util.List;

public interface JobScraper {
    List<Offre> scrape(String keyword);
}