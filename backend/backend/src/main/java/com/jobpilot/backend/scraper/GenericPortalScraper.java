package com.jobpilot.backend.scraper;

import com.jobpilot.backend.model.JobPosting;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GenericPortalScraper {

    public List<JobPosting> scrapePortal(String portalName, String keyword) {
        List<JobPosting> jobs = new ArrayList<>();
        WebDriver driver = null;

        try {
            String searchUrl = PortalConfig.getSearchUrl(portalName, keyword);
            System.out.println("GenericScraper: Scraping " + portalName + " at: " + searchUrl);

            driver = createDriver();
            driver.get(searchUrl);
            Thread.sleep(3000);

            String html = driver.getPageSource();
            Document doc = Jsoup.parse(html);

            Elements allLinks = doc.select("a[href]");
            List<String[]> jobLinks = new ArrayList<>();

            for (Element link : allLinks) {
                String text = link.text().trim();
                String href = link.absUrl("href");

                if (text.length() < 10 || text.length() > 200) continue;
                if (href.isEmpty()) continue;

                String firstKeyword = keyword.toLowerCase().split("\\s+")[0];
                if (!text.toLowerCase().contains(firstKeyword)) continue;

                if (href.contains("login") || href.contains("signup") || href.contains("register") ||
                    href.contains("about") || href.contains("contact") || href.contains("privacy") ||
                    href.contains("terms") || href.contains("cookie") || href.contains("faq")) continue;

                jobLinks.add(new String[]{text, href});
                if (jobLinks.size() >= 15) break;
            }

            System.out.println("GenericScraper: Found " + jobLinks.size() + " potential job links on " + portalName);

            for (String[] jobInfo : jobLinks) {
                String jobTitle = jobInfo[0];
                String jobUrl = jobInfo[1];

                try {
                    driver.get(jobUrl);
                    Thread.sleep(2000);

                    String detailHtml = driver.getPageSource();
                    Document detailDoc = Jsoup.parse(detailHtml);

                    String recruiterEmail = extractEmail(detailDoc);
                    if (recruiterEmail.isEmpty()) continue;

                    String description = detailDoc.select("body").text();
                    if (description.length() > 3000) {
                        description = description.substring(0, 3000);
                    }

                    String location = extractLocation(detailDoc.text());
                    String company = extractCompanyFromEmail(recruiterEmail);

                    jobs.add(JobPosting.builder()
                            .jobTitle(jobTitle)
                            .company(company)
                            .location(location)
                            .jobDescription(description)
                            .recruiterEmail(recruiterEmail)
                            .sourceSite(portalName)
                            .sourceUrl(jobUrl)
                            .postedDate("")
                            .build());

                    System.out.println("  Found: " + jobTitle + " | " + recruiterEmail);

                } catch (Exception e) {
                    System.err.println("  Error on detail: " + jobTitle + " - " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("GenericScraper failed for " + portalName + ": " + e.getMessage());
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception e) {}
            }
        }

        System.out.println("GenericScraper: " + portalName + " found " + jobs.size() + " jobs with emails");
        return jobs;
    }

    private WebDriver createDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        return new ChromeDriver(options);
    }

    private String extractEmail(Document doc) {
        Elements mailtoLinks = doc.select("a[href^=mailto:]");
        if (!mailtoLinks.isEmpty()) {
            return mailtoLinks.first().attr("href").replace("mailto:", "").trim();
        }
        return extractEmailFromText(doc.text());
    }

    private String extractEmailFromText(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                .matcher(text);
        if (matcher.find()) {
            String email = matcher.group();
            if (email.contains("noreply") || email.contains("no-reply") || email.contains("example.com")) {
                return "";
            }
            return email;
        }
        return "";
    }

    private String extractLocation(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?:Location|location|Location:)\\s*:?\\s*([A-Za-z\\s]+,\\s*[A-Z]{2})")
                .matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Remote";
    }

    private String extractCompanyFromEmail(String email) {
        try {
            String domain = email.substring(email.indexOf("@") + 1);
            String company = domain.substring(0, domain.lastIndexOf("."));
            return company.substring(0, 1).toUpperCase() + company.substring(1);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
