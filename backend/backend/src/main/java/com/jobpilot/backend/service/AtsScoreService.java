package com.jobpilot.backend.service;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AtsScoreService {

    public double calculateScore(String jobDescription, String resumeText) {
        if (jobDescription == null || resumeText == null) return 0.0;
        Set<String> jdKeywords = extractKeywords(jobDescription);
        Set<String> resumeKeywords = extractKeywords(resumeText);
        if (jdKeywords.isEmpty()) return 0.0;
        long matched = jdKeywords.stream().filter(resumeKeywords::contains).count();
        return ((double) matched / jdKeywords.size()) * 100.0;
    }

    private Set<String> extractKeywords(String text) {
        Set<String> stopWords = Set.of("the","and","for","with","this","that",
            "have","will","are","was","you","not","but","they","from","been",
            "has","its","your","our","can","may","should","would","also","into",
            "more","about","than","which","when","their","there","what","all","any","one","new");
        return Arrays.stream(text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9+#.\\s]", " ").split("\\s+"))
                .map(String::trim).filter(w -> w.length() > 2)
                .filter(w -> !stopWords.contains(w)).collect(Collectors.toSet());
    }
}
