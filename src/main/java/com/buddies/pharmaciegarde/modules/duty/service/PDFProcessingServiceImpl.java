package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;

@Service
public class PDFProcessingServiceImpl implements PDFProcessingService {

	@Override
	public List<ExtractedPharmacyDto> extractPharmaciesFromPdf(InputStream inputStream) throws IOException {
		List<ExtractedPharmacyDto> pharmacies = new ArrayList<>();

		try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);

			// Pattern pour extraire les noms de pharmacies
			// Ce pattern peut être ajusté selon le format de votre PDF
			Pattern pharmacyPattern = Pattern.compile(
					"(?i)(?:pharmacie|pharma)\\s+([A-ZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ][a-zàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ\\s-]+)",
					Pattern.MULTILINE);

			Matcher matcher = pharmacyPattern.matcher(text);
			while (matcher.find()) {
				String pharmacyName = matcher.group(1).trim();
				
				// Nettoyer le nom (enlever les caractères indésirables)
				pharmacyName = pharmacyName.replaceAll("[^a-zA-ZÀ-ÿ\\s-]", "").trim();
				
				if (!pharmacyName.isEmpty() && pharmacyName.length() > 2) {
					ExtractedPharmacyDto pharmacyInfo = new ExtractedPharmacyDto();
					pharmacyInfo.setPharmacyName(pharmacyName);
					pharmacyInfo.setConfidence(0.8);
					pharmacies.add(pharmacyInfo);
				}
			}

			// Si aucun pattern ne correspond, essayer d'extraire toutes les lignes qui contiennent "pharmacie"
			if (pharmacies.isEmpty()) {
				String[] lines = text.split("\\r?\\n");
				for (String line : lines) {
					if (line.toLowerCase().contains("pharmacie") || line.toLowerCase().contains("pharma")) {
						String cleaned = line.replaceAll("(?i)pharmacie|pharma", "").trim();
						if (!cleaned.isEmpty() && cleaned.length() > 2) {
							ExtractedPharmacyDto pharmacyInfo = new ExtractedPharmacyDto();
							pharmacyInfo.setPharmacyName(cleaned);
							pharmacyInfo.setConfidence(0.8);
							pharmacies.add(pharmacyInfo);
						}
					}
				}
			}
		}

		return pharmacies;
	}
}

