package com.buddies.pharmaciegarde.modules.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.buddies.pharmaciegarde.modules.user.model.User;
import com.buddies.pharmaciegarde.modules.user.model.UserRole;
import com.buddies.pharmaciegarde.modules.user.repository.UserRepository;

@Component
@Order(0)
public class AdminBootstrap implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;

	public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
			@Value("${app.admin.email:}") String email,
			@Value("${app.admin.password:}") String password) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(String... args) {
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			return;
		}
		if (userRepository.existsByEmail(email.trim())) {
			return;
		}
		User admin = new User();
		admin.setEmail(email.trim());
		admin.setPassword(passwordEncoder.encode(password));
		admin.setFirstName("Admin");
		admin.setLastName("Pharmacie");
		admin.setRole(UserRole.ADMIN);
		userRepository.save(admin);
		logger.info("Compte administrateur initial créé pour {}", email.trim());
	}
}
