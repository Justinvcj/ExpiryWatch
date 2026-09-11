package com.justin.expirywatch.controller;

import com.justin.expirywatch.model.Document;
import com.justin.expirywatch.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ApiController {
    
    private final DocumentService documentService;

    public ApiController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/check")
    public ResponseEntity<List<Document>> checkDocumentsForDomain(
            @RequestParam String domain,
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal UserDetails userDetails) {
            
        String targetEmail = null;
        if (userDetails != null) {
            targetEmail = userDetails.getUsername();
        } else if (email != null && !email.isEmpty()) {
            targetEmail = email;
        } else {
            return ResponseEntity.status(401).build();
        }

        List<Document> userDocs = documentService.getDocumentsForUser(targetEmail);
        
        List<Document> matchedDocs = userDocs.stream()
                .filter(doc -> "active".equals(doc.getStatus()))
                .filter(doc -> {
                    List<String> domains = doc.getDocumentType().getRelatedDomains();
                    if (domains != null) {
                        return domains.stream().anyMatch(d -> domain.contains(d));
                    }
                    return false;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(matchedDocs);
    }
}
