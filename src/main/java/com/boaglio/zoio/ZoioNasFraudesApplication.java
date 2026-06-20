package com.boaglio.zoio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZoioNasFraudesApplication {

	public static final String FRAUD = "fraud";

	void main(String[] args) {
		SpringApplication.run(ZoioNasFraudesApplication.class, args);
	}

}
