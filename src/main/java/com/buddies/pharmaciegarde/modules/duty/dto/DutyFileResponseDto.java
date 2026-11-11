package com.buddies.pharmaciegarde.modules.duty.dto;

import java.time.LocalDateTime;

import com.buddies.pharmaciegarde.modules.duty.model.DutyFile.ProcessingStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les informations d'un fichier de garde")
public class DutyFileResponseDto {

	@Schema(description = "Identifiant unique du fichier", example = "1")
	private Long id;

	@Schema(description = "Nom du fichier", example = "garde-semaine-01.pdf")
	private String fileName;

	@Schema(description = "Taille du fichier en octets", example = "1024000")
	private Long fileSize;

	@Schema(description = "Statut de traitement", example = "COMPLETED")
	private ProcessingStatus status;

	@Schema(description = "Date d'upload")
	private LocalDateTime uploadDate;

	@Schema(description = "Date de traitement")
	private LocalDateTime processedDate;

	@Schema(description = "Message d'erreur si échec")
	private String errorMessage;

	@Schema(description = "Nombre de pharmacies extraites", example = "15")
	private Integer pharmaciesCount;

	@Schema(description = "Description du fichier")
	private String description;

	@Schema(description = "ID de la période de garde liée", example = "1")
	private Long dutyPeriodId;

	@Schema(description = "Empreinte SHA-256 du contenu, pour l'idempotence")
	private String contentHash;

	// Constructeurs
	public DutyFileResponseDto() {
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public ProcessingStatus getStatus() {
		return status;
	}

	public void setStatus(ProcessingStatus status) {
		this.status = status;
	}

	public LocalDateTime getUploadDate() {
		return uploadDate;
	}

	public void setUploadDate(LocalDateTime uploadDate) {
		this.uploadDate = uploadDate;
	}

	public LocalDateTime getProcessedDate() {
		return processedDate;
	}

	public void setProcessedDate(LocalDateTime processedDate) {
		this.processedDate = processedDate;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Integer getPharmaciesCount() {
		return pharmaciesCount;
	}

	public void setPharmaciesCount(Integer pharmaciesCount) {
		this.pharmaciesCount = pharmaciesCount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getDutyPeriodId() {
		return dutyPeriodId;
	}

	public void setDutyPeriodId(Long dutyPeriodId) {
		this.dutyPeriodId = dutyPeriodId;
	}

	public String getContentHash() {
		return contentHash;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}
}

