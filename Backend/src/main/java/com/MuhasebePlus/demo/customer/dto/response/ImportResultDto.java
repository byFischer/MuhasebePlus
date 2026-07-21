package com.MuhasebePlus.demo.customer.dto.response;

import java.util.List;

public record ImportResultDto(
    int successCount,
    int errorCount,
    List<String> errors
) {
}
