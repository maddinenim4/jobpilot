package com.jobpilot.backend.service;

import com.jobpilot.backend.model.Resume;
import com.jobpilot.backend.repository.ResumeRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;

    public ResumeService(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    public Resume uploadResume(Long userId, String resumeLabel, MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.setResumeLabel(resumeLabel);
            resume.setFileName(fileName);
            resume.setContentType(file.getContentType());
            resume.setFileData(file.getBytes());
            resume.setFileSize(file.getSize());

            if (fileName != null && fileName.toLowerCase().endsWith(".docx")) {
                resume.setExtractedText(extractTextFromDocx(file.getBytes()));
            } else if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                resume.setExtractedText(extractTextFromPdf(file.getBytes()));
            } else {
                resume.setExtractedText("[Unsupported file format]");
            }

            return resumeRepository.save(resume);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process resume file: " + e.getMessage(), e);
        }
    }

    public List<Resume> getResumesByUser(Long userId) {
        return resumeRepository.findByUserId(userId);
    }

    public Resume getResumeById(Long id, Long userId) {
        return resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Resume not found with id: " + id));
    }

    public void deleteResume(Long id, Long userId) {
        Resume resume = getResumeById(id, userId);
        resumeRepository.delete(resume);
    }

    public Resume updateLabel(Long id, Long userId, String newLabel) {
        Resume resume = getResumeById(id, userId);
        resume.setResumeLabel(newLabel);
        return resumeRepository.save(resume);
    }

    public byte[] getResumeFileData(Long id, Long userId) {
        Resume resume = getResumeById(id, userId);
        if (resume.getFileData() == null) {
            throw new RuntimeException("No file data found for resume: " + id);
        }
        return resume.getFileData();
    }

    private String extractTextFromPdf(byte[] pdfBytes) {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            return "[PDF text extraction failed]";
        }
    }

    private String extractTextFromDocx(byte[] docxBytes) {
        try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(docxBytes);
             org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(bis)) {
            org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(document);
            String text = extractor.getText();
            extractor.close();
            return text;
        } catch (Exception e) {
            return "[DOCX text extraction failed]";
        }
    }
}
