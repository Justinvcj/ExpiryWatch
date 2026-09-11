package com.justin.expirywatch.config;

import com.justin.expirywatch.model.DocumentType;
import com.justin.expirywatch.repository.DocumentTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(DocumentTypeRepository documentTypeRepository) {
        return args -> {
            if (documentTypeRepository.count() == 0) {
                DocumentType insurance = new DocumentType();
                insurance.setName("Car Insurance");
                insurance.setDefaultSeverity("financial_loss");
                insurance.setReminderScheduleDays(Arrays.asList(30, 7, 1));
                insurance.setRelatedDomains(Arrays.asList("geico.com", "statefarm.com", "progressive.com"));

                DocumentType passport = new DocumentType();
                passport.setName("Passport");
                passport.setDefaultSeverity("legal");
                passport.setReminderScheduleDays(Arrays.asList(180, 90, 30));
                passport.setRelatedDomains(Arrays.asList("travel.state.gov", "passport.gov"));

                DocumentType license = new DocumentType();
                license.setName("Driver's License");
                license.setDefaultSeverity("fine");
                license.setReminderScheduleDays(Arrays.asList(60, 30, 7));
                license.setRelatedDomains(Arrays.asList("dmv.org", "dmv.ca.gov", "dmv.ny.gov"));

                DocumentType sub = new DocumentType();
                sub.setName("Subscription");
                sub.setDefaultSeverity("minor");
                sub.setReminderScheduleDays(Arrays.asList(7, 1));
                sub.setRelatedDomains(Arrays.asList("netflix.com", "spotify.com", "amazon.com", "apple.com"));

                documentTypeRepository.saveAll(Arrays.asList(insurance, passport, license, sub));
            }
        };
    }
}
