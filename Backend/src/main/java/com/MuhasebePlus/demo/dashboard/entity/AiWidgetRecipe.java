package com.MuhasebePlus.demo.dashboard.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_widget_recipe")
@EqualsAndHashCode(callSuper = false)
public class AiWidgetRecipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recipeId;

    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_config", columnDefinition = "jsonb")
    private String generatedConfig;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
