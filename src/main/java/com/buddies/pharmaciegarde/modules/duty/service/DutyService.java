package com.buddies.pharmaciegarde.modules.duty.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.buddies.pharmaciegarde.modules.duty.dto.DutyCreateDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyFileResponseDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyResponseDto;
import com.buddies.pharmaciegarde.modules.duty.dto.PdfProcessingResultDto;

public interface DutyService {

	DutyResponseDto create(DutyCreateDto dutyCreateDto);

	DutyResponseDto getById(Long id);

	List<DutyResponseDto> getAll();

	List<DutyResponseDto> getByPharmacyId(Long pharmacyId);

	List<DutyResponseDto> getPharmacyHistory(Long pharmacyId);

	List<DutyResponseDto> getByDateRange(LocalDate startDate, LocalDate endDate);

	DutyResponseDto update(Long id, DutyCreateDto dutyCreateDto);

	void delete(Long id);

	DutyFileResponseDto uploadPdfFile(MultipartFile file, String description, Long dutyPeriodId);

	List<DutyFileResponseDto> uploadPdfFiles(MultipartFile[] files, String description, Long dutyPeriodId);

	PdfProcessingResultDto processPdfFile(Long dutyFileId);

	PdfProcessingResultDto processPdfFile(Long dutyFileId, boolean autoCreateUnknown);

	com.buddies.pharmaciegarde.common.api.PageResponse<DutyResponseDto> getPage(int page, int size);

	com.buddies.pharmaciegarde.common.api.PageResponse<DutyFileResponseDto> getFilesPage(int page, int size);

	List<DutyFileResponseDto> getAllDutyFiles();

	List<DutyFileResponseDto> getDutyFilesByPeriodId(Long dutyPeriodId);

	void deleteDutyFile(Long id);

	com.buddies.pharmaciegarde.modules.duty.dto.TextExtractionResultDto extractTextOnly(Long dutyFileId);

	// Nouvelles méthodes pour le processus nominal
	List<DutyResponseDto> getDutiesByPeriodId(Long dutyPeriodId);

	void clearDutiesByPeriodId(Long dutyPeriodId);

	// Méthode pour traiter un fichier Excel
	com.buddies.pharmaciegarde.modules.duty.dto.ExcelProcessingResultDto processExcelFile(MultipartFile file, Long dutyPeriodId);

	// Méthode pour signaler une pharmacie de garde (app mobile)
	DutyResponseDto reportDuty(com.buddies.pharmaciegarde.modules.duty.dto.DutyReportDto dutyReportDto);

	// Méthode pour désactiver une pharmacie de garde (back office)
	DutyResponseDto disableDuty(Long dutyId);

	// Méthode pour réactiver une pharmacie de garde (back office)
	DutyResponseDto enableDuty(Long dutyId);
}

