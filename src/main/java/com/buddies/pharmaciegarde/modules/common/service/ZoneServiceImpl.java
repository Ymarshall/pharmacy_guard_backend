package com.buddies.pharmaciegarde.modules.common.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.modules.common.dto.ZoneCreateDto;
import com.buddies.pharmaciegarde.modules.common.dto.ZoneResponseDto;
import com.buddies.pharmaciegarde.modules.common.dto.ZoneUpdateDto;
import com.buddies.pharmaciegarde.modules.common.model.Zone;
import com.buddies.pharmaciegarde.modules.common.repository.ZoneRepository;
import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;

@Service
@Transactional
public class ZoneServiceImpl implements ZoneService {

	private final ZoneRepository zoneRepository;

	public ZoneServiceImpl(ZoneRepository zoneRepository) {
		this.zoneRepository = zoneRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ZoneResponseDto> getAllZones() {
		return zoneRepository.findAll().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public ZoneResponseDto getZoneById(Long id) {
		Zone zone = zoneRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Zone", id));
		return mapToResponseDto(zone);
	}

	@Override
	public ZoneResponseDto createZone(ZoneCreateDto createDto) {
		// Normaliser le nom de la zone (majuscules sans accents)
		String normalizedName = normalizeZoneName(createDto.getName());
		
		// Vérifier si une zone avec le même nom existe déjà
		if (zoneRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
			throw new IllegalArgumentException("Une zone avec le nom '" + normalizedName + "' existe déjà");
		}

		Zone zone = new Zone();
		zone.setName(normalizedName);
		zone.setType(createDto.getType());
		zone.setRegion(createDto.getRegion());
		zone.setDepartement(createDto.getDepartement());
		zone.setIsActive(createDto.getIsActive() != null ? createDto.getIsActive() : true);

		Zone savedZone = zoneRepository.save(zone);
		return mapToResponseDto(savedZone);
	}

	@Override
	public ZoneResponseDto updateZone(Long id, ZoneUpdateDto updateDto) {
		Zone zone = zoneRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Zone", id));

		// Vérifier si le nouveau nom n'est pas déjà utilisé par une autre zone
		if (updateDto.getName() != null && !updateDto.getName().equals(zone.getName())) {
			// Normaliser le nom de la zone (majuscules sans accents)
			String normalizedName = normalizeZoneName(updateDto.getName());
			if (zoneRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
				throw new IllegalArgumentException("Une zone avec le nom '" + normalizedName + "' existe déjà");
			}
			zone.setName(normalizedName);
		}
		if (updateDto.getType() != null) {
			zone.setType(updateDto.getType());
		}
		if (updateDto.getRegion() != null) {
			zone.setRegion(updateDto.getRegion());
		}
		if (updateDto.getDepartement() != null) {
			zone.setDepartement(updateDto.getDepartement());
		}
		if (updateDto.getIsActive() != null) {
			zone.setIsActive(updateDto.getIsActive());
		}

		Zone updatedZone = zoneRepository.save(zone);
		return mapToResponseDto(updatedZone);
	}

	@Override
	public void deleteZone(Long id) {
		Zone zone = zoneRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Zone", id));
		zoneRepository.delete(zone);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ZoneResponseDto> getActiveZones() {
		return zoneRepository.findByIsActive(true).stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ZoneResponseDto> getZonesByType(String type) {
		return zoneRepository.findByTypeAndIsActive(type, true).stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ZoneResponseDto> getZonesByRegion(String region) {
		return zoneRepository.findByRegion(region).stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	private ZoneResponseDto mapToResponseDto(Zone zone) {
		ZoneResponseDto dto = new ZoneResponseDto();
		dto.setId(zone.getId());
		dto.setName(zone.getName());
		dto.setType(zone.getType());
		dto.setRegion(zone.getRegion());
		dto.setDepartement(zone.getDepartement());
		dto.setIsActive(zone.getIsActive());
		return dto;
	}

	/**
	 * Normalise le nom d'une zone : majuscules sans accents
	 * Exemple: "Abengourou" -> "ABENGOUROU", "Adiaké" -> "ADIAKE"
	 */
	private String normalizeZoneName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return name;
		}
		
		// Convertir en majuscules
		String upper = name.trim().toUpperCase();
		
		// Remplacer les caractères accentués par leurs équivalents sans accent
		upper = upper.replace("À", "A")
					.replace("Á", "A")
					.replace("Â", "A")
					.replace("Ã", "A")
					.replace("Ä", "A")
					.replace("Å", "A")
					.replace("Æ", "AE")
					.replace("Ç", "C")
					.replace("È", "E")
					.replace("É", "E")
					.replace("Ê", "E")
					.replace("Ë", "E")
					.replace("Ì", "I")
					.replace("Í", "I")
					.replace("Î", "I")
					.replace("Ï", "I")
					.replace("Ð", "D")
					.replace("Ñ", "N")
					.replace("Ò", "O")
					.replace("Ó", "O")
					.replace("Ô", "O")
					.replace("Õ", "O")
					.replace("Ö", "O")
					.replace("Ø", "O")
					.replace("Ù", "U")
					.replace("Ú", "U")
					.replace("Û", "U")
					.replace("Ü", "U")
					.replace("Ý", "Y")
					.replace("Þ", "TH")
					.replace("Ÿ", "Y");
		
		return upper;
	}
}

