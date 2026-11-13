package com.buddies.pharmaciegarde.modules.pharmacy.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;

class PharmacyInitializationServiceTest {

	@Test
	void doesNotUpsertWhenSeedIsDisabled() {
		PharmacyRepository repository = mock(PharmacyRepository.class);
		PharmacyInitializationService service = new PharmacyInitializationService(repository, false);
		service.run();
		verifyNoInteractions(repository);
	}
}
