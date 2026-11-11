package com.buddies.pharmaciegarde.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Numéro ivoirien du plan à 10 chiffres (ARTCI, depuis 2021).
 * Mobiles 01, 05, 07 et fixes 21, 25, 27. L'indicatif 225, 00225 ou +225 est accepté.
 */
@Documented
@Constraint(validatedBy = IvorianPhoneValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface IvorianPhone {

	String message() default "Le numéro de téléphone doit être un numéro ivoirien valide";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
