package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informations complètes d'une pharmacie extraite du fichier")
public class ExtractedPharmacyDto {

	@Schema(description = "Zone/Ville de la pharmacie", example = "ABENGOUROU")
	private String zone;

	@Schema(description = "Nom de la pharmacie", example = "PHARMACIE DU MARCHE")
	private String pharmacyName;

	@Schema(description = "Nom du pharmacien", example = "MME SENIN DON MELLO A. CHRISTELLE")
	private String pharmacistName;

	@Schema(description = "Numéros de téléphone (peut contenir plusieurs numéros)", example = "27 35 91 30 09/01 02 03 87 17")
	private String phoneNumbers;

	@Schema(description = "Description/Localisation de la pharmacie", example = "PLACE DU GRAND MARCHE/ A COTE DE TELECEL")
	private String location;

	@Schema(description = "Commune/District (si disponible)")
	private String commune;

	@Schema(description = "ID de la pharmacie si trouvée dans la base", example = "1")
	private Long pharmacyId;

	@Schema(description = "Indique si la pharmacie existe déjà dans la base", example = "true")
	private Boolean existsInDatabase;

	@Schema(description = "Confiance de l'extraction (0.0 à 1.0)", example = "0.95")
	private Double confidence;

	// Constructeurs
	public ExtractedPharmacyDto() {
	}

	public ExtractedPharmacyDto(String zone, String pharmacyName, String pharmacistName, 
			String phoneNumbers, String location) {
		this.zone = zone;
		this.pharmacyName = pharmacyName;
		this.pharmacistName = pharmacistName;
		this.phoneNumbers = phoneNumbers;
		this.location = location;
	}

	// Getters et Setters
	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String getPharmacyName() {
		return pharmacyName;
	}

	public void setPharmacyName(String pharmacyName) {
		this.pharmacyName = pharmacyName;
	}

	public String getPharmacistName() {
		return pharmacistName;
	}

	public void setPharmacistName(String pharmacistName) {
		this.pharmacistName = pharmacistName;
	}

	public String getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(String phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getCommune() {
		return commune;
	}

	public void setCommune(String commune) {
		this.commune = commune;
	}

	public Long getPharmacyId() {
		return pharmacyId;
	}

	public void setPharmacyId(Long pharmacyId) {
		this.pharmacyId = pharmacyId;
	}

	public Boolean getExistsInDatabase() {
		return existsInDatabase;
	}

	public void setExistsInDatabase(Boolean existsInDatabase) {
		this.existsInDatabase = existsInDatabase;
	}

	public Double getConfidence() {
		return confidence;
	}

	public void setConfidence(Double confidence) {
		this.confidence = confidence;
	}
}

