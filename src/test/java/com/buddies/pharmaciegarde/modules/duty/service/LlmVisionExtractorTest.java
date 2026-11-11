package com.buddies.pharmaciegarde.modules.duty.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class LlmVisionExtractorTest {

	@Test
	void parsesJsonWrappedInMarkdown() throws Exception {
		String content = """
				```json
				[{"zone":"ABENGOUROU","pharmacyName":"PHARMACIE DU MARCHE","pharmacistName":"AWA","phoneNumbers":"27 35 91 30 09","location":"MARCHE"}]
				```
				""";
		var rows = LlmVisionExtractor.parseModelJson(content, new ObjectMapper());
		assertEquals(1, rows.size());
		assertEquals("PHARMACIE DU MARCHE", rows.get(0).getPharmacyName());
		assertEquals("ABENGOUROU", rows.get(0).getZone());
	}
}
