package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.InputStream;
import java.util.List;

import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;

/**
 * Service pour utiliser Google Vision API pour l'extraction OCR
 * Nécessite une clé API Google Cloud Platform
 */
public interface GoogleVisionService {

	/**
	 * Extrait le texte d'une image en utilisant Google Vision API
	 * @param imageBytes Les bytes de l'image
	 * @return Le texte extrait
	 */
	String extractTextFromImage(byte[] imageBytes) throws Exception;

	/**
	 * Extrait les pharmacies d'une image en utilisant Google Vision API
	 * @param imageBytes Les bytes de l'image
	 * @return Liste des pharmacies extraites
	 */
	List<ExtractedPharmacyDto> extractPharmaciesFromImage(byte[] imageBytes) throws Exception;

	/**
	 * @return true si Google Vision est activé dans la configuration
	 */
	boolean isEnabled();
}

