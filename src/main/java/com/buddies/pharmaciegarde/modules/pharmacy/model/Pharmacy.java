package com.buddies.pharmaciegarde.modules.pharmacy.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "pharmacies")
public class Pharmacy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Le nom de la pharmacie est obligatoire")
	@Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
	@Column(nullable = false)
	private String name;

	@NotBlank(message = "Le numéro de téléphone principal est obligatoire")
	@Size(min = 8, max = 20, message = "Le numéro de téléphone doit contenir entre 8 et 20 caractères")
	@Column(name = "phone1", nullable = false)
	private String phone1;

	@Size(min = 8, max = 20, message = "Le numéro de téléphone secondaire doit contenir entre 8 et 20 caractères")
	@Column(name = "phone2")
	private String phone2;

	@Email(message = "L'email doit être valide")
	@Column(name = "email")
	private String email;

	@Column(name = "horaires", columnDefinition = "TEXT")
	private String horaires;

	@NotBlank(message = "La ville est obligatoire")
	@Size(min = 2, max = 100, message = "La ville doit contenir entre 2 et 100 caractères")
	@Column(nullable = false)
	private String city;

	@NotBlank(message = "La commune est obligatoire")
	@Size(min = 2, max = 100, message = "La commune doit contenir entre 2 et 100 caractères")
	@Column(nullable = false)
	private String commune;

	@Size(max = 100, message = "Le district ne doit pas dépasser 100 caractères")
	@Column(name = "district")
	private String district;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Size(max = 255, message = "Le nom du pharmacien ne doit pas dépasser 255 caractères")
	@Column(name = "pharmacist_name")
	private String pharmacistName;

	@Column(name = "photo")
	private String photo;

	@NotNull(message = "Le statut est obligatoire")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PharmacyStatus statut;

	@NotBlank(message = "La localisation est obligatoire")
	@Column(name = "location", nullable = false)
	private String location;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "pharmacy_assurances",
			joinColumns = @JoinColumn(name = "pharmacy_id"),
			inverseJoinColumns = @JoinColumn(name = "assurance_id")
	)
	private Set<Assurance> assurances = new HashSet<>();

	// Constructeurs
	public Pharmacy() {
	}

	public Pharmacy(String name, String phone1, String city, String commune, PharmacyStatus statut, String location) {
		this.name = name;
		this.phone1 = phone1;
		this.city = city;
		this.commune = commune;
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

	public Set<Assurance> getAssurances() {
		return assurances;
	}

	public void setAssurances(Set<Assurance> assurances) {
		this.assurances = assurances;
	}

	// Enum pour le statut
	public enum PharmacyStatus {
		ACTIF, INACTIF
	}
}

