package com.MuhasebePlus.demo.dashboard.entity;

import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "widget_definitions")
public class WidgetDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "definition_id")
    private Long definitionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "widget_type", nullable = false, length = 50)
    private WidgetType widgetType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> config;

    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> queryConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visual_config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> visualConfig;

    @Column(name = "is_template", nullable = false)
    private boolean isTemplate = false;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
