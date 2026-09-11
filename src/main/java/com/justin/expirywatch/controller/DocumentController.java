package com.justin.expirywatch.controller;

import com.justin.expirywatch.model.Document;
import com.justin.expirywatch.service.DocumentService;
import com.justin.expirywatch.service.DateExtractionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping({"/", "/documents"})
public class DocumentController {

    private final DocumentService documentService;
    private final DateExtractionService dateExtractionService;

    public DocumentController(DocumentService documentService, DateExtractionService dateExtractionService) {
        this.documentService = documentService;
        this.dateExtractionService = dateExtractionService;
    }

    @GetMapping
    public String listDocuments(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("documents", documentService.getDocumentsForUser(userDetails.getUsername()));
        model.addAttribute("email", userDetails.getUsername());
        return "documents/list";
    }

    @GetMapping("/new")
    public String newDocumentForm(Model model) {
        model.addAttribute("types", documentService.getAllDocumentTypes());
        return "documents/new";
    }

    @PostMapping("/new")
    public String createDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Integer documentTypeId,
            @RequestParam String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate extractedExpiryDate,
            @RequestParam String severity,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "confirmed", required = false, defaultValue = "false") boolean confirmed,
            Model model) {
        
        try {
            if (file != null && !file.isEmpty() && !confirmed) {
                // Step 1: User uploaded a file but hasn't confirmed the OCR result yet.
                DateExtractionService.ExtractResult result = dateExtractionService.extractDateFromImage(file);
                
                model.addAttribute("types", documentService.getAllDocumentTypes());
                model.addAttribute("title", title);
                model.addAttribute("documentTypeId", documentTypeId);
                model.addAttribute("severity", severity);
                model.addAttribute("extractedDate", result.date);
                model.addAttribute("rawText", result.rawText);
                model.addAttribute("confidence", result.confidenceScore);
                
                // We shouldn't send the entire byte[] to the template if it's huge, but for V1 simplicity we can just
                // ask them to re-upload on confirmation, or we save it as a draft. 
                // To keep it strictly zero-state between requests, we will require the file input again, or base64 encode it.
                // Given standard constraints, let's just base64 encode it so they don't have to reselect.
                model.addAttribute("fileBase64", java.util.Base64.getEncoder().encodeToString(file.getBytes()));
                model.addAttribute("fileContentType", file.getContentType());
                
                model.addAttribute("needsConfirmation", true);
                return "documents/new";
            } else if (confirmed) {
                // Step 2: Confirmed! Save it. We must get the file bytes from the hidden base64 string.
                // Wait, retrieving base64 from a form submission is easier if we just accept it as a parameter.
            }
        } catch (IOException e) {
            model.addAttribute("error", "Error reading file: " + e.getMessage());
            model.addAttribute("types", documentService.getAllDocumentTypes());
            return "documents/new";
        }
        
        return "redirect:/documents";
    }

    @PostMapping("/confirm")
    public String confirmDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Integer documentTypeId,
            @RequestParam String title,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate extractedExpiryDate,
            @RequestParam String severity,
            @RequestParam String fileBase64,
            @RequestParam String fileContentType,
            @RequestParam String rawText,
            @RequestParam BigDecimal confidence) {

        byte[] fileBytes = java.util.Base64.getDecoder().decode(fileBase64);
        documentService.createDocument(userDetails.getUsername(), documentTypeId, title, extractedExpiryDate, severity, fileBytes, fileContentType, rawText, confidence);
        return "redirect:/documents";
    }

    @GetMapping("/{id}")
    public String viewDocument(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Document doc = documentService.getDocument(id, userDetails.getUsername());
        model.addAttribute("document", doc);
        return "documents/detail";
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        Document doc = documentService.getDocument(id, userDetails.getUsername());
        if (doc.getFileData() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, doc.getFileContentType())
                .body(doc.getFileData());
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable UUID id, @RequestParam String status, @AuthenticationPrincipal UserDetails userDetails) {
        documentService.updateDocumentStatus(id, userDetails.getUsername(), status);
        return "redirect:/documents/" + id;
    }
}
