package com.MuhasebePlus.demo.customer;

import com.MuhasebePlus.demo.customer.dto.response.ImportResultDto;
import com.MuhasebePlus.demo.customer.mapper.CustomerMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerDtoAndMapperTest {

    @Test
    void importResultDtoAndMapperCanBeConstructed() {
        ImportResultDto result = new ImportResultDto(1, 2, List.of("Hata"));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(2);
        assertThat(result.errors()).containsExactly("Hata");
        assertThat(new CustomerMapper()).isNotNull();
    }
}
