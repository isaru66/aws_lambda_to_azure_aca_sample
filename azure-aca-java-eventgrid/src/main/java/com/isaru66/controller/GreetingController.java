package com.isaru66.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

	@GetMapping("/")
	String home() {
		return "Azure Aca Blob Spring Boot Application is running!";
	}
}