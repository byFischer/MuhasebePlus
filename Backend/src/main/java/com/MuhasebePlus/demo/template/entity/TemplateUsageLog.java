package com.MuhasebePlus.demo.template.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "template_usage_log")
public class TemplateUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "target_entity_type", length = 50)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private Long targetEntityId;
}
