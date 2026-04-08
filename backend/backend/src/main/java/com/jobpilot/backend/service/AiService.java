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
        String prompt = "You are writing a job application email on behalf of a candidate. Read the RESUME carefully and write the email STRICTLY following this format and rules.\n\n" +
                "STRICT FORMAT (4 paragraphs, in this exact order):\n\n" +
                "Hi [RecruiterFirstName],\n\n" +
                "[Paragraph 1] Start with: 'I came across your requirement for an [Exact Job Title] and it aligns closely with my experience.' Then state the candidate's years of experience and 2-3 core technologies from the resume that match the JD.\n\n" +
                "[Paragraph 2] Mention 3-4 specific technical skills/responsibilities from the JD that the candidate has done in recent roles. Include domain experience (banking, finance, healthcare, government, etc.) ONLY if both the JD and resume mention it. Mention databases, cloud platforms, and tools the candidate actually has.\n\n" +
                "[Paragraph 3] If the JD says 'local', 'onsite', 'hybrid', or names a specific city/state, write: 'I am local to the [City] area and available for in-person interviews, and open to working [X] days onsite as required.' Otherwise skip this paragraph.\n\n" +
                "[Paragraph 4] Always end with exactly: 'Please find my resume attached for your review. I look forward to discussing this opportunity further.'\n\n" +
                "STRICT RULES:\n" +
                "- Extract recruiter first name from email address (e.g., amit.gupta@... → Amit)\n" +
                "- Use the EXACT job title from the JD, do not paraphrase\n" +
                "- Do NOT mention skills that are not in the resume\n" +
                "- Do NOT include subject line\n" +
                "- Do NOT include signature (no 'Thanks', 'Regards', name, email)\n" +
                "- Do NOT use markdown bolding (**) — write plain text only\n" +
                "- Do NOT add any extra commentary or notes\n" +
                "- Keep each paragraph 2-4 sentences\n" +
                "- Write ONLY the email body starting with 'Hi [Name],'\n\n" +
                "RECRUITER EMAIL: " + recruiterEmail + "\n\n" +
                "JOB DESCRIPTION:\n" + jobDescription + "\n\n" +
                "RESUME:\n" + resumeText + "\n\n" +
                "Write the email body now (start with 'Hi [Name],'):";

        return callAiApi(prompt, apiKey, aiProvider);
    }

    public String generateEmailSubject(String jobTitle, String company,
                                        String apiKey, String aiProvider) {
        String prompt = "Generate an email subject line for a job application following this EXACT format:\n\n" +
                "FORMAT: [Prefix] – [Exact Job Title] – [Location]\n\n" +
                "RULES:\n" +
                "- If the job title or description mentions 'local', 'onsite', or 'need locals', start with 'Local Candidate – '\n" +
                "- Otherwise, no prefix\n" +
                "- Use the EXACT job title — do not paraphrase or shorten\n" +
                "- Extract location (City, State) from the job title if present, otherwise use 'Remote'\n" +
                "- Use en-dash ( – ) as separator, not hyphen\n" +
                "- NEVER use placeholders like [Your Location] or [City]\n" +
                "- Respond with ONLY the subject line, nothing else\n\n" +
                "EXAMPLES:\n" +
                "- Local Candidate – ASP.NET Developer with OpenAI – New York City, NY\n" +
                "- Senior Java Developer – Remote\n" +
                "- Local Candidate – Network Engineer – Chicago, IL\n\n" +
                "Job Title: " + jobTitle + "\n" +
                "Company: " + company + "\n\n" +
                "Subject line:";

        return callAiApi(prompt, apiKey, aiProvider);
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
