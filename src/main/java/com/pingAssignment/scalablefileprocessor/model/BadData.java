package com.pingAssignment.scalablefileprocessor.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class BadData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String rawRecord;

    private String errorReason;

    public BadData() {}

    public BadData(String rawRecord, String errorReason) {
        this.rawRecord = rawRecord;
        this.errorReason = errorReason;
    }
}
