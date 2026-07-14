package com.buddies.pharmaciegarde.modules.pharmacy.service;

import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service pour mettre à jour les coordonnées GPS de toutes les pharmacies
 * en utilisant l'API Google Maps Geocoding
 */
@Service
@Order(3) // Exécuter après l'initialisation des pharmacies
public class PharmacyGeocodingUpdateService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PharmacyGeocodingUpdateService.class);

    private final PharmacyRepository pharmacyRepository;
    private final GeocodingService geocodingService;

    public PharmacyGeocodingUpdateService(PharmacyRepository pharmacyRepository, GeocodingService geocodingService) {
        this.pharmacyRepository = pharmacyRepository;
        this.geocodingService = geocodingService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Vérifier si on doit exécuter la mise à jour
        // Par défaut, on ne l'exécute pas automatiquement pour éviter les appels API coûteux
        // Utiliser un endpoint REST pour déclencher manuellement
        logger.info("Service de géocodage des pharmacies prêt. Utilisez l'endpoint /api/pharmacies/update-geocoding pour mettre à jour.");
    }

    /**
     * Met à jour les coordonnées GPS de toutes les pharmacies
     * 
     * @param forceUpdate Si true, met à jour même si les coordonnées existent déjà
     * @return Statistiques de la mise à jour
     */
    @Transactional
    public GeocodingUpdateStats updateAllPharmacies(boolean forceUpdate) {
        logger.info("Début de la mise à jour des coordonnées GPS pour toutes les pharmacies (forceUpdate={})", forceUpdate);

        List<Pharmacy> pharmacies = pharmacyRepository.findAll();
        GeocodingUpdateStats stats = new GeocodingUpdateStats();
        stats.total = pharmacies.size();

        for (Pharmacy pharmacy : pharmacies) {
            try {
                // Vérifier si la pharmacie a déjà des coordonnées valides
                boolean hasValidLocation = hasValidLocation(pharmacy.getLocation());
                
                if (!forceUpdate && hasValidLocation) {
                    logger.debug("Pharmacie {} a déjà des coordonnées valides, ignorée", pharmacy.getName());
                    stats.skipped++;
                    continue;
                }

                // Construire l'adresse complète
                String address = geocodingService.buildAddress(
                        pharmacy.getName(),
                        pharmacy.getCity(),
                        pharmacy.getCommune(),
                        pharmacy.getDistrict(),
                        pharmacy.getDescription()
                );

                if (address == null || address.trim().isEmpty()) {
                    logger.warn("Impossible de construire une adresse pour la pharmacie {}", pharmacy.getName());
                    stats.failed++;
                    continue;
                }

                // Géocoder l'adresse
                double[] coordinates = geocodingService.geocodeAddress(address);

                if (coordinates == null || coordinates.length != 2) {
                    logger.warn("Échec du géocodage pour la pharmacie: {}", pharmacy.getName());
                    stats.failed++;
                    continue;
                }

                // Mettre à jour la localisation
                String newLocation = coordinates[0] + "," + coordinates[1];
                pharmacy.setLocation(newLocation);
                pharmacyRepository.save(pharmacy);

                logger.info("Coordonnées mises à jour pour {}: {}", pharmacy.getName(), newLocation);
                stats.updated++;

                // Attendre un peu pour éviter de dépasser les limites de l'API (rate limiting)
                Thread.sleep(100); // 100ms entre chaque appel

            } catch (Exception e) {
                logger.error("Erreur lors de la mise à jour de la pharmacie {}: {}", 
                        pharmacy.getName(), e.getMessage(), e);
                stats.failed++;
            }
        }

        logger.info("Mise à jour terminée: {} mises à jour, {} ignorées, {} échecs sur {} total",
                stats.updated, stats.skipped, stats.failed, stats.total);

        return stats;
    }

    /**
     * Met à jour les coordonnées GPS d'une pharmacie spécifique
     */
    @Transactional
    public boolean updatePharmacy(Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacie non trouvée: " + pharmacyId));

        String address = geocodingService.buildAddress(
                pharmacy.getName(),
                pharmacy.getCity(),
                pharmacy.getCommune(),
                pharmacy.getDistrict(),
                pharmacy.getDescription()
        );

        if (address == null || address.trim().isEmpty()) {
            logger.warn("Impossible de construire une adresse pour la pharmacie {}", pharmacy.getName());
            return false;
        }

        double[] coordinates = geocodingService.geocodeAddress(address);

        if (coordinates == null || coordinates.length != 2) {
            logger.warn("Échec du géocodage pour la pharmacie: {}", pharmacy.getName());
            return false;
        }

        String newLocation = coordinates[0] + "," + coordinates[1];
        pharmacy.setLocation(newLocation);
        pharmacyRepository.save(pharmacy);

        logger.info("Coordonnées mises à jour pour {}: {}", pharmacy.getName(), newLocation);
        return true;
    }

    /**
     * Vérifie si une localisation est valide (format "lat,lon" avec des nombres valides)
     */
    private boolean hasValidLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }

        try {
            String[] parts = location.split(",");
            if (parts.length != 2) {
                return false;
            }

            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());

            // Vérifier que les coordonnées sont dans des plages valides
            return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Classe pour stocker les statistiques de mise à jour
     */
    public static class GeocodingUpdateStats {
        public int total = 0;
        public int updated = 0;
        public int skipped = 0;
        public int failed = 0;
    }
}

