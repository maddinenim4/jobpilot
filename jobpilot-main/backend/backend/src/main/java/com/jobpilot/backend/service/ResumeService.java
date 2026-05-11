package com.jobpilot.backend.service;

import com.jobpilot.backend.model.Resume;
import com.jobpilot.backend.repository.ResumeRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
public class ResumeService {

    @Autowired
    private ResumeRepository resumeRepository;

    private static final String UPLOAD_DIR = "/home/manoharmaddineni1/resumes/";

    public void saveResume(MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));

        String savedPath = UPLOAD_DIR + file.getOriginalFilename();
        Files.copy(file.getInputStream(),
            Paths.get(savedPath),
            StandardCopyOption.REPLACE_EXISTING);

        String text = extractText(file);

        Resume resume = new Resume();
        resume.setFileName(file.getOriginalFilename());
        resume.setFilePath(savedPath);
        resume.setParsedText(text);
        resume.setActive(true);

        resumeRepository.save(resume);
        System.out.println("Resume saved: " + file.getOriginalFilename());
    }

    private String extractText(MultipartFile file) throws IOException {
        PDDocument doc = PDDocument.load(file.getInputStream());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(doc);
        doc.close();
        return text;
    }

    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }
}
