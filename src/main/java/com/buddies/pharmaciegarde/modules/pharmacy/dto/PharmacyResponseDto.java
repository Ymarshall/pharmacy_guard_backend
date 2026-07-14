package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant les informations d'une pharmacie")
public class PharmacyResponseDto {

	@Schema(description = "Identifiant unique de la pharmacie", example = "1")
	private Long id;
	
	@Schema(description = "Nom de la pharmacie", example = "Pharmacie Centrale")
	private String name;
	
	@Schema(description = "Numéro de téléphone principal", example = "+261 34 12 345 67")
	private String phone1;
	
	@Schema(description = "Numéro de téléphone secondaire", example = "+261 34 12 345 68")
	private String phone2;
	
	@Schema(description = "Adresse email", example = "contact@pharmacie-centrale.mg")
	private String email;
	
	@Schema(description = "Horaires d'ouverture", example = "Lundi-Vendredi: 8h-18h, Samedi: 8h-12h")
	private String horaires;
	
	@Schema(description = "Ville", example = "Antananarivo")
	private String city;
	
	@Schema(description = "Commune", example = "Analakely")
	private String commune;
	
	@Schema(description = "District", example = "Analamanga")
	private String district;
	
	@Schema(description = "Description de la pharmacie")
	private String description;
	
	@Schema(description = "Nom du pharmacien", example = "Dr. Jean Dupont")
	private String pharmacistName;
	
	@Schema(description = "URL de la photo de la pharmacie")
	private String photo;
	
	@Schema(description = "Statut de la pharmacie", example = "ACTIF")
	private PharmacyStatus statut;
	
	@Schema(description = "Localisation (coordonnées GPS au format 'lat,lon')", example = "-18.8792,47.5079")
	private String location;

	@Schema(description = "Latitude GPS de la pharmacie", example = "-18.8792")
	private Double latitude;

	@Schema(description = "Longitude GPS de la pharmacie", example = "47.5079")
	private Double longitude;

	@Schema(description = "Liste des assurances prises en charge")
	private java.util.List<AssuranceResponseDto> assurances;

	@Schema(description = "Note moyenne (sur 5)", example = "4.5")
	private Double averageRating;

	@Schema(description = "Nombre total de commentaires", example = "25")
	private Long totalReviews;

	@Schema(description = "Liste des 3 derniers commentaires")
	private java.util.List<PharmacyReviewResponseDto> recentReviews;

	@Schema(description = "Indique si la pharmacie est de garde pour la période active", example = "true")
	private Boolean isOnDuty;

	// Constructeurs
	public PharmacyResponseDto() {
	}

	public PharmacyResponseDto(Long id, String name, String phone1, String phone2, String email, String horaires,
			String city, String commune, String district, String description, String photo, PharmacyStatus statut,
			String location) {
		this.id = id;
		this.name = name;
		this.phone1 = phone1;
		this.phone2 = phone2;
		this.email = email;
		this.horaires = horaires;
		this.city = city;
		this.commune = commune;
		this.district = district;
		this.description = description;
		this.photo = photo;
		this.statut = statut;
		this.location = location;
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone1() {
		return phone1;
	}

	public void setPhone1(String phone1) {
		this.phone1 = phone1;
	}

	public String getPhone2() {
		return phone2;
	}

	public void setPhone2(String phone2) {
		this.phone2 = phone2;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getHoraires() {
		return horaires;
	}

	public void setHoraires(String horaires) {
		this.horaires = horaires;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCommune() {
		return commune;
	}

	public void setCommune(String commune) {
		this.commune = commune;
	}

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPharmacistName() {
		return pharmacistName;
	}

	public void setPharmacistName(String pharmacistName) {
		this.pharmacistName = pharmacistName;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public PharmacyStatus getStatut() {
		return statut;
	}

	public void setStatut(PharmacyStatus statut) {
		this.statut = statut;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public java.util.List<AssuranceResponseDto> getAssurances() {
		return assurances;
	}

	public void setAssurances(java.util.List<AssuranceResponseDto> assurances) {
		this.assurances = assurances;
	}

	public Double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(Double averageRating) {
		this.averageRating = averageRating;
	}

	public Long getTotalReviews() {
		return totalReviews;
	}

	public void setTotalReviews(Long totalReviews) {
		this.totalReviews = totalReviews;
	}

	public java.util.List<PharmacyReviewResponseDto> getRecentReviews() {
		return recentReviews;
	}

	public void setRecentReviews(java.util.List<PharmacyReviewResponseDto> recentReviews) {
		this.recentReviews = recentReviews;
	}

	public Boolean getIsOnDuty() {
		return isOnDuty;
	}

	public void setIsOnDuty(Boolean isOnDuty) {
		this.isOnDuty = isOnDuty;
	}
}

