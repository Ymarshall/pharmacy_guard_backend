package com.buddies.pharmaciegarde.modules.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.api.PageRequests;
import com.buddies.pharmaciegarde.common.api.PageResponse;
import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.config.security.AuthPrincipal;
import com.buddies.pharmaciegarde.config.security.JwtService;
import com.buddies.pharmaciegarde.config.security.SecurityUtils;
import com.buddies.pharmaciegarde.modules.user.dto.ClientCreateDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientLoginResponseDto;
import com.buddies.pharmaciegarde.modules.user.dto.ClientResponseDto;
import com.buddies.pharmaciegarde.modules.user.dto.LocationUpdateDto;
import com.buddies.pharmaciegarde.modules.user.model.Client;
import com.buddies.pharmaciegarde.modules.user.model.Client.ProviderType;
import com.buddies.pharmaciegarde.modules.user.repository.ClientRepository;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

	private final ClientRepository clientRepository;
	private final JwtService jwtService;

	public ClientServiceImpl(ClientRepository clientRepository, JwtService jwtService) {
		this.clientRepository = clientRepository;
		this.jwtService = jwtService;
	}

	@Override
	public ClientResponseDto create(ClientCreateDto clientCreateDto) {
		// Vérifier que le téléphone n'existe pas déjà
		if (clientCreateDto.getPhone() != null && !clientCreateDto.getPhone().trim().isEmpty()) {
			if (clientRepository.existsByPhone(clientCreateDto.getPhone())) {
				throw new IllegalArgumentException("Un client avec le numéro de téléphone '" + clientCreateDto.getPhone()
						+ "' existe déjà");
			}
		}

		// Valider les coordonnées GPS
		if (clientCreateDto.getLatitude() == null || clientCreateDto.getLongitude() == null) {
			throw new IllegalArgumentException("La latitude et la longitude sont obligatoires");
		}

		if (clientCreateDto.getLatitude() < -90 || clientCreateDto.getLatitude() > 90) {
			throw new IllegalArgumentException("La latitude doit être entre -90 et 90");
		}

		if (clientCreateDto.getLongitude() < -180 || clientCreateDto.getLongitude() > 180) {
			throw new IllegalArgumentException("La longitude doit être entre -180 et 180");
		}

		Client client = new Client();
		client.setPhone(clientCreateDto.getPhone());
		client.setLatitude(clientCreateDto.getLatitude());
		client.setLongitude(clientCreateDto.getLongitude());
		client.setProviderType(ProviderType.PHONE); // Toujours PHONE maintenant
		client.setIsActive(clientCreateDto.getIsActive() != null ? clientCreateDto.getIsActive() : true);

		Client savedClient = clientRepository.save(client);
		return mapToResponseDto(savedClient);
	}

	@Override
	@Transactional(readOnly = true)
	public ClientResponseDto getById(Long id) {
		Client client = clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Client", id));
		return mapToResponseDto(client);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClientResponseDto> getAll() {
		return clientRepository.findAll().stream().map(this::mapToResponseDto).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public com.buddies.pharmaciegarde.common.api.PageResponse<ClientResponseDto> getPage(int page, int size,
			String search, String sort) {
		org.springframework.data.domain.Sort parsed = com.buddies.pharmaciegarde.common.api.Sorts.parseOr(sort,
				java.util.Set.of("id", "phone", "firstName", "lastName", "email"),
				org.springframework.data.domain.Sort.by("id"));
		if (search == null || search.isBlank()) {
			return com.buddies.pharmaciegarde.common.api.PageResponse.from(
					clientRepository.findAll(com.buddies.pharmaciegarde.common.api.PageRequests.of(page, size, parsed))
							.map(this::mapToResponseDto));
		}
		return com.buddies.pharmaciegarde.common.api.PageResponse.from(
				com.buddies.pharmaciegarde.common.api.Slices.of(filterClients(search, parsed), page, size)
						.map(this::mapToResponseDto));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClientResponseDto> list(String search, String sort) {
		org.springframework.data.domain.Sort parsed = com.buddies.pharmaciegarde.common.api.Sorts.parseOr(sort,
				java.util.Set.of("id", "phone", "firstName", "lastName", "email"),
				org.springframework.data.domain.Sort.by("id"));
		return filterClients(search, parsed).stream().map(this::mapToResponseDto).collect(Collectors.toList());
	}

	private List<Client> filterClients(String search, org.springframework.data.domain.Sort parsed) {
		String field = com.buddies.pharmaciegarde.common.api.Sorts.field(parsed, "id");
		boolean desc = com.buddies.pharmaciegarde.common.api.Sorts.isDescending(parsed);
		java.util.Comparator<Client> comparator = switch (field) {
			case "phone" -> java.util.Comparator.comparing(Client::getPhone, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "firstName" -> java.util.Comparator.comparing(Client::getFirstName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "lastName" -> java.util.Comparator.comparing(Client::getLastName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "email" -> java.util.Comparator.comparing(Client::getEmail, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			default -> java.util.Comparator.comparing(Client::getId);
		};
		if (desc) {
			comparator = comparator.reversed();
		}
		return clientRepository.findAll().stream()
				.filter(client -> com.buddies.pharmaciegarde.common.api.TextSearch.containsAny(search, client.getPhone(),
						client.getEmail(), client.getFirstName(), client.getLastName()))
				.sorted(comparator)
				.collect(Collectors.toList());
	}

	@Override
	public ClientResponseDto update(Long id, ClientCreateDto clientCreateDto) {
		Client client = clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Client", id));

		// Vérifier les doublons si le téléphone est modifié
		if (clientCreateDto.getPhone() != null && !clientCreateDto.getPhone().equals(client.getPhone())) {
			if (clientRepository.existsByPhone(clientCreateDto.getPhone())) {
				throw new IllegalArgumentException(
						"Un client avec le numéro de téléphone '" + clientCreateDto.getPhone() + "' existe déjà");
			}
			client.setPhone(clientCreateDto.getPhone());
		}

		// Mettre à jour la localisation si fournie
		if (clientCreateDto.getLatitude() != null) {
			client.setLatitude(clientCreateDto.getLatitude());
		}
		if (clientCreateDto.getLongitude() != null) {
			client.setLongitude(clientCreateDto.getLongitude());
		}

		// Mettre à jour le statut actif si fourni
		if (clientCreateDto.getIsActive() != null) {
			client.setIsActive(clientCreateDto.getIsActive());
		}

		Client updatedClient = clientRepository.save(client);
		return mapToResponseDto(updatedClient);
	}

	@Override
	public void delete(Long id) {
		if (!clientRepository.existsById(id)) {
			throw new ResourceNotFoundException("Client", id);
		}
		clientRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public ClientLoginResponseDto login(ClientLoginDto clientLoginDto) {
		// Chercher le client par téléphone
		Client client = clientRepository.findByPhone(clientLoginDto.getPhone())
				.orElseThrow(() -> new IllegalArgumentException("Numéro de téléphone non trouvé"));

		// Vérifier si le client est actif
		if (client.getIsActive() == null || !client.getIsActive()) {
			throw new IllegalArgumentException("Votre compte a été désactivé. Veuillez contacter l'administration.");
		}

		// Créer la réponse avec uniquement les informations du client
		ClientLoginResponseDto response = new ClientLoginResponseDto();
		response.setClient(mapToResponseDto(client));
		response.setToken(jwtService.generate(new AuthPrincipal(client.getId(), client.getPhone(),
				AuthPrincipal.ROLE_CLIENT, AuthPrincipal.KIND_CLIENT)));
		response.setTokenType("Bearer");
		response.setExpiresInSeconds(jwtService.getExpirationSeconds());
		return response;
	}

	@Override
	public ClientResponseDto updateLocation(Long id, LocationUpdateDto locationUpdateDto) {
		SecurityUtils.assertClientSelfOrBackoffice(id);
		Client client = clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Client", id));

		client.setLatitude(locationUpdateDto.getLatitude());
		client.setLongitude(locationUpdateDto.getLongitude());

		Client updatedClient = clientRepository.save(client);
		return mapToResponseDto(updatedClient);
	}

	@Override
	public ClientResponseDto updateStatus(Long id, Boolean isActive) {
		Client client = clientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Client", id));

		client.setIsActive(isActive);

		Client updatedClient = clientRepository.save(client);
		return mapToResponseDto(updatedClient);
	}

	private ClientResponseDto mapToResponseDto(Client client) {
		ClientResponseDto dto = new ClientResponseDto();
		dto.setId(client.getId());
		dto.setEmail(client.getEmail());
		dto.setPhone(client.getPhone());
		dto.setFirstName(client.getFirstName());
		dto.setLastName(client.getLastName());
		dto.setProviderType(client.getProviderType());
		dto.setLatitude(client.getLatitude());
		dto.setLongitude(client.getLongitude());
		dto.setIsActive(client.getIsActive());
		dto.setCreatedAt(client.getCreatedAt());
		dto.setUpdatedAt(client.getUpdatedAt());
		return dto;
	}
}
