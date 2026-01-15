package com.jobapp.scraping;

import com.jobapp.ml.JobClassifier;
import com.jobapp.model.Offre;
import java.util.List;


public abstract class AbstractJobScraper implements JobScraper {
    protected JobClassifier classifier = new JobClassifier();

    @Override
    public abstract List<Offre> scrape(String keyword);
}