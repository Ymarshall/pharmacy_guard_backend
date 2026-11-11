package com.buddies.pharmaciegarde.modules.duty.dto;

import com.buddies.pharmaciegarde.modules.duty.matching.PharmacyNameNormalizer;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidImportLineValidator implements ConstraintValidator<ValidImportLine, DutyImportLineDecision> {

	private static final java.util.regex.Pattern COORDINATES = java.util.regex.Pattern
			.compile("^-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?$");

	@Override
	public boolean isValid(DutyImportLineDecision line, ConstraintValidatorContext context) {
		if (line == null || line.skip() || !line.createPharmacy() || line.pharmacyId() != null) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		boolean valid = true;
		if (blank(line.city()) && blank(line.zone())) {
			violation(context, "city", "La ville ou la zone est obligatoire pour créer une pharmacie");
			valid = false;
		}
		if (blank(line.commune()) && blank(line.zone())) {
			violation(context, "commune", "La commune ou la zone est obligatoire pour créer une pharmacie");
			valid = false;
		}
		String phone = first(line.phone(), line.phoneNumbers());
		if (PharmacyNameNormalizer.phoneKey(phone).length() < 8) {
			violation(context, "phone", "Un numéro de téléphone réel d'au moins 8 chiffres est obligatoire");
			valid = false;
		}
		if (!blank(line.coordinates()) && !COORDINATES.matcher(line.coordinates().trim()).matches()) {
			violation(context, "coordinates", "La localisation doit être au format latitude,longitude");
			valid = false;
		}
		return valid;
	}

	private static void violation(ConstraintValidatorContext context, String field, String message) {
		context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	private static String first(String preferred, String fallback) {
		return !blank(preferred) ? preferred : fallback;
	}
}
