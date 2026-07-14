package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informations d'une pharmacie extraite du fichier Excel")
public class ExcelPharmacyDto {

	@Schema(description = "Zone", example = "Zone Nord")
	private String zone;

	@Schema(description = "Nom de la pharmacie", example = "PHCIE CENTRALE")
	private String pharmacyName;

	@Schema(description = "Nom du pharmacien", example = "Dr. Jean Dupont")
	private String pharmacistName;

	@Schema(description = "Contacts (téléphones)", example = "0341234567, 0321234567")
	private String contacts;

	@Schema(description = "Indication", example = "Pharmacie de garde 24h/24")
	private String indication;

	@Schema(description = "ID de la pharmacie créée ou mise à jour", example = "1")
	private Long pharmacyId;

	@Schema(description = "ID de la garde créée", example = "1")
	private Long dutyId;

	@Schema(description = "Statut du traitement", example = "CREATED", allowableValues = {"CREATED", "UPDATED", "ERROR"})
	private String status;

	@Schema(description = "Message d'erreur si échec")
	private String errorMessage;

	// Constructeurs
	public ExcelPharmacyDto() {
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

	public String getContacts() {
		return contacts;
	}

	public void setContacts(String contacts) {
		this.contacts = contacts;
	}

	public String getIndication() {
		return indication;
	}

	public void setIndication(String indication) {
		this.indication = indication;
	}

	public Long getPharmacyId() {
		return pharmacyId;
	}

	public void setPharmacyId(Long pharmacyId) {
		this.pharmacyId = pharmacyId;
	}

	public Long getDutyId() {
		return dutyId;
	}

	public void setDutyId(Long dutyId) {
		this.dutyId = dutyId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
