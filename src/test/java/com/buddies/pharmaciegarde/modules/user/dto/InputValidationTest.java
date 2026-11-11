package com.buddies.pharmaciegarde.modules.user.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.buddies.pharmaciegarde.common.validation.IvorianPhoneValidator;
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
	void acceptsIvorianMobileAndLandlineAndRejectsForeignNumbers() {
		assertTrue(validator.validate(new ClientRegisterDto("+225 07 12 34 56 78")).isEmpty());
		assertTrue(validator.validate(new ClientRegisterDto("0712345678")).isEmpty());
		assertTrue(validator.validate(new ClientRegisterDto("+2250712345678")).isEmpty());
		assertTrue(validator.validate(new ClientRegisterDto("00225 05 12 34 56 78")).isEmpty());
		assertTrue(validator.validate(new ClientRegisterDto("21 20 30 40 50")).isEmpty());
		assertEquals("0748102030", IvorianPhoneValidator.nationalNumber("+225 07 48 10 20 30"));
		assertEquals("0748102030", IvorianPhoneValidator.nationalNumber("07-48-10-20-30"));
		assertFalse(validator.validate(new ClientRegisterDto("+33 6 12 34 56 78")).isEmpty());
		assertFalse(validator.validate(new ClientRegisterDto("0612345678")).isEmpty());
		assertFalse(validator.validate(new ClientRegisterDto("07123456")).isEmpty());
		assertFalse(validator.validate(new ClientRegisterDto("   ")).isEmpty());
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
