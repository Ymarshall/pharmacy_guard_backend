package com.buddies.pharmaciegarde.modules.duty.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de l'extraction de texte brut")
public class TextExtractionResultDto {

	@Schema(description = "ID du fichier traité", example = "1")
	private Long dutyFileId;

	@Schema(description = "Statut de l'extraction", example = "COMPLETED")
	private String status;

	@Schema(description = "Chemin du fichier texte créé", example = "uploads/extractions/garde-semaine-01_20240115_143022_1.txt")
	private String textFilePath;

	@Schema(description = "Nombre de caractères extraits", example = "15234")
	private Integer characterCount;

	@Schema(description = "Nombre de lignes extraites", example = "456")
	private Integer lineCount;

	@Schema(description = "Message d'erreur si échec")
	private String errorMessage;

	// Constructeurs
	public TextExtractionResultDto() {
	}

	// Getters et Setters
	public Long getDutyFileId() {
		return dutyFileId;
	}

	public void setDutyFileId(Long dutyFileId) {
		this.dutyFileId = dutyFileId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTextFilePath() {
		return textFilePath;
	}

	public void setTextFilePath(String textFilePath) {
		this.textFilePath = textFilePath;
	}

	public Integer getCharacterCount() {
		return characterCount;
	}

	public void setCharacterCount(Integer characterCount) {
		this.characterCount = characterCount;
	}

	public Integer getLineCount() {
		return lineCount;
	}

	public void setLineCount(Integer lineCount) {
		this.lineCount = lineCount;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}

