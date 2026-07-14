package com.buddies.pharmaciegarde.common.exception;

public class ResourceNotFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public ResourceNotFoundException(String message) {
		super(message);
	}
	
	public ResourceNotFoundException(String resourceName, Long id) {
		super(String.format("%s avec l'id %d n'a pas été trouvé", resourceName, id));
	}
}

