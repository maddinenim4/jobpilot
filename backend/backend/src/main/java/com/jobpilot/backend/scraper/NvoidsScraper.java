package com.jobpilot.backend.scraper;

import com.jobpilot.backend.model.JobPosting;
import com.jobpilot.backend.service.ResumeSelectorService;
import com.jobpilot.backend.model.Resume;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Optional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class NvoidsScraper implements JobScraper {

    private static final String SEARCH_URL = "https://nvoids.com/search_sph.jsp";

    @Autowired
    private ResumeSelectorService resumeSelectorService;

    @Override
    public String getPortalName() {
        return "Nvoids";
    }

    @Override
    public List<JobPosting> scrapeJobs(String keyword, String location) {
        List<JobPosting> jobs = new ArrayList<>();
        WebDriver driver = null;

        try {
            driver = createDriver();
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String searchUrl = SEARCH_URL + "?q=" + encodedKeyword;

            System.out.println("NvoidsScraper: Loading search page");
            driver.get("https://nvoids.com/search_sph.jsp");
            Thread.sleep(3000);

            // Find search input, type keyword, submit form
            try {
                org.openqa.selenium.WebElement searchInput = driver.findElement(
                    org.openqa.selenium.By.cssSelector("input[type='text']")
                );
                searchInput.clear();
                searchInput.sendKeys(keyword);
                Thread.sleep(500);

                // Find submit button and click
                org.openqa.selenium.WebElement submitBtn = driver.findElement(
                    org.openqa.selenium.By.cssSelector("input[type='submit'], button[type='submit']")
                );
                submitBtn.click();
                Thread.sleep(4000);
                System.out.println("NvoidsScraper: Form submitted with keyword: " + keyword);
            } catch (Exception e) {
                System.err.println("NvoidsScraper: Form submission failed: " + e.getMessage());
            }

            String searchHtml = driver.getPageSource();
            Document doc = Jsoup.parse(searchHtml);
            Elements rows = doc.select("table tr");

            System.out.println("NvoidsScraper: Found " + rows.size() + " table rows");

            List<String[]> jobLinks = new ArrayList<>();
            int rowsChecked = 0;
            int rowsWithLinks = 0;
            int rowsRejectedByKeyword = 0;
            for (Element row : rows) {
                rowsChecked++;
                if (jobLinks.size() >= 100) break; // collect up to 20 matching jobs

                Elements cells = row.select("td");
                if (cells.size() < 2) continue;

                Element link = cells.get(0).selectFirst("a[href]");
                if (link == null) continue;
                rowsWithLinks++;

                String jobTitle = link.text().trim();
                if (jobTitle.isEmpty()) continue;

                // Skip hotlist rows
                if (jobTitle.toLowerCase().contains("hotlist")) {
                    continue;
                }

                // Nvoids search is unreliable — re-filter by keyword on the title
                String[] keywordWords = keyword.toLowerCase().trim().split("\\s+");
                String titleLower = jobTitle.toLowerCase();
                boolean allMatch = true;
                for (String word : keywordWords) {
                    if (word.length() < 2) continue;
                    if (!titleLower.contains(word)) {
                        allMatch = false;
                        break;
                    }
                }
                if (!allMatch) {
                    rowsRejectedByKeyword++;
                    System.out.println("  ROW " + rowsRejectedByKeyword + ": " + jobTitle);
                    continue;
                }

                String href = link.attr("href");
                String jobUrl;
                if (href.startsWith("http")) {
                    jobUrl = href;
                } else if (href.startsWith("/")) {
                    jobUrl = "https://nvoids.com" + href;
                } else {
                    jobUrl = "https://nvoids.com/" + href;
                }

                String jobLocation = cells.size() >= 2 ? cells.get(1).text().trim() : "";
                String postedDate = cells.size() >= 3 ? cells.get(2).text().trim() : "";

                jobLinks.add(new String[]{jobTitle, jobUrl, jobLocation, postedDate});
            }

            System.out.println("NvoidsScraper DEBUG: rowsChecked=" + rowsChecked + ", rowsWithLinks=" + rowsWithLinks + ", rowsRejectedByKeyword=" + rowsRejectedByKeyword);
            System.out.println("NvoidsScraper: Collected " + jobLinks.size() + " job links, fetching details...");

            for (String[] jobInfo : jobLinks) {
                String jobTitle = jobInfo[0];
                String jobUrl = jobInfo[1];
                String jobLocation = jobInfo[2];
                String postedDate = jobInfo[3];

                try {
                    driver.get(jobUrl);
                    Thread.sleep(1500);

                    String detailHtml = driver.getPageSource();
                    Document detailDoc = Jsoup.parse(detailHtml);

                    String description = detailDoc.select("body").text();
                    if (description.length() > 3000) {
                        description = description.substring(0, 3000);
                    }

                    String recruiterEmail = extractRecruiterEmail(description, "me@nvoids.com");
                    System.out.println("Sending application to: " + recruiterEmail);

                    Optional<Resume> bestResume = resumeSelectorService
                        .selectBestResume(description);

                    if (bestResume.isEmpty()) {
                        System.out.println("No resume found, skipping: " + jobTitle);
                        continue;
                    }

                    String resumePath = bestResume.get().getFilePath();
                    System.out.println("Using resume: " + bestResume.get().getFileName());

                    String company = extractCompanyFromEmail(recruiterEmail);

                    jobs.add(JobPosting.builder()
                            .jobTitle(jobTitle)
                            .company(company)
                            .location(jobLocation)
                            .jobDescription(description)
                            .recruiterEmail(recruiterEmail)
                            .sourceSite("Nvoids")
                            .sourceUrl(jobUrl)
                            .postedDate(postedDate)
                            .build());

                    System.out.println("  Found job: " + jobTitle + " | " + recruiterEmail);

                } catch (Exception e) {
                    System.err.println("  Error on detail: " + jobTitle + " - " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("NvoidsScraper failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    System.err.println("Error closing browser: " + e.getMessage());
                }
            }
        }

        System.out.println("NvoidsScraper found " + jobs.size() + " jobs with emails");
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
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", java.util.List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        ChromeDriver driver = new ChromeDriver(options);
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
            "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        );
        return driver;
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

    private String extractRecruiterEmail(String jobDescription, String defaultEmail) {
        if (jobDescription == null) return defaultEmail;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
        );
        java.util.regex.Matcher matcher = pattern.matcher(jobDescription);

        while (matcher.find()) {
            String email = matcher.group();
            // Skip nvoids default emails
            if (!email.contains("nvoids.com") && !email.contains("noreply")) {
                System.out.println("Found recruiter email: " + email);
                return email;
            }
        }

        System.out.println("No recruiter email found, using default: " + defaultEmail);
        return defaultEmail;
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
