package com.buddies.pharmaciegarde.modules.pharmacy.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.buddies.pharmaciegarde.modules.user.model.Client;

@Entity
@Table(name = "pharmacy_reviews", 
	uniqueConstraints = @UniqueConstraint(columnNames = {"pharmacy_id", "client_id"}))
public class PharmacyReview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pharmacy_id", nullable = false)
	@NotNull(message = "La pharmacie est obligatoire")
	private Pharmacy pharmacy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id", nullable = false)
	@NotNull(message = "Le client est obligatoire")
	private Client client;

	@NotNull(message = "La note est obligatoire")
	@Min(value = 1, message = "La note doit être au moins 1")
	@Max(value = 5, message = "La note doit être au plus 5")
	@Column(nullable = false)
	private Integer rating;

	@Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères")
	@Column(name = "comment", columnDefinition = "TEXT")
	private String comment;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// Constructeurs
	public PharmacyReview() {
		this.createdAt = LocalDateTime.now();
	}

	public PharmacyReview(Pharmacy pharmacy, Client client, Integer rating, String comment) {
		this();
		this.pharmacy = pharmacy;
		this.client = client;
		this.rating = rating;
		this.comment = comment;
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

	public Pharmacy getPharmacy() {
		return pharmacy;
	}

	public void setPharmacy(Pharmacy pharmacy) {
		this.pharmacy = pharmacy;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
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
}
