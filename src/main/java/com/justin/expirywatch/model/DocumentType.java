package com.justin.expirywatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

@Entity
@Table(name = "document_types")
public class DocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "default_severity", nullable = false)
    private String defaultSeverity;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reminder_schedule_days", columnDefinition = "integer[]")
    private List<Integer> reminderScheduleDays;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "related_domains", columnDefinition = "text[]")
    private List<String> relatedDomains;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDefaultSeverity() { return defaultSeverity; }
    public void setDefaultSeverity(String defaultSeverity) { this.defaultSeverity = defaultSeverity; }
    public List<Integer> getReminderScheduleDays() { return reminderScheduleDays; }
    public void setReminderScheduleDays(List<Integer> reminderScheduleDays) { this.reminderScheduleDays = reminderScheduleDays; }
    public List<String> getRelatedDomains() { return relatedDomains; }
    public void setRelatedDomains(List<String> relatedDomains) { this.relatedDomains = relatedDomains; }
}
