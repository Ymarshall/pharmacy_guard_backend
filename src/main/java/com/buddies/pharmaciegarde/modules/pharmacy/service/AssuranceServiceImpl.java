package com.buddies.pharmaciegarde.modules.pharmacy.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.api.PageRequests;
import com.buddies.pharmaciegarde.common.api.PageResponse;
import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.AssuranceCreateDto;
import com.buddies.pharmaciegarde.modules.pharmacy.dto.AssuranceResponseDto;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Assurance;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.AssuranceRepository;

@Service
@Transactional
public class AssuranceServiceImpl implements AssuranceService {

	private final AssuranceRepository assuranceRepository;

	public AssuranceServiceImpl(AssuranceRepository assuranceRepository) {
		this.assuranceRepository = assuranceRepository;
	}

	@Override
	@CacheEvict(value = "assurances", allEntries = true)
	public AssuranceResponseDto create(AssuranceCreateDto assuranceCreateDto) {
		// Vérifier si une assurance avec le même nom existe déjà
		if (assuranceRepository.existsByName(assuranceCreateDto.getName())) {
			throw new IllegalArgumentException("Une assurance avec le nom '" + assuranceCreateDto.getName() + "' existe déjà");
		}

		Assurance assurance = new Assurance();
		assurance.setName(assuranceCreateDto.getName());
		assurance.setDescription(assuranceCreateDto.getDescription());

		Assurance savedAssurance = assuranceRepository.save(assurance);
		return mapToResponseDto(savedAssurance);
	}

	@Override
	@Transactional(readOnly = true)
	public AssuranceResponseDto getById(Long id) {
		Assurance assurance = assuranceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Assurance", id));
		return mapToResponseDto(assurance);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "assurances", key = "'all'")
	public List<AssuranceResponseDto> getAll() {
		return assuranceRepository.findAll().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<AssuranceResponseDto> getPage(int page, int size) {
		Page<Assurance> result = assuranceRepository.findAll(PageRequests.of(page, size));
		return PageResponse.from(result.map(this::mapToResponseDto));
	}

	@Override
	@CacheEvict(value = "assurances", allEntries = true)
	public AssuranceResponseDto update(Long id, AssuranceCreateDto assuranceCreateDto) {
		Assurance assurance = assuranceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Assurance", id));

		// Vérifier si le nom est modifié et s'il existe déjà
		if (!assurance.getName().equals(assuranceCreateDto.getName()) 
				&& assuranceRepository.existsByName(assuranceCreateDto.getName())) {
			throw new IllegalArgumentException("Une assurance avec le nom '" + assuranceCreateDto.getName() + "' existe déjà");
		}

		assurance.setName(assuranceCreateDto.getName());
		assurance.setDescription(assuranceCreateDto.getDescription());

		Assurance updatedAssurance = assuranceRepository.save(assurance);
		return mapToResponseDto(updatedAssurance);
	}

	@Override
	@CacheEvict(value = "assurances", allEntries = true)
	public void delete(Long id) {
		if (!assuranceRepository.existsById(id)) {
			throw new ResourceNotFoundException("Assurance", id);
		}
		assuranceRepository.deleteById(id);
	}

	private AssuranceResponseDto mapToResponseDto(Assurance assurance) {
		AssuranceResponseDto dto = new AssuranceResponseDto();
		dto.setId(assurance.getId());
		dto.setName(assurance.getName());
		dto.setDescription(assurance.getDescription());
		return dto;
	}
}

