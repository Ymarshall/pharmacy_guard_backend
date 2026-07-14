package com.buddies.pharmaciegarde.modules.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "emergency_numbers")
public class EmergencyNumber {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Le nom est obligatoire")
	@Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
	@Column(nullable = false)
	private String name;

	@NotBlank(message = "Le numéro est obligatoire")
	@Size(min = 2, max = 30, message = "Le numéro doit contenir entre 2 et 30 caractères")
	@Column(nullable = false)
	private String number;

	@Size(max = 500, message = "L'icône ne doit pas dépasser 500 caractères")
	@Column(name = "icon")
	private String icon; // URL ou nom de l'icône

	@Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Size(max = 50, message = "La couleur ne doit pas dépasser 50 caractères")
	@Column(name = "color")
	private String color; // Code couleur (hex, nom, etc.)

	@NotNull(message = "La catégorie est obligatoire")
	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false)
	private EmergencyCategory category;

	// Constructeurs
	public EmergencyNumber() {
	}

	public EmergencyNumber(String name, String number) {
		this.name = name;
		this.number = number;
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

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public EmergencyCategory getCategory() {
		return category;
	}

	public void setCategory(EmergencyCategory category) {
		this.category = category;
	}

	// Enum pour la catégorie
	public enum EmergencyCategory {
		medical, fire, police, bloodBank, other
	}
}

