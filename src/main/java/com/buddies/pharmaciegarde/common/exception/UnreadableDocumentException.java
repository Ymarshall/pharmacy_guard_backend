package com.buddies.pharmaciegarde.common.exception;

/**
 * Le fichier de garde ne peut pas être lu (absent, vide ou corrompu).
 */
public class UnreadableDocumentException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnreadableDocumentException(String message) {
		super(message);
	}
}
