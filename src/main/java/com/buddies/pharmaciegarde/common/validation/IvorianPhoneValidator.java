package com.buddies.pharmaciegarde.common.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IvorianPhoneValidator implements ConstraintValidator<IvorianPhone, String> {

	/**
	 * Préfixes du plan national : 01 Moov, 05 MTN, 07 Orange (mobile),
	 * 21 Moov, 25 MTN, 27 Orange (fixe).
	 */
	private static final Pattern COMPACT = Pattern.compile(
			"^(?:\\+225|00225|225)?((?:01|05|07|21|25|27)\\d{8})$");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}
		return nationalNumber(value) != null;
	}

	/**
	 * @return les 10 chiffres nationaux, ou {@code null} si la valeur n'est pas un numéro ivoirien
	 */
	public static String nationalNumber(String raw) {
		if (raw == null) {
			return null;
		}
		String compact = raw.trim().replaceAll("[\\s.\\-()/]", "");
		Matcher matcher = COMPACT.matcher(compact);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);
	}
}
