package com.MuhasebePlus.demo.dashboard.controller;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetDefinitionRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.WidgetDefinitionResponseDto;
import com.MuhasebePlus.demo.dashboard.entity.WidgetDefinition;
import com.MuhasebePlus.demo.dashboard.repository.WidgetDefinitionRepository;
import com.MuhasebePlus.demo.dashboard.dto.query.QueryConfigDto;
import com.MuhasebePlus.demo.dashboard.service.DynamicQueryService;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/widget-definitions")
@RequiredArgsConstructor
public class WidgetDefinitionController {

    private final WidgetDefinitionRepository definitionRepository;
    private final CompanyContext companyContext;
    private final DynamicQueryService dynamicQueryService;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<WidgetDefinitionResponseDto>> getDefinitions() {
        Long companyId = companyContext.getCurrentCompanyId();
        List<WidgetDefinitionResponseDto> list = definitionRepository
                .findByCompanyCompanyIdOrIsSystemTrue(companyId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<WidgetDefinitionResponseDto> getDefinition(@PathVariable Long id) {
        WidgetDefinition def = loadAccessible(id);
        return ResponseEntity.ok(toDto(def));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<WidgetDefinitionResponseDto> createDefinition(
            @Valid @RequestBody WidgetDefinitionRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        WidgetDefinition def = new WidgetDefinition();
        applyDtoToEntity(dto, def);
        def.setCompany(companyRepository.getReferenceById(companyId));
        def.setUser(userRepository.getReferenceById(userId));
        def.setSystem(false);

        WidgetDefinition saved = definitionRepository.save(def);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<WidgetDefinitionResponseDto> updateDefinition(
            @PathVariable Long id, @Valid @RequestBody WidgetDefinitionRequestDto dto) {
        WidgetDefinition existing = loadOwned(id);
        applyDtoToEntity(dto, existing);
        existing.setUpdatedAt(LocalDateTime.now());
        WidgetDefinition saved = definitionRepository.save(existing);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> deleteDefinition(@PathVariable Long id) {
        WidgetDefinition existing = loadOwned(id);
        definitionRepository.delete(existing);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<WidgetDefinitionResponseDto> cloneDefinition(@PathVariable Long id) {
        WidgetDefinition source = loadAccessible(id);
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        WidgetDefinition clone = new WidgetDefinition();
        clone.setName(source.getName() + " (Kopya)");
        clone.setDescription(source.getDescription());
        clone.setWidgetType(source.getWidgetType());
        clone.setDataSource(source.getDataSource());
        clone.setQueryConfig(deepCopy(source.getQueryConfig()));
        clone.setVisualConfig(deepCopy(source.getVisualConfig()));
        clone.setConfig(deepCopy(source.getConfig()));
        clone.setCompany(companyRepository.getReferenceById(companyId));
        clone.setUser(userRepository.getReferenceById(userId));
        clone.setSystem(false);
        clone.setTemplate(false);

        WidgetDefinition saved = definitionRepository.save(clone);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> previewQuery(@RequestBody Map<String, Object> body) {
        QueryConfigDto config = objectMapper.convertValue(body.get("queryConfig"), QueryConfigDto.class);
        Long companyId = companyContext.getCurrentCompanyId();
        try {
            DynamicQueryService.QueryResult result = dynamicQueryService.executeQuery(config, companyId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.rows(),
                    "columns", result.columns(),
                    "aggregateMeta", result.aggregateMeta(),
                    "totalCount", result.totalCount()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private WidgetDefinition loadAccessible(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        WidgetDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Widget definition not found"));
        boolean accessible = def.isSystem()
                || (def.getCompany() != null && companyId.equals(def.getCompany().getCompanyId()));
        if (!accessible) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        return def;
    }

    private WidgetDefinition loadOwned(Long id) {
        WidgetDefinition def = loadAccessible(id);
        if (def.isSystem()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sistem şablonları değiştirilemez veya silinemez. Önce 'Kopyala' ile kendi widget'ınızı oluşturun.");
        }
        return def;
    }

    private void applyDtoToEntity(WidgetDefinitionRequestDto dto, WidgetDefinition entity) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setWidgetType(dto.widgetType());
        entity.setDataSource(dto.dataSource());
        entity.setQueryConfig(dto.queryConfig() != null ? dto.queryConfig() : Map.of());
        entity.setVisualConfig(dto.visualConfig() != null ? dto.visualConfig() : Map.of());
        entity.setConfig(dto.config() != null ? dto.config() : Map.of());
    }

    private WidgetDefinitionResponseDto toDto(WidgetDefinition def) {
        return new WidgetDefinitionResponseDto(
                def.getDefinitionId(),
                def.getName(),
                def.getDescription(),
                def.getWidgetType(),
                def.getDataSource(),
                def.getQueryConfig(),
                def.getVisualConfig(),
                def.getConfig(),
                def.isSystem(),
                def.isTemplate(),
                def.getCreatedAt(),
                def.getUpdatedAt()
        );
    }

    private Map<String, Object> deepCopy(Map<String, Object> source) {
        if (source == null) return new HashMap<>();
        return new HashMap<>(source);
    }
}
