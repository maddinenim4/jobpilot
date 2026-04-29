package com.jobpilot.backend.service;

import com.jobpilot.backend.model.Resume;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService {

    public void sendApplicationEmail(String senderEmail, String gmailAppPassword,
                                      String toEmail, String subject,
                                      String emailBody, Resume resume,
                                      String senderSignature) {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, gmailAppPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            // Build full body with signature
            String fullBody = emailBody;
            if (senderSignature != null && !senderSignature.isBlank()) {
                fullBody = emailBody + "\n\n" + senderSignature;
            }

            // Convert to HTML with proper line breaks
            String htmlBody = fullBody.replace("\n", "<br>");

            String fullHtml = "<html><body><p>" + htmlBody + "</p></body></html>";

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(fullHtml, "text/html; charset=utf-8");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource dataSource = new ByteArrayDataSource(
                    resume.getFileData(), resume.getContentType());
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            attachmentPart.setFileName(resume.getFileName());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + toEmail + ": " + e.getMessage(), e);
        }
    }
}
