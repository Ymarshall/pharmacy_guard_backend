package com.buddies.pharmaciegarde.modules.duty.model;

public enum DutyStatus {
	ACTIVE,      // Statut actif par défaut
	TO_VERIFY,   // En attente de vérification (après signalement)
	DISABLED     // Désactivée (par le back office)
}
