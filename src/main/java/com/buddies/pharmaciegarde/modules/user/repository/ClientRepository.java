package com.buddies.pharmaciegarde.modules.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.buddies.pharmaciegarde.modules.user.model.Client;
import com.buddies.pharmaciegarde.modules.user.model.Client.ProviderType;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

	Optional<Client> findByEmail(String email);

	Optional<Client> findByPhone(String phone);

	Optional<Client> findByProviderTypeAndProviderId(ProviderType providerType, String providerId);

	boolean existsByEmail(String email);

	boolean existsByPhone(String phone);

	boolean existsByProviderTypeAndProviderId(ProviderType providerType, String providerId);
}

