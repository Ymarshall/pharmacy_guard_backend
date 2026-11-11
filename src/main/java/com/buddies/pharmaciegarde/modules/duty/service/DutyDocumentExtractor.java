package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.buddies.pharmaciegarde.common.exception.OcrUnavailableException;
import com.buddies.pharmaciegarde.common.exception.UnreadableDocumentException;
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

	public List<ExtractedPharmacyDto> extract(Path filePath, String fileName) {
		byte[] fileBytes;
		try {
			fileBytes = Files.readAllBytes(filePath);
		} catch (IOException ex) {
			throw new UnreadableDocumentException("Fichier illisible");
		}
		if (fileBytes.length == 0) {
			throw new UnreadableDocumentException("Fichier illisible");
		}
		try (java.io.InputStream inputStream = Files.newInputStream(filePath)) {
			boolean needsOcr = documentProcessingService.requiresOCR(inputStream, fileName);
			if (!needsOcr) {
				try (java.io.InputStream textStream = Files.newInputStream(filePath)) {
					return documentProcessingService.extractPharmacies(textStream, fileName);
				}
			}
		} catch (UnreadableDocumentException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.warn("Lecture impossible du fichier {}", fileName);
			throw new UnreadableDocumentException("Fichier illisible");
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
		if (googleVisionService == null || !googleVisionService.isEnabled()) {
			throw new OcrUnavailableException("Google Vision API n'est pas activé");
		}
		try {
			return googleVisionService.extractPharmaciesFromImage(fileBytes);
		} catch (IllegalStateException ex) {
			throw new OcrUnavailableException("Moteur OCR indisponible");
		} catch (Exception ex) {
			logger.warn("OCR impossible pour {}", fileName);
			throw new UnreadableDocumentException("Fichier illisible");
		}
	}
}
