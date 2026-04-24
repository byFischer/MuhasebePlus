package com.MuhasebePlus.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MuhasebePlusApplication {

	public static void main(String[] args) {
		SpringApplication.run(MuhasebePlusApplication.class, args);
	}

}
