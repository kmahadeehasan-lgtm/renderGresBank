package com.izak.demoBankManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoBankManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoBankManagementApplication.class, args);
	}

}
