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
                List<String> types = Arrays.asList(
                        "vehicle_insurance:financial_loss",
                        "puc:fine",
                        "passport:legal",
                        "visa:legal",
                        "gym_membership:minor",
                        "domain:financial_loss",
                        "software_license:minor",
                        "amc:financial_loss",
                        "rent_agreement:legal",
                        "warranty:minor",
                        "other:minor"
                );

                for (String t : types) {
                    String[] parts = t.split(":");
                    DocumentType dt = new DocumentType();
                    dt.setName(parts[0]);
                    dt.setDefaultSeverity(parts[1]);
                    dt.setReminderScheduleDays(Arrays.asList(30, 7, 1));
                    documentTypeRepository.save(dt);
                }
            }
        };
    }
}
