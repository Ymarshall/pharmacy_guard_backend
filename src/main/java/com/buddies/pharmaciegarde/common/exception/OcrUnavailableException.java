package com.buddies.pharmaciegarde.common.exception;

/**
 * Google Vision est désactivé ou le moteur OCR ne peut pas répondre.
 */
public class OcrUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OcrUnavailableException(String message) {
		super(message);
	}
}
