package com.buddies.pharmaciegarde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PharmaciegardeApplication {

	public static void main(String[] args) {
		SpringApplication.run(PharmaciegardeApplication.class, args);
	}

}
