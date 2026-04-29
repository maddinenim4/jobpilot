package com.jobpilot.backend.scraper;

import java.util.LinkedHashMap;
import java.util.Map;

public class PortalConfig {

    private static final Map<String, String> PORTALS = new LinkedHashMap<>();

    static {
        PORTALS.put("Nvoids", "https://nvoids.com/search_sph.jsp?q={keyword}");
        PORTALS.put("Hiring42", "https://www.hiring42.com/all_jobs?q={keyword}");
        PORTALS.put("Robert Half", "https://www.roberthalf.com/us/en/jobs/all/{keyword}");
        PORTALS.put("Randstad", "https://www.randstadusa.com/jobs/s-{keyword}/");
        PORTALS.put("Insight Global", "https://jobs.insightglobal.com/jobs?keyword={keyword}");
        PORTALS.put("TEKsystems", "https://www.teksystems.com/en/it-jobs?keyword={keyword}");
        PORTALS.put("Apex Systems", "https://www.apexsystems.com/jobs?keyword={keyword}");
        PORTALS.put("Kforce", "https://www.kforce.com/find-work/search-jobs/?keyword={keyword}");
        PORTALS.put("Collabera", "https://www.collabera.com/find-jobs/?keyword={keyword}");
        PORTALS.put("Beacon Hill", "https://www.beaconhillstaffing.com/jobs?keyword={keyword}");
        PORTALS.put("Dice", "https://www.dice.com/jobs?q={keyword}");
    }

    public static Map<String, String> getAllPortals() {
        return PORTALS;
    }

    public static String getSearchUrl(String portalName, String keyword) {
        String template = PORTALS.get(portalName);
        if (template == null) {
            throw new IllegalArgumentException("Unknown portal: " + portalName);
        }
        return template.replace("{keyword}", keyword.replace(" ", "+"));
    }

    public static boolean isPortal(String name) {
        return PORTALS.containsKey(name);
    }
}
