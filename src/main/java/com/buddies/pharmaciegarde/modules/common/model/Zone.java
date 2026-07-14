package com.buddies.pharmaciegarde.modules.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "zones")
public class Zone {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Le nom de la zone est obligatoire")
	@Size(max = 255, message = "Le nom de la zone ne doit pas dépasser 255 caractères")
	@Column(nullable = false, unique = true)
	private String name; // Nom de la ville ou commune

	@Size(max = 100, message = "Le type ne doit pas dépasser 100 caractères")
	@Column(name = "type")
	private String type; // "VILLE" ou "COMMUNE"

	@Size(max = 100, message = "La région ne doit pas dépasser 100 caractères")
	@Column(name = "region")
	private String region; // Région de Côte d'Ivoire

	@Size(max = 100, message = "Le département ne doit pas dépasser 100 caractères")
	@Column(name = "departement")
	private String departement; // Département

	@Column(name = "is_active")
	private Boolean isActive = true; // Zone active ou non

	// Constructeurs
	public Zone() {
	}

	public Zone(String name, String type, String region, String departement) {
		this.name = name;
		this.type = type;
		this.region = region;
		this.departement = departement;
		this.isActive = true;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getDepartement() {
		return departement;
	}

	public void setDepartement(String departement) {
		this.departement = departement;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}
}

