package com.buddies.pharmaciegarde.config.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI pharmacieGardeOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("API Pharmacie de Garde")
						.description("API REST Pharmacie de Garde. "
								+ "Les lectures publiques (pharmacies, gardes courantes, urgences, zones) restent ouvertes. "
								+ "Le back-office envoie Authorization: Bearer <token> obtenu via POST /api/auth/login. "
								+ "L'app mobile obtient son jeton via POST /api/auth/client/login. "
								+ "Les listes historiques restent des tableaux JSON ; ajoutez page ou size pour une page {content, page, size, totalElements, totalPages}. "
								+ "Import d'affiches : POST /api/duty-files/{id}/preview puis POST /api/duty-files/{id}/confirm.")
						.version("v1.1")
						.contact(new Contact()
								.name("Buddies")
								.email("contact@buddies.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
				.addServersItem(new Server()
						.url("http://localhost:8080")
						.description("Serveur de développement"))
				.components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
	}
}

