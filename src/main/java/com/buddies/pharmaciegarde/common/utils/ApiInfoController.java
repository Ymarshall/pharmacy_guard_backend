package com.buddies.pharmaciegarde.common.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "API Info", description = "Informations sur l'API")
public class ApiInfoController {

	@Operation(summary = "Informations sur l'API", description = "Retourne les informations sur les endpoints disponibles")
	@GetMapping
	@Hidden
	public ResponseEntity<Map<String, Object>> getApiInfo() {
		Map<String, Object> apiInfo = new HashMap<>();
		apiInfo.put("message", "API Pharmacie de Garde");
		apiInfo.put("version", "v1.0");
		Map<String, String> endpoints = new HashMap<>();
		endpoints.put("pharmacies", "/api/pharmacies");
		endpoints.put("swagger-ui", "/swagger-ui.html");
		endpoints.put("api-docs", "/api-docs");
		apiInfo.put("endpoints", endpoints);
		apiInfo.put("documentation", "Consultez /swagger-ui.html pour la documentation complète de l'API");
		return ResponseEntity.ok(apiInfo);
	}
}

