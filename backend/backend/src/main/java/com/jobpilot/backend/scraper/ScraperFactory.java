package com.jobpilot.backend.scraper;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScraperFactory {

    private final Map<String, JobScraper> scraperMap = new HashMap<>();

    public ScraperFactory(List<JobScraper> scrapers) {
        for (JobScraper scraper : scrapers) {
            scraperMap.put(scraper.getPortalName().toLowerCase(), scraper);
        }
    }

    public JobScraper getScraper(String portalName) {
        JobScraper scraper = scraperMap.get(portalName.toLowerCase());
        if (scraper == null) {
            throw new IllegalArgumentException("No scraper registered for portal: " + portalName);
        }
        return scraper;
    }

    public Map<String, JobScraper> getAllScrapers() {
        return scraperMap;
    }

    public boolean hasScraperFor(String portalName) {
        return scraperMap.containsKey(portalName.toLowerCase());
    }
}
