package com.buddies.pharmaciegarde.modules.common.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.api.PageRequests;
import com.buddies.pharmaciegarde.common.api.PageResponse;
import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.modules.common.dto.EmergencyNumberCreateDto;
import com.buddies.pharmaciegarde.modules.common.dto.EmergencyNumberResponseDto;
import com.buddies.pharmaciegarde.modules.common.model.EmergencyNumber;
import com.buddies.pharmaciegarde.modules.common.repository.EmergencyNumberRepository;

@Service
@Transactional
public class EmergencyNumberServiceImpl implements EmergencyNumberService {

	private final EmergencyNumberRepository emergencyNumberRepository;

	public EmergencyNumberServiceImpl(EmergencyNumberRepository emergencyNumberRepository) {
		this.emergencyNumberRepository = emergencyNumberRepository;
	}

	@Override
	public EmergencyNumberResponseDto create(EmergencyNumberCreateDto emergencyNumberCreateDto) {
		// Vérifier si un numéro avec le même nom existe déjà
		if (emergencyNumberRepository.existsByName(emergencyNumberCreateDto.getName())) {
			throw new IllegalArgumentException("Un numéro d'urgence avec le nom '" + emergencyNumberCreateDto.getName() + "' existe déjà");
		}

		// Vérifier si un numéro avec le même numéro existe déjà
		if (emergencyNumberRepository.existsByNumber(emergencyNumberCreateDto.getNumber())) {
			throw new IllegalArgumentException("Un numéro d'urgence avec le numéro '" + emergencyNumberCreateDto.getNumber() + "' existe déjà");
		}

		EmergencyNumber emergencyNumber = new EmergencyNumber();
		emergencyNumber.setName(emergencyNumberCreateDto.getName());
		emergencyNumber.setNumber(emergencyNumberCreateDto.getNumber());
		emergencyNumber.setIcon(emergencyNumberCreateDto.getIcon());
		emergencyNumber.setDescription(emergencyNumberCreateDto.getDescription());
		emergencyNumber.setColor(emergencyNumberCreateDto.getColor());
		emergencyNumber.setCategory(emergencyNumberCreateDto.getCategory());

		EmergencyNumber savedEmergencyNumber = emergencyNumberRepository.save(emergencyNumber);
		return mapToResponseDto(savedEmergencyNumber);
	}

	@Override
	@Transactional(readOnly = true)
	public EmergencyNumberResponseDto getById(Long id) {
		EmergencyNumber emergencyNumber = emergencyNumberRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Numéro d'urgence", id));
		return mapToResponseDto(emergencyNumber);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmergencyNumberResponseDto> getAll() {
		return emergencyNumberRepository.findAll().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<EmergencyNumberResponseDto> getPage(int page, int size) {
		Page<EmergencyNumber> result = emergencyNumberRepository.findAll(PageRequests.of(page, size));
		return PageResponse.from(result.map(this::mapToResponseDto));
	}

	@Override
	public EmergencyNumberResponseDto update(Long id, EmergencyNumberCreateDto emergencyNumberCreateDto) {
		EmergencyNumber emergencyNumber = emergencyNumberRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Numéro d'urgence", id));

		// Vérifier si le nom est modifié et s'il existe déjà
		if (!emergencyNumber.getName().equals(emergencyNumberCreateDto.getName()) 
				&& emergencyNumberRepository.existsByName(emergencyNumberCreateDto.getName())) {
			throw new IllegalArgumentException("Un numéro d'urgence avec le nom '" + emergencyNumberCreateDto.getName() + "' existe déjà");
		}

		// Vérifier si le numéro est modifié et s'il existe déjà
		if (!emergencyNumber.getNumber().equals(emergencyNumberCreateDto.getNumber()) 
				&& emergencyNumberRepository.existsByNumber(emergencyNumberCreateDto.getNumber())) {
			throw new IllegalArgumentException("Un numéro d'urgence avec le numéro '" + emergencyNumberCreateDto.getNumber() + "' existe déjà");
		}

		emergencyNumber.setName(emergencyNumberCreateDto.getName());
		emergencyNumber.setNumber(emergencyNumberCreateDto.getNumber());
		emergencyNumber.setIcon(emergencyNumberCreateDto.getIcon());
		emergencyNumber.setDescription(emergencyNumberCreateDto.getDescription());
		emergencyNumber.setColor(emergencyNumberCreateDto.getColor());
		emergencyNumber.setCategory(emergencyNumberCreateDto.getCategory());

		EmergencyNumber updatedEmergencyNumber = emergencyNumberRepository.save(emergencyNumber);
		return mapToResponseDto(updatedEmergencyNumber);
	}

	@Override
	public void delete(Long id) {
		if (!emergencyNumberRepository.existsById(id)) {
			throw new ResourceNotFoundException("Numéro d'urgence", id);
		}
		emergencyNumberRepository.deleteById(id);
	}

	private EmergencyNumberResponseDto mapToResponseDto(EmergencyNumber emergencyNumber) {
		EmergencyNumberResponseDto dto = new EmergencyNumberResponseDto();
		dto.setId(emergencyNumber.getId());
		dto.setName(emergencyNumber.getName());
		dto.setNumber(emergencyNumber.getNumber());
		dto.setIcon(emergencyNumber.getIcon());
		dto.setDescription(emergencyNumber.getDescription());
		dto.setColor(emergencyNumber.getColor());
		dto.setCategory(emergencyNumber.getCategory());
		return dto;
	}
}

