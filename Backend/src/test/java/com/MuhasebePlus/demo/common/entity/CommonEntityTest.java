package com.MuhasebePlus.demo.common.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommonEntityTest {

    @Test
    void baseEntity_onCreateAndOnUpdateMaintainTimestamps() {
        TestBaseEntity entity = new TestBaseEntity();

        entity.create();
        LocalDateTime createdAt = entity.getCreatedAt();
        LocalDateTime firstUpdatedAt = entity.getUpdatedAt();
        entity.update();

        assertThat(createdAt).isNotNull();
        assertThat(firstUpdatedAt).isNotNull();
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
    }

    @Test
    void softDeletableEntity_defaultsAndSettersWork() {
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 12, 10, 0);

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();

        entity.setDeleted(true);
        entity.setDeletedAt(deletedAt);

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    private static class TestBaseEntity extends BaseEntity {
        void create() {
            onCreate();
        }

        void update() {
            onUpdate();
        }
    }

    private static class TestSoftDeletableEntity extends SoftDeletableEntity {
    }
}
