package com.justin.expirywatch.service;

import com.justin.expirywatch.model.Document;
import com.justin.expirywatch.model.DocumentType;
import com.justin.expirywatch.model.User;
import com.justin.expirywatch.repository.DocumentRepository;
import com.justin.expirywatch.repository.DocumentTypeRepository;
import com.justin.expirywatch.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final UserRepository userRepository;
    private final com.justin.expirywatch.repository.ReminderRepository reminderRepository;

    public DocumentService(DocumentRepository documentRepository, DocumentTypeRepository documentTypeRepository, UserRepository userRepository, com.justin.expirywatch.repository.ReminderRepository reminderRepository) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.userRepository = userRepository;
        this.reminderRepository = reminderRepository;
    }

    public List<com.justin.expirywatch.dto.DocumentView> getDocumentsForUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return documentRepository.findByUserIdOrderByExtractedExpiryDateAsc(user.getId());
    }

    private volatile List<DocumentType> cachedDocumentTypes;

    public List<DocumentType> getAllDocumentTypes() {
        if (cachedDocumentTypes == null || cachedDocumentTypes.isEmpty()) {
            cachedDocumentTypes = documentTypeRepository.findAll();
        }
        return cachedDocumentTypes;
    }

    public Document createDocument(String email, Integer typeId, String title, LocalDate expiryDate, String severity, byte[] fileData, String fileContentType, String rawText, BigDecimal confidence) {
        User user = userRepository.findByEmail(email).orElseThrow();
        DocumentType type = getAllDocumentTypes().stream()
                .filter(t -> t.getId().equals(typeId))
                .findFirst()
                .orElseGet(() -> documentTypeRepository.findById(typeId).orElseThrow());

        Document doc = new Document();
        doc.setUser(user);
        doc.setDocumentType(type);
        doc.setTitle(title);
        doc.setExtractedExpiryDate(expiryDate);
        doc.setSeverity(severity);
        doc.setStatus("active");
        doc.setFileData(fileData);
        doc.setFileContentType(fileContentType);
        doc.setRawOcrText(rawText);
        doc.setConfidenceScore(confidence);
        
        doc = documentRepository.save(doc);

        for (Integer days : type.getReminderScheduleDays()) {
            LocalDate reminderDate = doc.getExtractedExpiryDate().minusDays(days);
            if (!reminderDate.isBefore(LocalDate.now())) {
                com.justin.expirywatch.model.Reminder reminder = new com.justin.expirywatch.model.Reminder();
                reminder.setDocument(doc);
                reminder.setScheduledFor(reminderDate);
                reminder.setSent(false);
                reminderRepository.save(reminder);
            }
        }
        
        return doc;
    }

    public Document getDocument(UUID id, String email) {
        Document doc = documentRepository.findById(id).orElseThrow();
        if (!doc.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }
        return doc;
    }

    public void updateDocumentStatus(UUID id, String email, String status) {
        Document doc = getDocument(id, email);
        doc.setStatus(status);
        documentRepository.save(doc);
    }
}
