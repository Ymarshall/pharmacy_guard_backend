package com.buddies.pharmaciegarde.common.utils;

/**
 * Utilitaire pour calculer la distance entre deux points GPS en utilisant la formule de Haversine
 */
public class DistanceCalculator {

	private static final double EARTH_RADIUS_KM = 6371.0;

	/**
	 * Calcule la distance en kilomètres entre deux points GPS
	 * 
	 * @param lat1 Latitude du premier point
	 * @param lon1 Longitude du premier point
	 * @param lat2 Latitude du deuxième point
	 * @param lon2 Longitude du deuxième point
	 * @return Distance en kilomètres
	 */
	public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		// Convertir les degrés en radians
		double lat1Rad = Math.toRadians(lat1);
		double lon1Rad = Math.toRadians(lon1);
		double lat2Rad = Math.toRadians(lat2);
		double lon2Rad = Math.toRadians(lon2);

		// Différence de latitude et longitude
		double deltaLat = lat2Rad - lat1Rad;
		double deltaLon = lon2Rad - lon1Rad;

		// Formule de Haversine
		double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
				+ Math.cos(lat1Rad) * Math.cos(lat2Rad)
						* Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		// Distance en kilomètres
		return EARTH_RADIUS_KM * c;
	}

	/**
	 * Parse une chaîne de localisation au format "latitude,longitude"
	 * 
	 * @param location Chaîne au format "lat,lon"
	 * @return Tableau [latitude, longitude] ou null si le format est invalide
	 */
	public static double[] parseLocation(String location) {
		if (location == null || location.trim().isEmpty()) {
			return null;
		}

		try {
			String[] parts = location.split(",");
			if (parts.length != 2) {
				return null;
			}

			double lat = Double.parseDouble(parts[0].trim());
			double lon = Double.parseDouble(parts[1].trim());

			return new double[] { lat, lon };
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

