package com.buddies.pharmaciegarde.modules.duty.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyJobResponse;
import com.buddies.pharmaciegarde.modules.duty.dto.PdfProcessingResultDto;

@Service
public class DutyProcessingJobService {

	private static final Logger logger = LoggerFactory.getLogger(DutyProcessingJobService.class);

	private final DutyService dutyService;
	private final ApplicationContext applicationContext;
	private final Map<String, DutyJobResponse> jobs = new ConcurrentHashMap<>();

	public DutyProcessingJobService(DutyService dutyService, ApplicationContext applicationContext) {
		this.dutyService = dutyService;
		this.applicationContext = applicationContext;
	}

	public DutyJobResponse enqueue(Long dutyFileId) {
		String jobId = UUID.randomUUID().toString();
		DutyJobResponse pending = new DutyJobResponse(jobId, "PENDING", "Traitement en file", null);
		jobs.put(jobId, pending);
		applicationContext.getBean(DutyProcessingJobService.class).execute(jobId, dutyFileId);
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
		jobs.put(jobId, new DutyJobResponse(jobId, "RUNNING", "Extraction en cours", null));
		try {
			PdfProcessingResultDto result = dutyService.processPdfFile(dutyFileId, false);
			jobs.put(jobId, new DutyJobResponse(jobId, "COMPLETED", "Traitement terminé", result));
		} catch (RuntimeException ex) {
			logger.error("Échec du traitement asynchrone {}", jobId, ex);
			String message = ex instanceof IllegalArgumentException ? ex.getMessage() : "Le traitement a échoué";
			jobs.put(jobId, new DutyJobResponse(jobId, "FAILED", message, null));
		}
	}
}
