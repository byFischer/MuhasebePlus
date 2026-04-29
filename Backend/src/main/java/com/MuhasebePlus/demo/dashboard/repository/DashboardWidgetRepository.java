package com.MuhasebePlus.demo.dashboard.repository;

import com.MuhasebePlus.demo.dashboard.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

    List<DashboardWidget> findByLayoutLayoutIdOrderByPositionYAscPositionXAsc(Long layoutId);

    Optional<DashboardWidget> findByWidgetIdAndLayoutLayoutId(Long widgetId, Long layoutId);

    @Modifying
    @Query("DELETE FROM DashboardWidget w WHERE w.layout.layoutId = :layoutId")
    void deleteAllByLayoutId(Long layoutId);
}
