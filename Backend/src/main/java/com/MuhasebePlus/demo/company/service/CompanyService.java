package com.MuhasebePlus.demo.company.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.dto.request.CompanyRequestDto;
import com.MuhasebePlus.demo.company.dto.response.CompanyResponseDto;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;

    @Transactional
    public CompanyResponseDto createCompany(CompanyRequestDto dto) {
        if (dto.taxNumber() != null && companyRepository.existsByTaxNumber(dto.taxNumber())) {
            throw new RuntimeException("Tax number already exists: " + dto.taxNumber());
        }

        Company company = Company.builder()
                .companyName(dto.companyName())
                .taxOffice(dto.taxOffice())
                .taxNumber(dto.taxNumber())
                .address(dto.address())
                .city(dto.city())
                .phone(dto.phone())
                .email(dto.email())
                .isActive(true)
                .build();

        Company savedCompany = companyRepository.save(company);
        return mapToResponseDto(savedCompany);
    }

    public CompanyResponseDto getCompanyById(Long id) {
        Long currentCompanyId = companyContext.getCurrentCompanyId();
        if (!currentCompanyId.equals(id)) {
            throw new RuntimeException("Bu kaydı görüntüleme yetkiniz yok. Sadece kendi şirketinizi görebilirsiniz.");
        }

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found: " + id));
        return mapToResponseDto(company);
    }

    public List<CompanyResponseDto> getAllCompanies() {
        // Güvenlik gereği bir kullanıcı tüm şirketleri LİSTELEYEMEZ. 
        // Sadece kendi şirketini bir liste içinde döndürüyoruz.
        Long currentCompanyId = companyContext.getCurrentCompanyId();
        Company company = companyRepository.findById(currentCompanyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
                
        return Collections.singletonList(mapToResponseDto(company));
    }

    @Transactional
    public CompanyResponseDto updateCompany(Long id, CompanyRequestDto dto) {
        Long currentCompanyId = companyContext.getCurrentCompanyId();
        if (!currentCompanyId.equals(id)) {
            throw new RuntimeException("Bu kaydı güncelleme yetkiniz yok. Sadece kendi şirketinizi güncelleyebilirsiniz.");
        }

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found: " + id));

        // Vergi no değişiyorsa mükerrerlik kontrolü yap
        if (dto.taxNumber() != null && !dto.taxNumber().equals(company.getTaxNumber())) {
            if (companyRepository.existsByTaxNumber(dto.taxNumber())) {
                throw new RuntimeException("Tax number already exists: " + dto.taxNumber());
            }
        }

        company.setCompanyName(dto.companyName());
        company.setTaxOffice(dto.taxOffice());
        company.setTaxNumber(dto.taxNumber());
        company.setAddress(dto.address());
        company.setCity(dto.city());
        company.setPhone(dto.phone());
        company.setEmail(dto.email());

        Company updatedCompany = companyRepository.save(company);
        return mapToResponseDto(updatedCompany);
    }

    @Transactional
    public void deleteCompany(Long id) {
        Long currentCompanyId = companyContext.getCurrentCompanyId();
        if (!currentCompanyId.equals(id)) {
            throw new RuntimeException("Bu kaydı silme yetkiniz yok. Sadece kendi şirketinizi silebilirsiniz.");
        }

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found: " + id));
        
        // Soft delete - is_active'i false yapıyoruz
        company.setActive(false);
        companyRepository.save(company);
    }

    // Helper metot: Entity -> ResponseDTO dönüşümü
    private CompanyResponseDto mapToResponseDto(Company company) {
        return new CompanyResponseDto(
                company.getCompanyId(),
                company.getCompanyName(),
                company.getTaxOffice(),
                company.getTaxNumber(),
                company.getAddress(),
                company.getCity(),
                company.getPhone(),
                company.getEmail(),
                company.isActive(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
