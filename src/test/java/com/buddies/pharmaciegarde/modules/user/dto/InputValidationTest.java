package com.buddies.pharmaciegarde.modules.user.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.buddies.pharmaciegarde.modules.duty.dto.DutyImportLineDecision;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class InputValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void rejectsBlankAndOversizedPasswords() {
		assertFalse(validator.validate(new LoginDto("pas-un-email", "")).isEmpty());
		assertFalse(validator.validate(new LoginDto("yann@example.com", "x".repeat(80))).isEmpty());
		assertTrue(validator.validate(new LoginDto("yann@example.com", "secret123")).isEmpty());
	}

	@Test
	void rejectsBlankClientPhone() {
		ClientCreateDto dto = new ClientCreateDto();
		dto.setPhone("   ");
		dto.setLatitude(5.3);
		dto.setLongitude(-4.0);
		assertFalse(validator.validate(dto).isEmpty());
	}

	@Test
	void rejectsConfirmLineWithoutName() {
		DutyImportLineDecision line = new DutyImportLineDecision("  ", "ABIDJAN", null, null, null, null, null, null,
				null, null, null, false, false);
		assertFalse(validator.validate(line).isEmpty());
	}
}
