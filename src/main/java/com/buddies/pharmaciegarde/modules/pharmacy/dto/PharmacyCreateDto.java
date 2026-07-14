package com.buddies.pharmaciegarde.modules.pharmacy.dto;

import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour créer une nouvelle pharmacie")
public class PharmacyCreateDto {

	@Schema(description = "Nom de la pharmacie", example = "Pharmacie Centrale", required = true)
	@NotBlank(message = "Le nom de la pharmacie est obligatoire")
	@Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
	private String name;

	@Schema(description = "Numéro de téléphone principal", example = "+261 34 12 345 67", required = true)
	@NotBlank(message = "Le numéro de téléphone principal est obligatoire")
	@Size(min = 8, max = 20, message = "Le numéro de téléphone doit contenir entre 8 et 20 caractères")
	private String phone1;

	@Schema(description = "Numéro de téléphone secondaire", example = "+261 34 12 345 68")
	@Size(min = 8, max = 20, message = "Le numéro de téléphone secondaire doit contenir entre 8 et 20 caractères")
	private String phone2;

	@Schema(description = "Adresse email", example = "contact@pharmacie-centrale.mg")
	@Email(message = "L'email doit être valide")
	private String email;

	@Schema(description = "Horaires d'ouverture", example = "Lundi-Vendredi: 8h-18h, Samedi: 8h-12h")
	private String horaires;

	@Schema(description = "Ville", example = "Antananarivo", required = true)
	@NotBlank(message = "La ville est obligatoire")
	@Size(min = 2, max = 100, message = "La ville doit contenir entre 2 et 100 caractères")
	private String city;

	@Schema(description = "Commune", example = "Analakely", required = true)
	@NotBlank(message = "La commune est obligatoire")
	@Size(min = 2, max = 100, message = "La commune doit contenir entre 2 et 100 caractères")
	private String commune;

	@Schema(description = "District", example = "Analamanga")
	@Size(max = 100, message = "Le district ne doit pas dépasser 100 caractères")
	private String district;

	@Schema(description = "Description de la pharmacie")
	private String description;

	@Schema(description = "Nom du pharmacien", example = "Dr. Jean Dupont")
	@Size(max = 255, message = "Le nom du pharmacien ne doit pas dépasser 255 caractères")
	private String pharmacistName;

	@Schema(description = "URL de la photo de la pharmacie")
	private String photo;

	@Schema(description = "Statut de la pharmacie", example = "ACTIF", required = true, allowableValues = {"ACTIF", "INACTIF"})
	@NotNull(message = "Le statut est obligatoire")
	private PharmacyStatus statut;

	@Schema(description = "Localisation (coordonnées GPS)", example = "-18.8792,47.5079", required = true)
	@NotBlank(message = "La localisation est obligatoire")
	private String location;

	@Schema(description = "Liste des IDs des assurances prises en charge", example = "[1, 2, 3]")
	private java.util.List<Long> assuranceIds;

	// Constructeurs
	public PharmacyCreateDto() {
	}

	// Getters et Setters
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

	public java.util.List<Long> getAssuranceIds() {
		return assuranceIds;
	}

	public void setAssuranceIds(java.util.List<Long> assuranceIds) {
		this.assuranceIds = assuranceIds;
	}
}

