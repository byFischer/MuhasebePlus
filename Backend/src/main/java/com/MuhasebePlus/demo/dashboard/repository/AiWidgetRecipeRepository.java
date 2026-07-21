package com.MuhasebePlus.demo.dashboard.repository;

import com.MuhasebePlus.demo.dashboard.entity.AiWidgetRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiWidgetRecipeRepository extends JpaRepository<AiWidgetRecipe, Long> {

    List<AiWidgetRecipe> findByCreatedByUserIdOrderByCreatedAtDesc(Long userId);
}
