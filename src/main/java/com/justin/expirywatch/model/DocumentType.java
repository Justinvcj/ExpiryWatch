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

    @Column(name = "reminder_schedule_days", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<Integer> reminderScheduleDays;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDefaultSeverity() { return defaultSeverity; }
    public void setDefaultSeverity(String defaultSeverity) { this.defaultSeverity = defaultSeverity; }
    public List<Integer> getReminderScheduleDays() { return reminderScheduleDays; }
    public void setReminderScheduleDays(List<Integer> reminderScheduleDays) { this.reminderScheduleDays = reminderScheduleDays; }
}
