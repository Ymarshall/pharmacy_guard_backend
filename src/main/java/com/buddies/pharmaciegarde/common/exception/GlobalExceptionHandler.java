package com.buddies.pharmaciegarde.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
				ApiError.of(404, "NOT_FOUND", "Introuvable", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest().body(
				ApiError.of(400, "BAD_REQUEST", "Requête invalide", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
				ApiError.of(403, "FORBIDDEN", "Accès refusé", "Accès refusé", request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
			fieldErrors.put(fieldName, error.getDefaultMessage());
		});
		return ResponseEntity.badRequest().body(ApiError.of(400, "BAD_REQUEST", "Erreurs de validation",
				"Erreurs de validation", request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getConstraintViolations().forEach(violation -> fieldErrors.put(
				violation.getPropertyPath().toString(), violation.getMessage()));
		return ResponseEntity.badRequest().body(ApiError.of(400, "BAD_REQUEST", "Erreurs de validation",
				"Erreurs de validation", request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		logger.debug("Corps de requête illisible sur {}", request.getRequestURI());
		return ResponseEntity.badRequest().body(ApiError.of(400, "BAD_REQUEST", "Requête invalide",
				"Le corps de la requête est invalide", request.getRequestURI()));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiError.of(413, "PAYLOAD_TOO_LARGE",
				"Fichier trop volumineux", "Le fichier dépasse la taille maximale autorisée (10 Mo)",
				request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
		logger.error("Erreur interne sur {}", request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(500,
				"INTERNAL_SERVER_ERROR", "Erreur interne", "Une erreur interne est survenue",
				request.getRequestURI()));
	}
}
