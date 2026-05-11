package com.jobpilot.backend.service;

import com.jobpilot.backend.model.Resume;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Resume selectBestResume(List<Resume> resumes, String jobDescription,
                                    String apiKey, String aiProvider) {
        if (resumes == null || resumes.isEmpty()) {
            throw new RuntimeException("No resumes available");
        }
        if (resumes.size() == 1) {
            return resumes.get(0);
        }

        // Detect frontend and cloud stack from JD
        String jdLower = jobDescription.toLowerCase();

        boolean mentionsAngular = jdLower.contains("angular");
        boolean mentionsReact = jdLower.contains("react");
        boolean mentionsVue = jdLower.contains("vue");
        boolean hasFrontend = mentionsAngular || mentionsReact || mentionsVue;

        boolean mentionsAzure = jdLower.contains("azure");
        boolean mentionsAws = jdLower.contains(" aws") || jdLower.contains("amazon web services");
        boolean mentionsGcp = jdLower.contains("gcp") || jdLower.contains("google cloud");
        boolean hasCloud = mentionsAzure || mentionsAws || mentionsGcp;

        System.out.println("Resume selection: frontend=" + hasFrontend +
                           " (Angular=" + mentionsAngular + ", React=" + mentionsReact + ")" +
                           " cloud=" + hasCloud +
                           " (Azure=" + mentionsAzure + ", AWS=" + mentionsAws + ")");

        // If JD doesn't mention any frontend OR cloud, use first resume (user's preferred default)
        if (!hasFrontend && !hasCloud) {
            System.out.println("JD has no frontend/cloud specifics — using first resume: " + resumes.get(0).getResumeLabel());
            return resumes.get(0);
        }

        // Score each resume by matching label + extracted text
        Resume bestMatch = null;
        int bestScore = -1;

        for (Resume resume : resumes) {
            String label = resume.getResumeLabel() != null ? resume.getResumeLabel().toLowerCase() : "";
            String text = resume.getExtractedText() != null ? resume.getExtractedText().toLowerCase() : "";
            String combined = label + " " + text;

            int score = 0;

            // Frontend match (weighted heavily on label)
            if (mentionsAngular && label.contains("angular")) score += 10;
            if (mentionsReact && label.contains("react")) score += 10;
            if (mentionsVue && label.contains("vue")) score += 10;
            if (mentionsAngular && combined.contains("angular")) score += 3;
            if (mentionsReact && combined.contains("react")) score += 3;

            // Cloud match (weighted heavily on label)
            if (mentionsAzure && label.contains("azure")) score += 10;
            if (mentionsAws && label.contains("aws")) score += 10;
            if (mentionsGcp && label.contains("gcp")) score += 10;
            if (mentionsAzure && combined.contains("azure")) score += 3;
            if (mentionsAws && combined.contains("aws")) score += 3;

            System.out.println("  Resume '" + resume.getResumeLabel() + "' scored: " + score);

            if (score > bestScore) {
                bestScore = score;
                bestMatch = resume;
            }
        }

        // If no resume scored anything, fall back to first
        if (bestMatch == null || bestScore == 0) {
            System.out.println("No resume matched — using first resume: " + resumes.get(0).getResumeLabel());
            return resumes.get(0);
        }

        System.out.println("Selected resume: " + bestMatch.getResumeLabel() + " (score: " + bestScore + ")");
        return bestMatch;
    }

    public String generateEmailBody(String jobDescription, String resumeText,
                                     String recruiterEmail, String apiKey, String aiProvider) {
        String prompt = "You are writing a SHORT, professional job application email on behalf of a candidate. Read the RESUME carefully and write the email STRICTLY following this format.\n\n" +
                "STRICT FORMAT (keep it concise — 3 short paragraphs only):\n\n" +
                "Hi [RecruiterFirstName],\n\n" +
                "[Paragraph 1 — 2 sentences max] Start with: 'I came across your requirement for an [Exact Job Title] and it aligns closely with my experience.' Then mention years of experience and 2-3 core technologies from the resume that match the JD.\n\n" +
                "[Paragraph 2 — 2-3 sentences max] Mention 3-4 specific technical skills from the JD that the candidate has. Keep it tight — no fluff, no generic statements.\n\n" +
                "[Paragraph 3 — exactly this sentence] 'Please find my resume attached for your review. I look forward to discussing this opportunity further.'\n\n" +
                "STRICT RULES:\n" +
                "- Extract recruiter first name from email (e.g., amit.gupta@... → Amit)\n" +
                "- Use the EXACT job title from the JD\n" +
                "- Keep the email SHORT — recruiters skim emails\n" +
                "- Do NOT add a paragraph about being local or available for interviews\n" +
                "- Do NOT say 'I am local to X area' or 'available for onsite interviews' anywhere\n" +
                "- Do NOT mention location at all in the body\n" +
                "- Do NOT mention skills not in the resume\n" +
                "- Do NOT include subject line\n" +
                "- Do NOT include signature (no Thanks/Regards/name)\n" +
                "- Do NOT use markdown (**) — plain text only\n" +
                "- Write ONLY the email body starting with 'Hi [Name],'\n\n" +
                "RECRUITER EMAIL: " + recruiterEmail + "\n\n" +
                "JOB DESCRIPTION:\n" + jobDescription + "\n\n" +
                "RESUME:\n" + resumeText + "\n\n" +
                "Write the SHORT email body now:";

        return callAiApi(prompt, apiKey, aiProvider).trim();
    }
    public String extractJobTitleFromJD(String jobDescription, String apiKey, String aiProvider) {
        String prompt = "Extract ONLY the exact job title from the following job description. " +
                "Return just the title, nothing else — no explanations, no quotes, no labels, no 'Job Title:' prefix. " +
                "If multiple titles appear, return the primary/main one. " +
                "If no clear title exists, return 'Position'.\n\n" +
                "EXAMPLES:\n" +
                "JD: 'We are hiring a Senior Software Engineer (Backend) at Acme Corp...'\n" +
                "Output: Senior Software Engineer (Backend)\n\n" +
                "JD: 'Urgent requirement: .Net Developer - Boston MA - Onsite'\n" +
                "Output: .Net Developer\n\n" +
                "JD: 'Looking for React Developer with 5+ years experience'\n" +
                "Output: React Developer\n\n" +
                "Now extract the title from this JD:\n\n" + jobDescription + "\n\nTitle:";

        return callAiApi(prompt, apiKey, aiProvider).trim();
    }

    public String extractLocationFromJD(String jobDescription, String apiKey, String aiProvider) {
        String prompt = "Extract the PRIMARY work location from the job description. " +
                "Look for city names, state names/abbreviations, and phrases like 'local to X', 'onsite in X', 'based in X', 'must be in X'. " +
                "Return in format 'City, ST' if city is clearly stated, or just 'ST' if only state is mentioned. " +
                "Return 'Remote' ONLY if the JD explicitly says 'fully remote', '100% remote', or 'remote work from anywhere' AND does NOT mention any specific city/state requirement. " +
                "If the JD mentions local/onsite/hybrid with a location, return that location, NOT 'Remote'. " +
                "No explanations, no quotes, no prefix — just the location.\n\n" +
                "EXAMPLES:\n" +
                "JD: 'Senior Engineer - local to California' → CA\n" +
                "JD: '.Net Developer onsite in Boston, MA' → Boston, MA\n" +
                "JD: 'Remote role, work from anywhere in US' → Remote\n" +
                "JD: '.Net Developer - Remote (must be local to TX)' → TX\n" +
                "JD: 'Hybrid role in Plano Texas, 3 days onsite' → Plano, TX\n" +
                "JD: 'Full stack developer, must reside in NJ or NY' → NJ\n\n" +
                "Now extract location from:\n\n" + jobDescription + "\n\nLocation:";

        return callAiApi(prompt, apiKey, aiProvider).trim();
    }

    public String generateEmailSubject(String jobTitle, String company, String location,
                                        String jobDescription, String apiKey, String aiProvider) {
        String prompt = "Generate an email subject line for a job application. Use the provided fields exactly — do NOT invent or hardcode anything.\n\n" +
                "FORMAT:\n" +
                "- If the JD mentions a specific city/state (local, onsite, hybrid, based in, must reside in, etc.): '[Job Title]-[City], [State]' OR '[Job Title]-[State]'\n" +
                "- If the role is remote: '[Job Title]-Remote'\n" +
                "- Use the exact job title provided\n" +
                "- Use hyphens as separators (not en-dash)\n" +
                "- Do NOT include 'Local Candidate' or any prefix\n" +
                "- Do NOT use quotes, brackets, or placeholders\n" +
                "- Respond with ONLY the subject line, nothing else\n\n" +
                "EXAMPLES:\n" +
                "Title: '.Net Developer', Location: 'Boston, MA', JD mentions local → .Net Developer-Boston, MA\n" +
                "Title: '.Net Full Stack Developer', Location: 'CA', JD mentions onsite → .Net Full Stack Developer-CA\n" +
                "Title: 'Senior Java Developer', Location: 'Remote', JD says fully remote → Senior Java Developer-Remote\n" +
                "Title: 'React Developer', Location: 'Plano, TX', JD says hybrid → React Developer-Plano, TX\n\n" +
                "Now generate the subject line:\n" +
                "Job Title: " + jobTitle + "\n" +
                "Company: " + company + "\n" +
                "Location: " + location + "\n" +
                "Job Description:\n" + jobDescription + "\n\n" +
                "Subject line:";

        return callAiApi(prompt, apiKey, aiProvider).trim();
    }
    private String callAiApi(String prompt, String apiKey, String aiProvider) {
        if ("claude".equalsIgnoreCase(aiProvider)) {
            return callClaude(prompt, apiKey);
        } else if ("openai".equalsIgnoreCase(aiProvider)) {
            return callOpenAi(prompt, apiKey);
        } else {
            throw new RuntimeException("Unsupported AI provider: " + aiProvider);
        }
    }

    private String callClaude(String prompt, String apiKey) {
        String url = "https://api.anthropic.com/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-sonnet-4-20250514");
        body.put("max_tokens", 1024);
        body.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.get("content").get(0).get("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Claude response: " + e.getMessage(), e);
        }
    }

    private String callOpenAi(String prompt, String apiKey) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("messages", List.of(message));
        body.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    private int parseResumeSelection(String response, int maxResumes) {
        String cleaned = response.replaceAll("[^0-9]", "");
        try {
            int selection = Integer.parseInt(cleaned.substring(0, 1));
            if (selection >= 1 && selection <= maxResumes) {
                return selection - 1;
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
