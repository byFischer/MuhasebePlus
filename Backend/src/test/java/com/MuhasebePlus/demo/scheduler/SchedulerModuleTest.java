package com.MuhasebePlus.demo.scheduler;

import com.MuhasebePlus.demo.scheduler.controller.SchedulerController;
import com.MuhasebePlus.demo.scheduler.dto.request.ScheduleTaskRequestDto;
import com.MuhasebePlus.demo.scheduler.dto.response.ScheduleTaskResponseDto;
import com.MuhasebePlus.demo.scheduler.entity.ScheduleTask;
import com.MuhasebePlus.demo.scheduler.mapper.ScheduleTaskMapper;
import com.MuhasebePlus.demo.scheduler.service.SchedulerService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scheduler modülü şu an yalnızca iskelet (placeholder) sınıflardan oluşuyor;
 * henüz iş mantığı içermiyor. Bu smoke testi, sınıfların ve kayıtların
 * örneklenebildiğini doğrular ve modül geliştikçe genişletilecek bir başlangıç
 * noktası sağlar.
 */
class SchedulerModuleTest {

    @Test
    void placeholderTypesAreInstantiable() {
        assertThat(new SchedulerService()).isNotNull();
        assertThat(new SchedulerController()).isNotNull();
        assertThat(new ScheduleTask()).isNotNull();
        assertThat(new ScheduleTaskMapper()).isNotNull();
    }

    @Test
    void emptyRecordsAreEqualAndInstantiable() {
        ScheduleTaskRequestDto request = new ScheduleTaskRequestDto();
        ScheduleTaskResponseDto response = new ScheduleTaskResponseDto();

        assertThat(request).isEqualTo(new ScheduleTaskRequestDto());
        assertThat(response).isEqualTo(new ScheduleTaskResponseDto());
    }
}
