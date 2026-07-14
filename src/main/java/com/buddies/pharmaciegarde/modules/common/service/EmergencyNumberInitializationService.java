package com.buddies.pharmaciegarde.modules.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.modules.common.model.EmergencyNumber;
import com.buddies.pharmaciegarde.modules.common.model.EmergencyNumber.EmergencyCategory;
import com.buddies.pharmaciegarde.modules.common.repository.EmergencyNumberRepository;

@Component
public class EmergencyNumberInitializationService implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(EmergencyNumberInitializationService.class);

	private final EmergencyNumberRepository emergencyNumberRepository;

	public EmergencyNumberInitializationService(EmergencyNumberRepository emergencyNumberRepository) {
		this.emergencyNumberRepository = emergencyNumberRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		logger.info("Initialisation des numéros d'urgence de Côte d'Ivoire...");
		initializeEmergencyNumbers();
		logger.info("Initialisation des numéros d'urgence terminée.");
	}

	private void initializeEmergencyNumbers() {
		// Numéros d'urgence nationaux (gratuits et standards)
		createIfNotExists("Police secours (110)", "110", "Icons.local_police", EmergencyCategory.police,
				"#FF0000", "Secours police généraliste pour vol, agression, troubles à l'ordre public");

		createIfNotExists("Police secours (111)", "111", "Icons.local_police", EmergencyCategory.police,
				"#FF0000", "Secours police généraliste pour vol, agression, troubles à l'ordre public");

		createIfNotExists("Police secours (170)", "170", "Icons.local_police", EmergencyCategory.police,
				"#FF0000", "Secours police généraliste pour vol, agression, troubles à l'ordre public");

		createIfNotExists("Pompiers (incendie & secours)", "180", "Icons.fire_extinguisher", EmergencyCategory.fire,
				"#FF4500", "Intervention incendie, accident, sauvetage");

		createIfNotExists("SAMU / Ambulance", "185", "Icons.local_hospital", EmergencyCategory.medical,
				"#00FF00", "Assistance médicale urgente");

		createIfNotExists("Défense nationale (sécurité)", "199", "Icons.security", EmergencyCategory.other,
				"#0000FF", "Service défense / urgence nationale");

		createIfNotExists("Urgence universelle (112)", "112", "Icons.phone_in_talk", EmergencyCategory.other,
				"#FFD700", "Numéro de secours universel pouvant être réorienté vers police/pompiers/ambulance");

		// Police et forces de l'ordre - Abidjan
		createIfNotExists("Direction Générale de la Police Nationale", "+225 27 20 22 20 30",
				"Icons.account_balance", EmergencyCategory.police, "#FF0000",
				"Direction Générale de la Police Nationale - Abidjan");

		createIfNotExists("Préfecture de police – Abidjan", "+225 27 20 21 00 22 27", "Icons.local_police",
				EmergencyCategory.police, "#FF0000", "Préfecture de police – Abidjan");

		createIfNotExists("Direction de la Police Judiciaire", "+225 27 20 21 23 00", "Icons.gavel",
				EmergencyCategory.police, "#FF0000", "Direction de la Police Judiciaire - Abidjan");

		createIfNotExists("Direction Police Économique", "+225 27 20 32 51 44", "Icons.business_center",
				EmergencyCategory.police, "#FF0000", "Direction Police Économique - Abidjan");

		// Pompiers – Abidjan
		createIfNotExists("Centre principal Pompiers", "+225 07 07 81 18 18", "Icons.fire_extinguisher",
				EmergencyCategory.fire, "#FF4500", "Centre principal Pompiers - Abidjan");

		createIfNotExists("Pompiers Indénié", "+225 27 20 21 12 89", "Icons.fire_extinguisher", EmergencyCategory.fire,
				"#FF4500", "Pompiers Indénié - Abidjan");

		createIfNotExists("Pompiers Yopougon", "+225 27 23 45 16 90", "Icons.fire_extinguisher", EmergencyCategory.fire,
				"#FF4500", "Pompiers Yopougon - Abidjan");

		createIfNotExists("Pompiers Zone 4", "+225 27 21 35 73 65", "Icons.fire_extinguisher", EmergencyCategory.fire,
				"#FF4500", "Pompiers Zone 4 - Abidjan");

		// Urgences médicales – Abidjan
		createIfNotExists("SAMU Ambulance (Abidjan)", "+225 27 27 22 44 53 53", "Icons.local_hospital",
				EmergencyCategory.medical, "#00FF00", "SAMU Ambulance - Abidjan");

		createIfNotExists("CHU Cocody", "+225 27 22 44 90 00", "Icons.local_hospital", EmergencyCategory.medical,
				"#00FF00", "Centre Hospitalier Universitaire Cocody - Abidjan");

		createIfNotExists("CHU Cocody (alternatif)", "+225 27 22 48 10 00", "Icons.local_hospital",
				EmergencyCategory.medical, "#00FF00", "Centre Hospitalier Universitaire Cocody - Abidjan");

		createIfNotExists("CHU Treichville", "+225 27 21 24 91 22", "Icons.local_hospital", EmergencyCategory.medical,
				"#00FF00", "Centre Hospitalier Universitaire Treichville - Abidjan");

		createIfNotExists("CHU Grande-Bassam", "+225 27 21 30 10 36", "Icons.local_hospital", EmergencyCategory.medical,
				"#00FF00", "Centre Hospitalier Universitaire Grande-Bassam");

		createIfNotExists("Croix-Rouge Côte d'Ivoire", "+225 27 20 32 13 35", "Icons.favorite",
				EmergencyCategory.medical, "#FF0000", "Croix-Rouge Côte d'Ivoire - Abidjan");

		createIfNotExists("Croix Bleue", "+225 27 20 37 00 53", "Icons.favorite", EmergencyCategory.medical,
				"#0000FF", "Croix Bleue - Abidjan");

		// Services essentiels (non urgence vitale mais utile)
		createIfNotExists("Électricité – CIE", "179", "Icons.flash_on", EmergencyCategory.other, "#FFD700",
				"Service client Électricité - CIE (Compagnie Ivoirienne d'Électricité)");

		createIfNotExists("Eau – SODECI", "175", "Icons.water", EmergencyCategory.other, "#00CED1",
				"Service client Eau - SODECI (Société de Distribution d'Eau de Côte d'Ivoire)");
	}

	private void createIfNotExists(String name, String number, String icon, EmergencyCategory category,
			String color, String description) {
		// Nettoyer le numéro (retirer les espaces pour la vérification d'existence)
		String cleanedNumber = number.replaceAll("\\s+", "");
		
		// Vérifier si le numéro existe déjà (avec ou sans espaces)
		boolean numberExists = emergencyNumberRepository.existsByNumber(number) 
				|| emergencyNumberRepository.existsByNumber(cleanedNumber);
		
		if (!emergencyNumberRepository.existsByName(name) && !numberExists) {
			EmergencyNumber emergencyNumber = new EmergencyNumber();
			emergencyNumber.setName(name);
			emergencyNumber.setNumber(number); // Garder le format original avec espaces si nécessaire
			emergencyNumber.setIcon(icon);
			emergencyNumber.setCategory(category);
			emergencyNumber.setColor(color);
			emergencyNumber.setDescription(description);

			emergencyNumberRepository.save(emergencyNumber);
			logger.info("Numéro d'urgence créé: {} - {}", name, number);
		} else {
			logger.debug("Numéro d'urgence déjà existant: {} - {}", name, number);
		}
	}
}

