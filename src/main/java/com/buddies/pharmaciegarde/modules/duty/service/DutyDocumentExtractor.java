package com.buddies.pharmaciegarde.modules.duty.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;

/**
 * Extraction d'une affiche. Google Vision reste le moteur par défaut.
 * Un LLM vision n'est utilisé que s'il est explicitement activé, avec repli sur l'OCR.
 */
@Service
public class DutyDocumentExtractor {

	private static final Logger logger = LoggerFactory.getLogger(DutyDocumentExtractor.class);

	private final DocumentProcessingService documentProcessingService;
	private final GoogleVisionService googleVisionService;
	private final ObjectProvider<LlmVisionExtractor> llmVisionExtractor;

	public DutyDocumentExtractor(DocumentProcessingService documentProcessingService,
			GoogleVisionService googleVisionService,
			ObjectProvider<LlmVisionExtractor> llmVisionExtractor) {
		this.documentProcessingService = documentProcessingService;
		this.googleVisionService = googleVisionService;
		this.llmVisionExtractor = llmVisionExtractor;
	}

	public List<ExtractedPharmacyDto> extract(Path filePath, String fileName) throws Exception {
		byte[] fileBytes = Files.readAllBytes(filePath);
		try (java.io.InputStream inputStream = Files.newInputStream(filePath)) {
			boolean needsOcr = documentProcessingService.requiresOCR(inputStream, fileName);
			if (!needsOcr) {
				try (java.io.InputStream textStream = Files.newInputStream(filePath)) {
					return documentProcessingService.extractPharmacies(textStream, fileName);
				}
			}
		}
		LlmVisionExtractor llm = llmVisionExtractor.getIfAvailable();
		if (llm != null && llm.isEnabled()) {
			try {
				List<ExtractedPharmacyDto> fromLlm = llm.extract(fileBytes, fileName);
				if (fromLlm != null && !fromLlm.isEmpty()) {
					logger.info("Extraction structurée LLM : {} ligne(s)", fromLlm.size());
					return fromLlm;
				}
			} catch (Exception ex) {
				logger.warn("LLM vision indisponible, repli sur Google Vision : {}", ex.getMessage());
			}
		}
		if (googleVisionService == null) {
			throw new IllegalStateException("Aucun moteur OCR n'est configuré pour ce fichier");
		}
		return googleVisionService.extractPharmaciesFromImage(fileBytes);
	}
}
