package com.justin.expirywatch.service;

import com.justin.expirywatch.model.Reminder;
import com.justin.expirywatch.model.Document;
import com.justin.expirywatch.repository.ReminderRepository;
import com.justin.expirywatch.repository.DocumentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderScheduler {

    private final ReminderRepository reminderRepository;
    private final DocumentRepository documentRepository;
    private final GmailService gmailService;

    public ReminderScheduler(ReminderRepository reminderRepository, DocumentRepository documentRepository, GmailService gmailService) {
        this.reminderRepository = reminderRepository;
        this.documentRepository = documentRepository;
        this.gmailService = gmailService;
    }

    // Run every hour
    @Scheduled(fixedRate = 3600000)
    public void processReminders() {
        LocalDate today = LocalDate.now();
        List<Reminder> dueReminders = reminderRepository.findByScheduledForLessThanEqualAndSentFalse(today);

        for (Reminder reminder : dueReminders) {
            try {
                String toEmail = reminder.getDocument().getUser().getEmail();
                String docTitle = reminder.getDocument().getTitle();
                long daysLeft = ChronoUnit.DAYS.between(today, reminder.getDocument().getExtractedExpiryDate());

                String severity = reminder.getDocument().getSeverity();
                String urgencyText = "";
                if ("legal".equals(severity) || "fine".equals(severity)) {
                    urgencyText = "URGENT: Missing this renewal could result in a fine or legal issue.\n\n";
                }

                String subject = "Reminder: " + docTitle + " expires in " + daysLeft + " days!";
                String body = "Hello,\n\n" +
                        urgencyText +
                        "This is a reminder that your document '" + docTitle + "' is set to expire on " +
                        reminder.getDocument().getExtractedExpiryDate() + ".\n\n" +
                        "Please renew it to avoid any issues.\n\n" +
                        "Best,\nExpiryWatch";

                gmailService.sendEmail(toEmail, subject, body);

                reminder.setSent(true);
                reminder.setSentAt(LocalDateTime.now());
                reminderRepository.save(reminder);

            } catch (Exception e) {
                System.err.println("Failed to send reminder " + reminder.getId() + ": " + e.getMessage());
            }
        }
    }

    // Run daily at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void autoExpireDocuments() {
        LocalDate today = LocalDate.now();
        List<Document> documents = documentRepository.findAll();
        for (Document doc : documents) {
            if ("active".equals(doc.getStatus()) && doc.getExtractedExpiryDate().isBefore(today)) {
                doc.setStatus("expired");
                documentRepository.save(doc);
            }
        }
    }
}
