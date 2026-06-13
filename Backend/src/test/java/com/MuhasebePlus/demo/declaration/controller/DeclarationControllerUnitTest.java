package com.MuhasebePlus.demo.declaration.controller;

import com.MuhasebePlus.demo.declaration.dto.BaBsDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.dto.GenerateDeclarationRequestDto;
import com.MuhasebePlus.demo.declaration.dto.GenericDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.dto.VatDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.dto.WithholdingDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.entity.BaBsDeclaration;
import com.MuhasebePlus.demo.declaration.entity.BaBsDeclarationLine;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
import com.MuhasebePlus.demo.declaration.entity.GenericDeclaration;
import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclaration;
import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclarationLine;
import com.MuhasebePlus.demo.declaration.service.BaBsDeclarationService;
import com.MuhasebePlus.demo.declaration.service.GenericDeclarationService;
import com.MuhasebePlus.demo.declaration.service.VatDeclarationService;
import com.MuhasebePlus.demo.declaration.service.WithholdingDeclarationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeclarationControllerUnitTest {

    @Mock
    private VatDeclarationService vatService;
    @Mock
    private GenericDeclarationService genericService;
    @Mock
    private WithholdingDeclarationService withholdingService;
    @Mock
    private BaBsDeclarationService baBsService;

    @Test
    void vatController_delegatesAllEndpointsAndBuildsXmlDownload() {
        VatDeclarationController controller = new VatDeclarationController(vatService);
        GenerateDeclarationRequestDto request = new GenerateDeclarationRequestDto(2026, 5, BigDecimal.TEN);
        VatDeclarationResponseDto response = vatResponse(3L);
        when(vatService.generate(request)).thenReturn(response);
        when(vatService.list()).thenReturn(List.of(response));
        when(vatService.getById(3L)).thenReturn(response);
        when(vatService.finalize(3L)).thenReturn(response);
        when(vatService.markSubmitted(3L)).thenReturn(response);
        when(vatService.getXml(3L)).thenReturn("<xml/>");

        assertThat(controller.generate(request).getBody()).isSameAs(response);
        assertThat(controller.list().getBody()).containsExactly(response);
        assertThat(controller.getById(3L).getBody()).isSameAs(response);
        assertThat(controller.finalize(3L).getBody()).isSameAs(response);
        assertThat(controller.markSubmitted(3L).getBody()).isSameAs(response);

        ResponseEntity<byte[]> xml = controller.downloadXml(3L);
        assertThat(xml.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_XML);
        assertThat(xml.getHeaders().getContentDisposition().getFilename()).isEqualTo("kdv1-beyannamesi.xml");
        assertThat(new String(xml.getBody(), StandardCharsets.UTF_8)).isEqualTo("<xml/>");

        assertThat(controller.delete(3L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(vatService).delete(3L);
    }

    @Test
    void genericController_mapsEntitiesToResponseDtos() {
        GenericDeclarationController controller = new GenericDeclarationController(genericService);
        GenericDeclaration declaration = GenericDeclaration.builder()
                .declarationId(9L)
                .declarationType(GenericDeclarationService.KDV2)
                .year(2026)
                .period(5)
                .status(DeclarationStatus.DRAFT)
                .totalPayable(new BigDecimal("120.00"))
                .dataSnapshot(Map.of("total", new BigDecimal("120.00")))
                .build();
        when(genericService.generate(GenericDeclarationService.KDV2, 2026, 5)).thenReturn(declaration);
        when(genericService.list(GenericDeclarationService.KDV2)).thenReturn(List.of(declaration));
        when(genericService.getById(9L)).thenReturn(declaration);
        when(genericService.finalize(9L)).thenReturn(declaration);
        when(genericService.markSubmitted(9L)).thenReturn(declaration);

        GenericDeclarationResponseDto generated = controller.generate(GenericDeclarationService.KDV2, 2026, 5).getBody();

        assertThat(generated.declarationId()).isEqualTo(9L);
        assertThat(generated.totalPayable()).isEqualByComparingTo("120.00");
        assertThat(controller.list(GenericDeclarationService.KDV2).getBody()).hasSize(1);
        assertThat(controller.getById(9L).getBody().declarationType()).isEqualTo(GenericDeclarationService.KDV2);
        assertThat(controller.finalize(9L).getBody().status()).isEqualTo(DeclarationStatus.DRAFT);
        assertThat(controller.markSubmitted(9L).getBody().dataSnapshot()).containsKey("total");
        assertThat(controller.delete(9L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(genericService).delete(9L);
    }

    @Test
    void withholdingController_mapsDeclarationAndLines() {
        WithholdingDeclarationController controller = new WithholdingDeclarationController(withholdingService);
        WithholdingDeclaration declaration = WithholdingDeclaration.builder()
                .declarationId(4L)
                .year(2026)
                .month(5)
                .status(DeclarationStatus.DRAFT)
                .totalWithholdingBase(new BigDecimal("1000.00"))
                .totalWithholdingAmount(new BigDecimal("200.00"))
                .lines(List.of(WithholdingDeclarationLine.builder()
                        .lineId(44L)
                        .withholdingTaxCodeId(1)
                        .withholdingCode("601")
                        .description("Hizmet")
                        .baseAmount(new BigDecimal("1000.00"))
                        .withholdingAmount(new BigDecimal("200.00"))
                        .documentCount(2)
                        .lineOrder(0)
                        .build()))
                .build();
        when(withholdingService.generate(2026, 5)).thenReturn(declaration);
        when(withholdingService.list()).thenReturn(List.of(declaration));
        when(withholdingService.getById(4L)).thenReturn(declaration);
        when(withholdingService.finalize(4L)).thenReturn(declaration);

        WithholdingDeclarationResponseDto response = controller.generate(2026, 5).getBody();

        assertThat(response.lines()).singleElement().satisfies(line -> {
            assertThat(line.withholdingCode()).isEqualTo("601");
            assertThat(line.withholdingAmount()).isEqualByComparingTo("200.00");
        });
        assertThat(controller.list().getBody()).hasSize(1);
        assertThat(controller.getById(4L).getBody().totalWithholdingBase()).isEqualByComparingTo("1000.00");
        assertThat(controller.finalize(4L).getBody().declarationId()).isEqualTo(4L);
        assertThat(controller.delete(4L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(withholdingService).delete(4L);
    }

    @Test
    void babsController_mapsDeclarationAndLines() {
        BaBsDeclarationController controller = new BaBsDeclarationController(baBsService);
        BaBsDeclaration declaration = BaBsDeclaration.builder()
                .declarationId(8L)
                .babsType("BA")
                .year(2026)
                .month(5)
                .status(DeclarationStatus.DRAFT)
                .recordCount(1)
                .totalAmount(new BigDecimal("7500.00"))
                .lines(List.of(BaBsDeclarationLine.builder()
                        .lineId(81L)
                        .taxNumber("1234567890")
                        .customerName("Ada Ltd.")
                        .totalAmount(new BigDecimal("7500.00"))
                        .documentCount(3)
                        .build()))
                .build();
        when(baBsService.generate(2026, 5, "BA")).thenReturn(declaration);
        when(baBsService.list()).thenReturn(List.of(declaration));
        when(baBsService.getById(8L)).thenReturn(declaration);
        when(baBsService.finalize(8L)).thenReturn(declaration);

        BaBsDeclarationResponseDto response = controller.generate(2026, 5, "BA").getBody();

        assertThat(response.lines()).singleElement().satisfies(line -> {
            assertThat(line.customerName()).isEqualTo("Ada Ltd.");
            assertThat(line.documentCount()).isEqualTo(3);
        });
        assertThat(controller.list().getBody()).hasSize(1);
        assertThat(controller.getById(8L).getBody().babsType()).isEqualTo("BA");
        assertThat(controller.finalize(8L).getBody().totalAmount()).isEqualByComparingTo("7500.00");
        assertThat(controller.delete(8L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(baBsService).delete(8L);
    }

    private VatDeclarationResponseDto vatResponse(Long id) {
        return new VatDeclarationResponseDto(
                id,
                "KDV1",
                2026,
                5,
                DeclarationStatus.DRAFT,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                List.of()
        );
    }
}
