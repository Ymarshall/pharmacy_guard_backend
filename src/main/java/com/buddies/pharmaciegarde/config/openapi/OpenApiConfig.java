package com.buddies.pharmaciegarde.config.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI pharmacieGardeOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("API Pharmacie de Garde")
						.description("API REST pour la gestion des pharmacies de garde. "
								+ "Cette API permet de gérer les pharmacies via un back office et de fournir des services à une application mobile.")
						.version("v1.0")
						.contact(new Contact()
								.name("Buddies")
								.email("contact@buddies.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
				.addServersItem(new Server()
						.url("http://localhost:8080")
						.description("Serveur de développement"));
	}
}

