package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;

public interface PDFProcessingService {

	List<ExtractedPharmacyDto> extractPharmaciesFromPdf(InputStream inputStream) throws IOException;

	default List<ExtractedPharmacyDto> extractPharmaciesFromPdf(MultipartFile file) throws IOException {
		return extractPharmaciesFromPdf(file.getInputStream());
	}
}

