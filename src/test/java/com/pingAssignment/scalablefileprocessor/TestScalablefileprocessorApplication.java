package com.pingAssignment.scalablefileprocessor;

import org.springframework.boot.SpringApplication;

public class TestScalablefileprocessorApplication {

	public static void main(String[] args) {
		SpringApplication.from(ScalablefileprocessorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
