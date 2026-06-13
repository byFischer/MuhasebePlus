package com.MuhasebePlus.demo.dashboard.controller;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.dto.query.QueryConfigDto;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetDefinitionRequestDto;
import com.MuhasebePlus.demo.dashboard.entity.WidgetDefinition;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.repository.WidgetDefinitionRepository;
import com.MuhasebePlus.demo.dashboard.service.DynamicQueryService;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WidgetDefinitionControllerTest {

    @Mock private WidgetDefinitionRepository definitionRepository;
    @Mock private CompanyContext companyContext;
    @Mock private DynamicQueryService dynamicQueryService;
    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;

    private WidgetDefinitionController controller;
    private Company company;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new WidgetDefinitionController(
                definitionRepository,
                companyContext,
                dynamicQueryService,
                companyRepository,
                userRepository);
        company = Company.builder().companyId(1L).companyName("Test").build();
        user = new User();
        user.setUserId(7L);
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
    }

    @Test
    void getDefinitions_returnsCompanyAndSystemDefinitions() {
        when(definitionRepository.findByCompanyCompanyIdOrIsSystemTrue(1L))
                .thenReturn(List.of(definition(1L, company, false), definition(2L, null, true)));

        var response = controller.getDefinitions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().getFirst().definitionId()).isEqualTo(1L);
        assertThat(response.getBody().get(1).isSystem()).isTrue();
    }

    @Test
    void getDefinition_whenAccessible_returnsDto() {
        when(definitionRepository.findById(2L)).thenReturn(Optional.of(definition(2L, null, true)));

        var response = controller.getDefinition(2L);

        assertThat(response.getBody().definitionId()).isEqualTo(2L);
        assertThat(response.getBody().isSystem()).isTrue();
    }

    @Test
    void createDefinition_appliesCurrentCompanyAndUser() {
        WidgetDefinitionRequestDto request = request("Gelir", WidgetType.DATA_KPI);
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(definitionRepository.save(any(WidgetDefinition.class))).thenAnswer(inv -> {
            WidgetDefinition saved = inv.getArgument(0);
            saved.setDefinitionId(10L);
            return saved;
        });

        var response = controller.createDefinition(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().definitionId()).isEqualTo(10L);
        assertThat(response.getBody().name()).isEqualTo("Gelir");
        ArgumentCaptor<WidgetDefinition> captor = ArgumentCaptor.forClass(WidgetDefinition.class);
        verify(definitionRepository).save(captor.capture());
        assertThat(captor.getValue().getCompany()).isEqualTo(company);
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().isSystem()).isFalse();
    }

    @Test
    void updateDefinition_whenOwned_updatesMutableFields() {
        WidgetDefinition existing = definition(3L, company, false);
        when(definitionRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(definitionRepository.save(existing)).thenReturn(existing);

        var response = controller.updateDefinition(3L, request("Guncel", WidgetType.DATA_TABLE));

        assertThat(response.getBody().name()).isEqualTo("Guncel");
        assertThat(response.getBody().widgetType()).isEqualTo(WidgetType.DATA_TABLE);
        assertThat(existing.getUpdatedAt()).isNotNull();
    }

    @Test
    void deleteDefinition_whenOwned_deletesAndReturnsSuccess() {
        WidgetDefinition existing = definition(4L, company, false);
        when(definitionRepository.findById(4L)).thenReturn(Optional.of(existing));

        var response = controller.deleteDefinition(4L);

        assertThat(response.getBody()).containsEntry("success", true);
        verify(definitionRepository).delete(existing);
    }

    @Test
    void cloneDefinition_copiesSourceAsUserOwnedDefinition() {
        WidgetDefinition source = definition(5L, null, true);
        source.setTemplate(true);
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(definitionRepository.findById(5L)).thenReturn(Optional.of(source));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(definitionRepository.save(any(WidgetDefinition.class))).thenAnswer(inv -> {
            WidgetDefinition clone = inv.getArgument(0);
            clone.setDefinitionId(6L);
            return clone;
        });

        var response = controller.cloneDefinition(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("Def 5 (Kopya)");
        assertThat(response.getBody().isSystem()).isFalse();
        assertThat(response.getBody().isTemplate()).isFalse();
    }

    @Test
    void previewQuery_returnsSuccessOrFailurePayload() {
        when(dynamicQueryService.executeQuery(any(QueryConfigDto.class), eq(1L)))
                .thenReturn(new DynamicQueryService.QueryResult(
                        List.of(Map.of("value", 1)),
                        List.of(new DynamicQueryService.ColumnMeta("value", "Deger", "NUMBER")),
                        Map.of("alias", "value"),
                        1L))
                .thenThrow(new RuntimeException("bad query"));

        var success = controller.previewQuery(Map.of("queryConfig", Map.of("dataSource", "INVOICE")));
        var failure = controller.previewQuery(Map.of("queryConfig", Map.of("dataSource", "UNKNOWN")));

        assertThat(success.getBody()).containsEntry("success", true).containsEntry("totalCount", 1L);
        assertThat(failure.getBody()).containsEntry("success", false).containsEntry("message", "bad query");
    }

    @Test
    void loadOwnedRejectsSystemDefinitionsAndLoadAccessibleRejectsOtherCompany() {
        Company other = Company.builder().companyId(99L).companyName("Other").build();
        when(definitionRepository.findById(8L)).thenReturn(Optional.of(definition(8L, company, true)));
        when(definitionRepository.findById(9L)).thenReturn(Optional.of(definition(9L, other, false)));

        assertThatThrownBy(() -> controller.updateDefinition(8L, request("x", WidgetType.DATA_KPI)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> controller.getDefinition(9L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void loadAccessible_whenMissing_returnsNotFound() {
        when(definitionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getDefinition(404L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private WidgetDefinitionRequestDto request(String name, WidgetType type) {
        return new WidgetDefinitionRequestDto(
                name,
                "Aciklama",
                type,
                "INVOICE",
                Map.of("dataSource", "INVOICE"),
                Map.of("chartType", "BAR"),
                Map.of("color", "blue"));
    }

    private WidgetDefinition definition(Long id, Company owner, boolean system) {
        WidgetDefinition definition = new WidgetDefinition();
        definition.setDefinitionId(id);
        definition.setCompany(owner);
        definition.setName("Def " + id);
        definition.setDescription("Aciklama");
        definition.setWidgetType(WidgetType.DATA_CHART);
        definition.setDataSource("INVOICE");
        definition.setQueryConfig(Map.of("dataSource", "INVOICE"));
        definition.setVisualConfig(Map.of("chartType", "BAR"));
        definition.setConfig(Map.of("color", "blue"));
        definition.setSystem(system);
        return definition;
    }
}
