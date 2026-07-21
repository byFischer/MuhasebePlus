package com.MuhasebePlus.demo.template.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "template")
@EqualsAndHashCode(callSuper = false)
public class Template extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "template_code", length = 20, unique = true)
    private String templateCode;

    @Column(name = "template_name", length = 255, nullable = false)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", length = 30, nullable = false)
    private TemplateType templateType;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "period", length = 20)
    private String period;

    @Lob
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;
}
