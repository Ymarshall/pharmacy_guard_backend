package com.buddies.pharmaciegarde.modules.duty.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.buddies.pharmaciegarde.modules.duty.dto.ExtractedPharmacyDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extracteur optionnel, compatible API OpenAI (vision). Désactivé par défaut.
 * La clé vient uniquement de LLM_VISION_API_KEY.
 */
@Service
@ConditionalOnProperty(prefix = "app.vision.llm", name = "enabled", havingValue = "true")
public class LlmVisionExtractor {

	private static final Logger logger = LoggerFactory.getLogger(LlmVisionExtractor.class);

	private final boolean enabled;
	private final String baseUrl;
	private final String apiKey;
	private final String model;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public LlmVisionExtractor(
			@Value("${app.vision.llm.enabled:false}") boolean enabled,
			@Value("${app.vision.llm.base-url:}") String baseUrl,
			@Value("${app.vision.llm.api-key:}") String apiKey,
			@Value("${app.vision.llm.model:gpt-4o-mini}") String model,
			ObjectMapper objectMapper) {
		this.enabled = enabled && baseUrl != null && !baseUrl.isBlank() && apiKey != null && !apiKey.isBlank();
		this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/$", "");
		this.apiKey = apiKey;
		this.model = model;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
		if (enabled && !this.enabled) {
			logger.warn("LLM vision demandé mais base URL ou clé absente : l'extracteur reste inactif");
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public List<ExtractedPharmacyDto> extract(byte[] fileBytes, String fileName) throws Exception {
		if (!enabled) {
			return List.of();
		}
		String mime = mime(fileName);
		String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(fileBytes);
		String prompt = """
				Extrais les pharmacies de garde de cette affiche ivoirienne.
				Réponds uniquement par un tableau JSON. Chaque objet contient :
				zone, pharmacyName, pharmacistName, phoneNumbers, location.
				N'invente aucune ligne absente de l'image.
				""";
		String body = objectMapper.writeValueAsString(java.util.Map.of(
				"model", model,
				"temperature", 0,
				"messages", List.of(java.util.Map.of(
						"role", "user",
						"content", List.of(
								java.util.Map.of("type", "text", "text", prompt),
								java.util.Map.of("type", "image_url", "image_url", java.util.Map.of("url", dataUrl)))))));
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/chat/completions"))
				.timeout(Duration.ofSeconds(60))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 300) {
			throw new IllegalStateException("Le service LLM a répondu " + response.statusCode());
		}
		JsonNode root = objectMapper.readTree(response.body());
		String content = root.path("choices").path(0).path("message").path("content").asText("");
		return parseModelJson(content, objectMapper);
	}

	static List<ExtractedPharmacyDto> parseModelJson(String content, ObjectMapper objectMapper) throws Exception {
		String json = content == null ? "" : content.trim();
		if (json.startsWith("```")) {
			json = json.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
		}
		int start = json.indexOf('[');
		int end = json.lastIndexOf(']');
		if (start >= 0 && end > start) {
			json = json.substring(start, end + 1);
		}
		JsonNode array = objectMapper.readTree(json);
		List<ExtractedPharmacyDto> rows = new ArrayList<>();
		if (!array.isArray()) {
			return rows;
		}
		for (JsonNode node : array) {
			String name = text(node, "pharmacyName");
			if (name.isBlank()) {
				continue;
			}
			ExtractedPharmacyDto dto = new ExtractedPharmacyDto();
			dto.setZone(text(node, "zone"));
			dto.setPharmacyName(name);
			dto.setPharmacistName(text(node, "pharmacistName"));
			dto.setPhoneNumbers(text(node, "phoneNumbers"));
			dto.setLocation(text(node, "location"));
			dto.setConfidence(0.8);
			rows.add(dto);
		}
		return rows;
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? "" : value.asText("").trim();
	}

	private static String mime(String fileName) {
		String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".png")) {
			return "image/png";
		}
		if (lower.endsWith(".pdf")) {
			return "application/pdf";
		}
		return "image/jpeg";
	}
}
