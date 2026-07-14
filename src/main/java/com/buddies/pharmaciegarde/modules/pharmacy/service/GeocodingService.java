package com.buddies.pharmaciegarde.modules.pharmacy.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Service pour géocoder des adresses en utilisant l'API Google Maps Geocoding
 */
@Service
public class GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);
    private static final String GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GeocodingService(@Value("${google.maps.api.key:}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    /**
     * Géocode une adresse et retourne les coordonnées GPS (latitude, longitude)
     * 
     * @param address Adresse complète à géocoder
     * @return Tableau [latitude, longitude] ou null si le géocodage a échoué
     */
    public double[] geocodeAddress(String address) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Clé API Google Maps non configurée. Impossible de géocoder l'adresse: {}", address);
            return null;
        }

        if (address == null || address.trim().isEmpty()) {
            logger.warn("Adresse vide, impossible de géocoder");
            return null;
        }

        try {
            // Construire l'URL de l'API
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(GEOCODING_API_URL)
                    .queryParam("address", address)
                    .queryParam("key", apiKey);

            String url = builder.build().toUriString();
            logger.debug("Appel API Google Geocoding pour: {}", address);

            // Appel à l'API
            GeocodingResponse response = restTemplate.getForObject(url, GeocodingResponse.class);

            if (response == null || !"OK".equals(response.status)) {
                logger.warn("Échec du géocodage pour '{}': status = {}", address, 
                        response != null ? response.status : "null");
                return null;
            }

            if (response.results == null || response.results.isEmpty()) {
                logger.warn("Aucun résultat de géocodage pour '{}'", address);
                return null;
            }

            // Prendre le premier résultat
            GeocodingResult firstResult = response.results.get(0);
            if (firstResult.geometry == null || firstResult.geometry.location == null) {
                logger.warn("Résultat de géocodage invalide pour '{}'", address);
                return null;
            }

            double lat = firstResult.geometry.location.lat;
            double lng = firstResult.geometry.location.lng;

            logger.info("Géocodage réussi pour '{}': lat={}, lng={}", address, lat, lng);
            return new double[]{lat, lng};

        } catch (Exception e) {
            logger.error("Erreur lors du géocodage de '{}': {}", address, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Construit une adresse complète à partir des informations de la pharmacie
     */
    public String buildAddress(String name, String city, String commune, String district, String description) {
        StringBuilder address = new StringBuilder();

        // Ajouter le nom de la pharmacie
        if (name != null && !name.trim().isEmpty()) {
            address.append(name.trim());
        }

        // Ajouter la description si disponible (contient souvent des détails d'adresse)
        if (description != null && !description.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(description.trim());
        }

        // Ajouter le district/quartier
        if (district != null && !district.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(district.trim());
        }

        // Ajouter la commune
        if (commune != null && !commune.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(commune.trim());
        }

        // Ajouter la ville
        if (city != null && !city.trim().isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(city.trim());
        }

        // Ajouter "Côte d'Ivoire" pour améliorer la précision
        if (address.length() > 0) {
            address.append(", Côte d'Ivoire");
        }

        return address.toString();
    }

    // Classes internes pour désérialiser la réponse JSON de l'API Google
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeocodingResponse {
        public String status;
        public List<GeocodingResult> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeocodingResult {
        public Geometry geometry;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Geometry {
        public Location location;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Location {
        @JsonProperty("lat")
        public double lat;
        @JsonProperty("lng")
        public double lng;
    }
}

