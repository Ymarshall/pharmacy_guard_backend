package com.buddies.pharmaciegarde.modules.user.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "clients")
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Email(message = "L'email doit être valide")
	@Column(unique = true)
	private String email;

	@Size(min = 8, max = 20, message = "Le numéro de téléphone doit contenir entre 8 et 20 caractères")
	@Column(unique = true)
	private String phone;

	@Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
	@Column(name = "password")
	private String password;

	@Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
	@Column(name = "first_name")
	private String firstName;

	@Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
	@Column(name = "last_name")
	private String lastName;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_type", nullable = false)
	private ProviderType providerType;

	@Column(name = "provider_id")
	private String providerId; // ID du compte Google/Facebook

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true; // Par défaut, le client est actif

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// Constructeurs
	public Client() {
		this.createdAt = LocalDateTime.now();
		this.isActive = true; // Par défaut, le client est actif
	}

	public Client(String firstName, String lastName, ProviderType providerType) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.providerType = providerType;
		this.createdAt = LocalDateTime.now();
	}

	// Méthode pour mettre à jour la date de modification
	@jakarta.persistence.PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public ProviderType getProviderType() {
		return providerType;
	}

	public void setProviderType(ProviderType providerType) {
		this.providerType = providerType;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
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

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	// Enum pour le type de provider
	public enum ProviderType {
		EMAIL, PHONE, GOOGLE, FACEBOOK
	}
}

