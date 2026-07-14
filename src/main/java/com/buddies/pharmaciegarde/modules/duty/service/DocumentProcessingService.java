package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.InputStream;
import java.util.List;

import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;

public interface DocumentProcessingService {

	/**
	 * Extrait les pharmacies d'un document (PDF texte, PDF scanné, ou image)
	 * @param inputStream Le flux d'entrée du document
	 * @param fileName Le nom du fichier pour déterminer le type
	 * @return Liste des pharmacies extraites avec toutes leurs informations
	 */
	List<ExtractedPharmacyDto> extractPharmacies(InputStream inputStream, String fileName) throws Exception;

	/**
	 * Détecte si le document est une image ou un PDF scanné nécessitant OCR
	 * @param inputStream Le flux d'entrée du document
	 * @param fileName Le nom du fichier
	 * @return true si OCR est nécessaire
	 */
	boolean requiresOCR(InputStream inputStream, String fileName) throws Exception;
}

