package com.buddies.pharmaciegarde.modules.duty.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.buddies.pharmaciegarde.common.exception.OcrUnavailableException;
import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.common.exception.UnreadableDocumentException;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyImportPreviewResponse;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyJobResponse;
import com.buddies.pharmaciegarde.modules.duty.dto.PdfProcessingResultDto;

@Service
public class DutyProcessingJobService {

	private static final Logger logger = LoggerFactory.getLogger(DutyProcessingJobService.class);

	private final DutyService dutyService;
	private final DutyImportService dutyImportService;
	private final ApplicationContext applicationContext;
	private final Map<String, DutyJobResponse> jobs = new ConcurrentHashMap<>();

	public DutyProcessingJobService(DutyService dutyService, DutyImportService dutyImportService,
			ApplicationContext applicationContext) {
		this.dutyService = dutyService;
		this.dutyImportService = dutyImportService;
		this.applicationContext = applicationContext;
	}

	public DutyJobResponse enqueue(Long dutyFileId) {
		dutyImportService.requireExisting(dutyFileId);
		String jobId = UUID.randomUUID().toString();
		DutyJobResponse pending = new DutyJobResponse(jobId, "PENDING", "Traitement en file", "PROCESS", null, null);
		jobs.put(jobId, pending);
		applicationContext.getBean(DutyProcessingJobService.class).execute(jobId, dutyFileId);
		return pending;
	}

	public DutyJobResponse enqueuePreview(Long dutyFileId) {
		dutyImportService.requireExisting(dutyFileId);
		String jobId = UUID.randomUUID().toString();
		DutyJobResponse pending = new DutyJobResponse(jobId, "PENDING", "Prévisualisation en file", "PREVIEW", null, null);
		jobs.put(jobId, pending);
		applicationContext.getBean(DutyProcessingJobService.class).executePreview(jobId, dutyFileId);
		return pending;
	}

	public DutyJobResponse get(String jobId) {
		DutyJobResponse job = jobs.get(jobId);
		if (job == null) {
			throw new ResourceNotFoundException("Traitement introuvable");
		}
		return job;
	}

	@Async("dutyExecutor")
	public void execute(String jobId, Long dutyFileId) {
		jobs.put(jobId, new DutyJobResponse(jobId, "RUNNING", "Extraction en cours", "PROCESS", null, null));
		try {
			PdfProcessingResultDto result = dutyService.processPdfFile(dutyFileId, false);
			jobs.put(jobId, new DutyJobResponse(jobId, "COMPLETED", "Traitement terminé", "PROCESS", result, null));
		} catch (RuntimeException ex) {
			logger.error("Échec du traitement asynchrone {}", jobId, ex);
			jobs.put(jobId, new DutyJobResponse(jobId, "FAILED", failureMessage(ex), "PROCESS", null, null));
		}
	}

	@Async("dutyExecutor")
	public void executePreview(String jobId, Long dutyFileId) {
		jobs.put(jobId, new DutyJobResponse(jobId, "RUNNING", "Prévisualisation en cours", "PREVIEW", null, null));
		try {
			DutyImportPreviewResponse preview = dutyImportService.preview(dutyFileId);
			jobs.put(jobId, new DutyJobResponse(jobId, "COMPLETED", "Prévisualisation terminée", "PREVIEW", null, preview));
		} catch (RuntimeException ex) {
			logger.error("Échec de la prévisualisation asynchrone {}", jobId, ex);
			jobs.put(jobId, new DutyJobResponse(jobId, "FAILED", failureMessage(ex), "PREVIEW", null, null));
		}
	}

	private String failureMessage(RuntimeException ex) {
		if (ex instanceof OcrUnavailableException || ex instanceof UnreadableDocumentException
				|| ex instanceof IllegalArgumentException) {
			return ex.getMessage();
		}
		return "Le traitement a échoué";
	}
}
