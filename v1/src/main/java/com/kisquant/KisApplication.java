package com.kisquant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KisApplication {

	public static void main(String[] args) {
		SpringApplication.run(KisApplication.class, args);
	}
}
