package com.buddies.pharmaciegarde.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.swagger.v3.oas.annotations.Hidden;

@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
		Map<String, String> errorResponse = new HashMap<>();
		errorResponse.put("message", ex.getMessage());
		errorResponse.put("status", "NOT_FOUND");
		return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
		Map<String, String> errorResponse = new HashMap<>();
		errorResponse.put("message", ex.getMessage());
		errorResponse.put("status", "BAD_REQUEST");
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, Object> errorResponse = new HashMap<>();
		Map<String, String> fieldErrors = new HashMap<>();
		
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			fieldErrors.put(fieldName, errorMessage);
		});
		
		errorResponse.put("message", "Erreurs de validation");
		errorResponse.put("errors", fieldErrors);
		errorResponse.put("status", "BAD_REQUEST");
		
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
		Map<String, String> errorResponse = new HashMap<>();
		errorResponse.put("message", "Une erreur interne est survenue: " + ex.getMessage());
		errorResponse.put("status", "INTERNAL_SERVER_ERROR");
		ex.printStackTrace(); // Pour le débogage
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

