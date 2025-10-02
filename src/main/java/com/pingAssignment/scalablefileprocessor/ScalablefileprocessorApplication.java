package com.pingAssignment.scalablefileprocessor;

import com.pingAssignment.scalablefileprocessor.service.FileProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class ScalablefileprocessorApplication implements CommandLineRunner {

    @Autowired
    private FileProcessorService fileProcessorService;

	public static void main(String[] args) {
        SpringApplication.run(ScalablefileprocessorApplication.class, args);
        System.out.println("hipp hipp hurray...!!");
	}

    @Override
    public void run(String... args) throws Exception {
        Path dir = Paths.get("src/main/resources/jsonldata-directory");
        fileProcessorService.processAllFilesInDirectory(dir);
    }

}
