package com.buddies.pharmaciegarde.modules.duty.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "duty_files")
public class DutyFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "file_path")
	private String filePath;

	@Column(name = "file_size")
	private Long fileSize;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ProcessingStatus status;

	@Column(name = "upload_date", nullable = false, updatable = false)
	private LocalDateTime uploadDate;

	@Column(name = "processed_date")
	private LocalDateTime processedDate;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "pharmacies_count")
	private Integer pharmaciesCount; // Nombre de pharmacies extraites du PDF

	@Column(name = "description", columnDefinition = "TEXT")
	private String description; // Description optionnelle du fichier

	@Column(name = "content_hash", length = 64)
	private String contentHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "duty_period_id")
	private DutyPeriod dutyPeriod; // Période de garde à laquelle ce fichier est lié

	// Constructeurs
	public DutyFile() {
		this.uploadDate = LocalDateTime.now();
		this.status = ProcessingStatus.PENDING;
	}

	public DutyFile(String fileName) {
		this();
		this.fileName = fileName;
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

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
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

	public DutyPeriod getDutyPeriod() {
		return dutyPeriod;
	}

	public void setDutyPeriod(DutyPeriod dutyPeriod) {
		this.dutyPeriod = dutyPeriod;
	}

	public String getContentHash() {
		return contentHash;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}

	// Enum pour le statut de traitement
	public enum ProcessingStatus {
		PENDING, PROCESSING, COMPLETED, FAILED
	}
}

