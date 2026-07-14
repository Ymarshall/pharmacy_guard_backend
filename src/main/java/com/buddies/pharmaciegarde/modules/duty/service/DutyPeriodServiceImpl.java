package com.buddies.pharmaciegarde.modules.duty.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buddies.pharmaciegarde.common.exception.ResourceNotFoundException;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodCreateDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodResponseDto;
import com.buddies.pharmaciegarde.modules.duty.dto.DutyPeriodUpdateDto;
import com.buddies.pharmaciegarde.modules.duty.model.DutyPeriod;
import com.buddies.pharmaciegarde.modules.duty.repository.DutyPeriodRepository;

@Service
@Transactional
public class DutyPeriodServiceImpl implements DutyPeriodService {

	private final DutyPeriodRepository dutyPeriodRepository;

	public DutyPeriodServiceImpl(DutyPeriodRepository dutyPeriodRepository) {
		this.dutyPeriodRepository = dutyPeriodRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyPeriodResponseDto> getAllPeriods() {
		return dutyPeriodRepository.findAll().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public DutyPeriodResponseDto getPeriodById(Long id) {
		DutyPeriod period = dutyPeriodRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Période de garde", id));
		return mapToResponseDto(period);
	}

	@Override
	public DutyPeriodResponseDto createPeriod(DutyPeriodCreateDto createDto) {
		// Valider que la date de fin est après la date de début
		if (createDto.getEndDate().isBefore(createDto.getStartDate())) {
			throw new IllegalArgumentException("La date de fin doit être après la date de début");
		}

		// Vérifier s'il y a des chevauchements
		List<DutyPeriod> overlapping = dutyPeriodRepository.findOverlappingPeriods(
				createDto.getStartDate(), createDto.getEndDate());
		if (!overlapping.isEmpty()) {
			throw new IllegalArgumentException("Une période de garde chevauche déjà cette période");
		}

		// Si on marque comme courante, désactiver TOUTES les autres gardes courantes
		// Il ne doit y avoir qu'une seule période active à la fois
		if (createDto.getIsCurrent() != null && createDto.getIsCurrent()) {
			List<DutyPeriod> currentPeriods = dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc();
			for (DutyPeriod currentPeriod : currentPeriods) {
				currentPeriod.setIsCurrent(false);
				dutyPeriodRepository.save(currentPeriod);
			}
		}

		DutyPeriod period = new DutyPeriod();
		period.setStartDate(createDto.getStartDate());
		period.setEndDate(createDto.getEndDate());
		period.setIsCurrent(createDto.getIsCurrent() != null ? createDto.getIsCurrent() : false);
		period.setDescription(createDto.getDescription());

		DutyPeriod savedPeriod = dutyPeriodRepository.save(period);
		return mapToResponseDto(savedPeriod);
	}

	@Override
	public DutyPeriodResponseDto updatePeriod(Long id, DutyPeriodUpdateDto updateDto) {
		DutyPeriod period = dutyPeriodRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Période de garde", id));

		if (updateDto.getStartDate() != null) {
			period.setStartDate(updateDto.getStartDate());
		}
		if (updateDto.getEndDate() != null) {
			period.setEndDate(updateDto.getEndDate());
		}
		if (updateDto.getDescription() != null) {
			period.setDescription(updateDto.getDescription());
		}
		if (updateDto.getIsCurrent() != null) {
			// Si on marque comme courante, désactiver TOUTES les autres gardes courantes
			// Il ne doit y avoir qu'une seule période active à la fois
			if (updateDto.getIsCurrent()) {
				List<DutyPeriod> currentPeriods = dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc();
				for (DutyPeriod currentPeriod : currentPeriods) {
					// Désactiver toutes les périodes courantes sauf celle qu'on met à jour
					if (!currentPeriod.getId().equals(id)) {
						currentPeriod.setIsCurrent(false);
						dutyPeriodRepository.save(currentPeriod);
					}
				}
			}
			period.setIsCurrent(updateDto.getIsCurrent());
		}

		// Valider que la date de fin est après la date de début
		if (period.getEndDate().isBefore(period.getStartDate())) {
			throw new IllegalArgumentException("La date de fin doit être après la date de début");
		}

		DutyPeriod updatedPeriod = dutyPeriodRepository.save(period);
		return mapToResponseDto(updatedPeriod);
	}

	@Override
	public void deletePeriod(Long id) {
		DutyPeriod period = dutyPeriodRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Période de garde", id));
		dutyPeriodRepository.delete(period);
	}

	@Override
	@Transactional(readOnly = true)
	public DutyPeriodResponseDto getCurrentPeriod() {
		DutyPeriod period = dutyPeriodRepository.findByIsCurrentTrue()
				.orElseThrow(() -> new ResourceNotFoundException("Aucune garde courante trouvée", null));
		return mapToResponseDto(period);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DutyPeriodResponseDto> getCurrentPeriods() {
		return dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public DutyPeriodResponseDto activatePeriod(Long id) {
		DutyPeriod period = dutyPeriodRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Période de garde", id));

		// Désactiver TOUTES les périodes courantes existantes (il ne doit y avoir qu'une seule période active à la fois)
		// Utiliser la méthode qui retourne une liste pour s'assurer de tout désactiver
		List<DutyPeriod> currentPeriods = dutyPeriodRepository.findByIsCurrentTrueOrderByStartDateDesc();
		for (DutyPeriod currentPeriod : currentPeriods) {
			// Désactiver toutes les périodes courantes, y compris celle qu'on va activer (pour éviter les doublons)
			currentPeriod.setIsCurrent(false);
			dutyPeriodRepository.save(currentPeriod);
		}

		// Activer la période demandée
		period.setIsCurrent(true);
		DutyPeriod updatedPeriod = dutyPeriodRepository.save(period);
		return mapToResponseDto(updatedPeriod);
	}

	private DutyPeriodResponseDto mapToResponseDto(DutyPeriod period) {
		DutyPeriodResponseDto dto = new DutyPeriodResponseDto();
		dto.setId(period.getId());
		dto.setStartDate(period.getStartDate());
		dto.setEndDate(period.getEndDate());
		dto.setIsCurrent(period.getIsCurrent());
		dto.setDescription(period.getDescription());
		return dto;
	}
}

