package com.buddies.pharmaciegarde.modules.pharmacy.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "assurances")
public class Assurance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Le nom de l'assurance est obligatoire")
	@Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
	@Column(nullable = false, unique = true)
	private String name;

	@Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@ManyToMany(mappedBy = "assurances")
	private Set<Pharmacy> pharmacies = new HashSet<>();

	// Constructeurs
	public Assurance() {
	}

	public Assurance(String name) {
		this.name = name;
	}

	public Assurance(String name, String description) {
		this.name = name;
		this.description = description;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Pharmacy> getPharmacies() {
		return pharmacies;
	}

	public void setPharmacies(Set<Pharmacy> pharmacies) {
		this.pharmacies = pharmacies;
	}
}

