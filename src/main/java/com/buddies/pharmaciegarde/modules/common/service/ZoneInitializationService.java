package com.buddies.pharmaciegarde.modules.common.service;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.modules.common.model.Zone;
import com.buddies.pharmaciegarde.modules.common.repository.ZoneRepository;

/**
 * Service d'initialisation des zones (villes et communes de Côte d'Ivoire)
 * S'exécute au démarrage de l'application pour remplir la table zones
 */
@Service
@Order(1) // S'exécute en premier au démarrage
public class ZoneInitializationService implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ZoneInitializationService.class);

	@Autowired
	private ZoneRepository zoneRepository;

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		logger.info("Vérification et initialisation des zones de Côte d'Ivoire...");
		
		// Normaliser toutes les zones existantes
		normalizeExistingZones();
		
		long countBefore = zoneRepository.count();
		initializeZones();
		long countAfter = zoneRepository.count();
		long added = countAfter - countBefore;
		if (added > 0) {
			logger.info("Initialisation terminée. {} nouvelles zones créées. Total: {}", added, countAfter);
		} else {
			logger.info("Toutes les zones sont déjà présentes. Total: {}", countAfter);
		}
	}

	/**
	 * Normalise toutes les zones existantes dans la base de données
	 */
	private void normalizeExistingZones() {
		List<Zone> allZones = zoneRepository.findAll();
		int normalizedCount = 0;
		
		for (Zone zone : allZones) {
			String originalName = zone.getName();
			String normalizedName = normalizeZoneName(originalName);
			
			// Si le nom a changé après normalisation
			if (!originalName.equals(normalizedName)) {
				// Vérifier si une zone avec le nom normalisé existe déjà
				java.util.Optional<Zone> existingZone = zoneRepository.findByNameIgnoreCase(normalizedName);
				
				if (existingZone.isPresent() && !existingZone.get().getId().equals(zone.getId())) {
					// Une zone avec le nom normalisé existe déjà, supprimer la zone dupliquée
					logger.warn("Zone '{}' sera supprimée car une zone '{}' existe déjà", originalName, normalizedName);
					zoneRepository.delete(zone);
				} else {
					// Mettre à jour le nom normalisé
					zone.setName(normalizedName);
					zoneRepository.save(zone);
					normalizedCount++;
					logger.debug("Zone normalisée: '{}' -> '{}'", originalName, normalizedName);
				}
			}
		}
		
		if (normalizedCount > 0) {
			logger.info("{} zones existantes ont été normalisées (majuscules sans accents)", normalizedCount);
		}
	}

	private void initializeZones() {
		List<Zone> zones = Arrays.asList(
				// Région des Lagunes
				new Zone("Abidjan", "VILLE", "Lagunes", "Abidjan"),
				new Zone("Dabou", "VILLE", "Lagunes", "Dabou"),
				new Zone("Grand-Bassam", "VILLE", "Lagunes", "Grand-Bassam"),
				new Zone("Jacqueville", "VILLE", "Lagunes", "Jacqueville"),

				// Région du Sud-Comoé
				new Zone("Aboisso", "VILLE", "Sud-Comoé", "Aboisso"),
				new Zone("Adiaké", "VILLE", "Sud-Comoé", "Adiaké"),
				new Zone("Bonoua", "VILLE", "Sud-Comoé", "Bonoua"),
				new Zone("Tiapoum", "VILLE", "Sud-Comoé", "Tiapoum"),

				// Région de l'Indénié-Djuablin
				new Zone("Abengourou", "VILLE", "Indénié-Djuablin", "Abengourou"),
				new Zone("Agnibilékrou", "VILLE", "Indénié-Djuablin", "Agnibilékrou"),
				new Zone("Bettié", "VILLE", "Indénié-Djuablin", "Bettié"),

				// Région du Sud-Bandama
				new Zone("Divo", "VILLE", "Sud-Bandama", "Divo"),
				new Zone("Lakota", "VILLE", "Sud-Bandama", "Lakota"),
				new Zone("Guibéroua", "VILLE", "Sud-Bandama", "Guibéroua"),
				new Zone("Fresco", "VILLE", "Sud-Bandama", "Fresco"),

				// Région de la Nawa
				new Zone("Soubré", "VILLE", "Nawa", "Soubré"),
				new Zone("Buyo", "VILLE", "Nawa", "Buyo"),
				new Zone("Méagui", "VILLE", "Nawa", "Méagui"),

				// Région du Gbôklé
				new Zone("Sassandra", "VILLE", "Gbôklé", "Sassandra"),
				new Zone("San-Pédro", "VILLE", "Gbôklé", "San-Pédro"),

				// Région du Haut-Sassandra
				new Zone("Daloa", "VILLE", "Haut-Sassandra", "Daloa"),
				new Zone("Vavoua", "VILLE", "Haut-Sassandra", "Vavoua"),
				new Zone("Issia", "VILLE", "Haut-Sassandra", "Issia"),
				new Zone("Zoukougbeu", "VILLE", "Haut-Sassandra", "Zoukougbeu"),

				// Région de la Marahoué
				new Zone("Bouaflé", "VILLE", "Marahoué", "Bouaflé"),
				new Zone("Sinfra", "VILLE", "Marahoué", "Sinfra"),
				new Zone("Zuenoula", "VILLE", "Marahoué", "Zuenoula"),

				// Région du Worodougou
				new Zone("Séguéla", "VILLE", "Worodougou", "Séguéla"),
				new Zone("Mankono", "VILLE", "Worodougou", "Mankono"),
				new Zone("Kani", "VILLE", "Worodougou", "Kani"),

				// Région du Béré
				new Zone("Mankono", "VILLE", "Béré", "Mankono"),
				new Zone("Kounahiri", "VILLE", "Béré", "Kounahiri"),

				// Région du Cavally
				new Zone("Guiglo", "VILLE", "Cavally", "Guiglo"),
				new Zone("Toulepleu", "VILLE", "Cavally", "Toulepleu"),
				new Zone("Bloléquin", "VILLE", "Cavally", "Bloléquin"),

				// Région du Guémon
				new Zone("Duékoué", "VILLE", "Guémon", "Duékoué"),
				new Zone("Bangolo", "VILLE", "Guémon", "Bangolo"),
				new Zone("Kouibly", "VILLE", "Guémon", "Kouibly"),

				// Région du Tonkpi
				new Zone("Man", "VILLE", "Tonkpi", "Man"),
				new Zone("Danané", "VILLE", "Tonkpi", "Danané"),
				new Zone("Zouan-Hounien", "VILLE", "Tonkpi", "Zouan-Hounien"),

				// Région du Bafing
				new Zone("Touba", "VILLE", "Bafing", "Touba"),
				new Zone("Ouaninou", "VILLE", "Bafing", "Ouaninou"),

				// Région du Woroba
				new Zone("Mankono", "VILLE", "Woroba", "Mankono"),

				// Région du Bounkani
				new Zone("Bouna", "VILLE", "Bounkani", "Bouna"),
				new Zone("Nassian", "VILLE", "Bounkani", "Nassian"),
				new Zone("Téhini", "VILLE", "Bounkani", "Téhini"),

				// Région du Gontougo
				new Zone("Bondoukou", "VILLE", "Gontougo", "Bondoukou"),
				new Zone("Tanda", "VILLE", "Gontougo", "Tanda"),
				new Zone("Koun-Fao", "VILLE", "Gontougo", "Koun-Fao"),
				new Zone("Transua", "VILLE", "Gontougo", "Transua"),

				// Région de l'Iffou
				new Zone("Daoukro", "VILLE", "Iffou", "Daoukro"),
				new Zone("M'Bahiakro", "VILLE", "Iffou", "M'Bahiakro"),
				new Zone("Anoumaba", "VILLE", "Iffou", "Anoumaba"),

				// Région de la Mé
				new Zone("Adzopé", "VILLE", "Mé", "Adzopé"),
				new Zone("Akoupé", "VILLE", "Mé", "Akoupé"),
				new Zone("Alépé", "VILLE", "Mé", "Alépé"),

				// Région du Moronou
				new Zone("Bongouanou", "VILLE", "Moronou", "Bongouanou"),
				new Zone("Arrah", "VILLE", "Moronou", "Arrah"),
				new Zone("M'Batto", "VILLE", "Moronou", "M'Batto"),

				// Région de la N'Zi
				new Zone("Dimbokro", "VILLE", "N'Zi", "Dimbokro"),
				new Zone("Bocanda", "VILLE", "N'Zi", "Bocanda"),
				new Zone("Kouassi-Kouassikro", "VILLE", "N'Zi", "Kouassi-Kouassikro"),

				// Région du Gbêkê
				new Zone("Bouaké", "VILLE", "Gbêkê", "Bouaké"),
				new Zone("Sakassou", "VILLE", "Gbêkê", "Sakassou"),
				new Zone("Béoumi", "VILLE", "Gbêkê", "Béoumi"),
				new Zone("Botro", "VILLE", "Gbêkê", "Botro"),

				// Région du Hambol
				new Zone("Katiola", "VILLE", "Hambol", "Katiola"),
				new Zone("Niakaramandougou", "VILLE", "Hambol", "Niakaramandougou"),
				new Zone("Ferkessédougou", "VILLE", "Hambol", "Ferkessédougou"),

				// Région du Poro
				new Zone("Korhogo", "VILLE", "Poro", "Korhogo"),
				new Zone("Dikodougou", "VILLE", "Poro", "Dikodougou"),
				new Zone("M'Bengué", "VILLE", "Poro", "M'Bengué"),
				new Zone("Sinématiali", "VILLE", "Poro", "Sinématiali"),

				// Région du Tchologo
				new Zone("Ferkessédougou", "VILLE", "Tchologo", "Ferkessédougou"),
				new Zone("Kong", "VILLE", "Tchologo", "Kong"),
				new Zone("Ouangolodougou", "VILLE", "Tchologo", "Ouangolodougou"),

				// Région du Bagoué
				new Zone("Boundiali", "VILLE", "Bagoué", "Boundiali"),
				new Zone("Kouto", "VILLE", "Bagoué", "Kouto"),
				new Zone("Tengréla", "VILLE", "Bagoué", "Tengréla"),

				// Région du Gbêkê
				new Zone("Yamoussoukro", "VILLE", "Yamoussoukro", "Yamoussoukro"),

				// Communes d'Abidjan et environs
				new Zone("Abobo", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Adjamé", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Attécoubé", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Cocody", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Koumassi", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Marcory", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Le Plateau", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Plateau", "COMMUNE", "Lagunes", "Abidjan"), // Alias
				new Zone("Port-Bouët", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Treichville", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Yopougon", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Anyama", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Bingerville", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Songon", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Brofodoumé", "COMMUNE", "Lagunes", "Abidjan"),
				new Zone("Riviera", "COMMUNE", "Lagunes", "Abidjan")

		);

		// Sauvegarder uniquement les zones qui n'existent pas déjà
		for (Zone zone : zones) {
			// Normaliser le nom de la zone (majuscules sans accents)
			String normalizedName = normalizeZoneName(zone.getName());
			zone.setName(normalizedName);
			
			if (!zoneRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
				zoneRepository.save(zone);
			}
		}
	}

	/**
	 * Normalise le nom d'une zone : majuscules sans accents
	 * Exemple: "Abengourou" -> "ABENGOUROU", "Adiaké" -> "ADIAKE"
	 */
	private String normalizeZoneName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return name;
		}
		
		// Convertir en majuscules
		String upper = name.trim().toUpperCase();
		
		// Remplacer les caractères accentués par leurs équivalents sans accent
		upper = upper.replace("À", "A")
					.replace("Á", "A")
					.replace("Â", "A")
					.replace("Ã", "A")
					.replace("Ä", "A")
					.replace("Å", "A")
					.replace("Æ", "AE")
					.replace("Ç", "C")
					.replace("È", "E")
					.replace("É", "E")
					.replace("Ê", "E")
					.replace("Ë", "E")
					.replace("Ì", "I")
					.replace("Í", "I")
					.replace("Î", "I")
					.replace("Ï", "I")
					.replace("Ð", "D")
					.replace("Ñ", "N")
					.replace("Ò", "O")
					.replace("Ó", "O")
					.replace("Ô", "O")
					.replace("Õ", "O")
					.replace("Ö", "O")
					.replace("Ø", "O")
					.replace("Ù", "U")
					.replace("Ú", "U")
					.replace("Û", "U")
					.replace("Ü", "U")
					.replace("Ý", "Y")
					.replace("Þ", "TH")
					.replace("Ÿ", "Y");
		
		return upper;
	}
}

