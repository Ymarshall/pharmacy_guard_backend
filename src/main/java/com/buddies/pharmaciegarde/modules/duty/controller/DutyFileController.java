package com.buddies.pharmaciegarde.modules.duty.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buddies.pharmaciegarde.modules.duty.dto.DutyFileResponseDto;
import com.buddies.pharmaciegarde.modules.duty.dto.PdfProcessingResultDto;
import com.buddies.pharmaciegarde.modules.duty.dto.TextExtractionResultDto;
import com.buddies.pharmaciegarde.modules.duty.service.DutyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/duty-files")
@Tag(name = "Duty Files", description = "API de gestion des fichiers PDF de garde")
public class DutyFileController {

	private final DutyService dutyService;

	public DutyFileController(DutyService dutyService) {
		this.dutyService = dutyService;
	}

	@Operation(
			summary = "Uploader un ou plusieurs fichiers PDF ou JPEG", 
			description = "Upload un ou plusieurs fichiers PDF ou images JPEG contenant la liste des pharmacies de garde. " +
					"Utilise multipart/form-data. Les fichiers doivent être des PDF ou JPEG valides (max 10MB chacun). " +
					"Les images JPEG nécessiteront OCR (Google Vision) pour l'extraction. " +
					"Vous pouvez uploader un seul fichier avec le paramètre 'file' ou plusieurs fichiers avec le paramètre 'files'.",
			requestBody = @RequestBody(
					content = @Content(
							mediaType = "multipart/form-data",
							encoding = @Encoding(name = "file(s)", contentType = "application/pdf, image/jpeg")
					)
			)
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Fichier(s) uploadé(s) avec succès",
					content = @Content(schema = @Schema(implementation = DutyFileResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Fichier(s) invalide(s) (vide, format incorrect, ou taille > 10MB)")
	})
	@PostMapping(value = "/upload", consumes = "multipart/form-data")
	public ResponseEntity<?> uploadFiles(
			@Parameter(
					description = "Un seul fichier PDF ou JPEG à uploader (utiliser 'file' pour un seul fichier)", 
					required = false
			) 
			@RequestParam(value = "file", required = false) MultipartFile file,
			@Parameter(
					description = "Plusieurs fichiers PDF ou JPEG à uploader (utiliser 'files' pour plusieurs fichiers)", 
					required = false
			) 
			@RequestParam(value = "files", required = false) MultipartFile[] files,
			@Parameter(description = "Description optionnelle des fichiers") 
			@RequestParam(value = "description", required = false) String description,
			@Parameter(description = "ID de la période de garde à laquelle lier les fichiers", required = true) 
			@RequestParam(value = "dutyPeriodId", required = true) Long dutyPeriodId) {
		
		// Vérifier qu'au moins un fichier ou plusieurs fichiers sont fournis
		if ((file == null || file.isEmpty()) && (files == null || files.length == 0)) {
			throw new IllegalArgumentException("Au moins un fichier doit être fourni (paramètre 'file' ou 'files')");
		}
		
		// Si plusieurs fichiers sont fournis via 'files', utiliser cette méthode (priorité)
		if (files != null && files.length > 0) {
			// Vérifier que tous les fichiers ne sont pas vides
			boolean hasValidFile = false;
			for (MultipartFile f : files) {
				if (f != null && !f.isEmpty()) {
					hasValidFile = true;
					break;
				}
			}
			if (!hasValidFile) {
				throw new IllegalArgumentException("Tous les fichiers fournis sont vides");
			}
			List<DutyFileResponseDto> uploadedFiles = dutyService.uploadPdfFiles(files, description, dutyPeriodId);
			return new ResponseEntity<>(uploadedFiles, HttpStatus.CREATED);
		}
		
		// Si un seul fichier est fourni via 'file'
		if (file != null && !file.isEmpty()) {
			DutyFileResponseDto uploadedFile = dutyService.uploadPdfFile(file, description, dutyPeriodId);
			return new ResponseEntity<>(uploadedFile, HttpStatus.CREATED);
		}
		
		throw new IllegalArgumentException("Aucun fichier valide fourni");
	}

	@Operation(summary = "Traiter un fichier PDF", 
			description = "Extrait les pharmacies du PDF et crée les gardes pour les pharmacies trouvées")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Fichier traité avec succès",
					content = @Content(schema = @Schema(implementation = PdfProcessingResultDto.class))),
			@ApiResponse(responseCode = "400", description = "Erreur lors du traitement"),
			@ApiResponse(responseCode = "404", description = "Fichier non trouvé")
	})
	@PostMapping("/{id}/process")
	public ResponseEntity<PdfProcessingResultDto> processFile(
			@Parameter(description = "ID du fichier à traiter", required = true) @PathVariable Long id) {
		PdfProcessingResultDto result = dutyService.processPdfFile(id);
		return ResponseEntity.ok(result);
	}

	@Operation(summary = "Extraire le texte brut d'un fichier (sans IA)", 
			description = "Extrait le texte brut d'un fichier PDF et le sauvegarde dans un fichier txt. " +
					"Cette méthode n'utilise pas d'IA/OCR, uniquement l'extraction directe du texte. " +
					"Fonctionne uniquement pour les PDF avec texte extractible.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Texte extrait avec succès",
					content = @Content(schema = @Schema(implementation = TextExtractionResultDto.class))),
			@ApiResponse(responseCode = "400", description = "Erreur lors de l'extraction (fichier image, PDF scanné, etc.)"),
			@ApiResponse(responseCode = "404", description = "Fichier non trouvé")
	})
	@PostMapping("/{id}/extract-text")
	public ResponseEntity<TextExtractionResultDto> extractTextOnly(
			@Parameter(description = "ID du fichier à traiter", required = true) @PathVariable Long id) {
		TextExtractionResultDto result = dutyService.extractTextOnly(id);
		return ResponseEntity.ok(result);
	}

	@Operation(summary = "Lister tous les fichiers uploadés", description = "Retourne la liste de tous les fichiers PDF uploadés")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des fichiers récupérée avec succès")
	})
	@GetMapping
	public ResponseEntity<List<DutyFileResponseDto>> getAllFiles() {
		return ResponseEntity.ok(dutyService.getAllDutyFiles());
	}

	@Operation(summary = "Récupérer les fichiers d'une période de garde", 
			description = "Retourne tous les fichiers uploadés pour une période de garde spécifique, " +
					"triés par date d'upload (plus récents en premier)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Liste des fichiers récupérée avec succès",
					content = @Content(schema = @Schema(implementation = DutyFileResponseDto.class))),
			@ApiResponse(responseCode = "404", description = "Période de garde non trouvée")
	})
	@GetMapping("/period/{dutyPeriodId}")
	public ResponseEntity<List<DutyFileResponseDto>> getFilesByPeriodId(
			@Parameter(description = "ID de la période de garde", required = true) @PathVariable Long dutyPeriodId) {
		return ResponseEntity.ok(dutyService.getDutyFilesByPeriodId(dutyPeriodId));
	}

	@Operation(summary = "Supprimer un fichier", 
			description = "Supprime un fichier PDF et son entrée en base de données. " +
					"Si des gardes sont associées à ce fichier, elles seront également supprimées.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Fichier supprimé avec succès"),
			@ApiResponse(responseCode = "404", description = "Fichier non trouvé")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFile(
			@Parameter(description = "ID du fichier à supprimer", required = true) @PathVariable Long id) {
		dutyService.deleteDutyFile(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "Uploader et traiter un fichier Excel de pharmacies de garde", 
			description = "Upload un fichier Excel (.xlsx ou .xls) contenant la liste des pharmacies de garde. " +
					"Le fichier doit avoir les colonnes suivantes (première ligne = en-têtes, à partir de la 2ème ligne = données): " +
					"Colonne 1: Zone, Colonne 2: Nom de la pharmacie, Colonne 3: Nom du pharmacien, " +
					"Colonne 4: Contacts, Colonne 5: Indication. " +
					"Les pharmacies seront créées ou mises à jour, et les gardes seront associées à la période spécifiée.",
			requestBody = @RequestBody(
					content = @Content(
							mediaType = "multipart/form-data",
							encoding = @Encoding(name = "file", contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/vnd.ms-excel")
					)
			)
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Fichier Excel traité avec succès",
					content = @Content(schema = @Schema(implementation = com.buddies.pharmaciegarde.modules.duty.dto.ExcelProcessingResultDto.class))),
			@ApiResponse(responseCode = "400", description = "Fichier invalide (vide, format incorrect, ou taille > 10MB)"),
			@ApiResponse(responseCode = "404", description = "Période de garde non trouvée")
	})
	@PostMapping(value = "/upload-excel", consumes = "multipart/form-data")
	public ResponseEntity<com.buddies.pharmaciegarde.modules.duty.dto.ExcelProcessingResultDto> uploadExcelFile(
			@Parameter(
					description = "Fichier Excel (.xlsx ou .xls) à uploader et traiter", 
					required = true
			) 
			@RequestParam(value = "file", required = true) org.springframework.web.multipart.MultipartFile file,
			@Parameter(description = "ID de la période de garde à laquelle lier les pharmacies", required = true) 
			@RequestParam(value = "dutyPeriodId", required = true) Long dutyPeriodId) {
		
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Le fichier Excel est requis");
		}
		
		com.buddies.pharmaciegarde.modules.duty.dto.ExcelProcessingResultDto result = 
				dutyService.processExcelFile(file, dutyPeriodId);
		
		return ResponseEntity.ok(result);
	}
}

