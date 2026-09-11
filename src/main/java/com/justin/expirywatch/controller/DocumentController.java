package com.justin.expirywatch.controller;

import com.justin.expirywatch.model.Document;
import com.justin.expirywatch.service.DocumentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping({"/", "/documents"})
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate extractedExpiryDate,
            @RequestParam String severity) {
        
        documentService.createDocument(userDetails.getUsername(), documentTypeId, title, extractedExpiryDate, severity);
        return "redirect:/documents";
    }

    @GetMapping("/{id}")
    public String viewDocument(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Document doc = documentService.getDocument(id, userDetails.getUsername());
        model.addAttribute("document", doc);
        return "documents/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable UUID id, @RequestParam String status, @AuthenticationPrincipal UserDetails userDetails) {
        documentService.updateDocumentStatus(id, userDetails.getUsername(), status);
        return "redirect:/documents/" + id;
    }
}
