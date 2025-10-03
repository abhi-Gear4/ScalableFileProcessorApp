package com.pingAssignment.scalablefileprocessor;

import com.pingAssignment.scalablefileprocessor.model.BadData;
import com.pingAssignment.scalablefileprocessor.model.User;
import com.pingAssignment.scalablefileprocessor.repository.BadDataRepository;
import com.pingAssignment.scalablefileprocessor.repository.UserRepository;
import com.pingAssignment.scalablefileprocessor.service.FileProcessorService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Testcontainers
class ScalablefileprocessorApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private FileProcessorService fileProcessor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BadDataRepository badDataRepository;

    @BeforeAll
    public static void setUp() {
        if (!postgres.isRunning()) {
            postgres.start();
        }
    }

    @BeforeEach
    void cleanBefore() {
        userRepository.deleteAll();
        badDataRepository.deleteAll();
    }

    @AfterEach
    public void cleanUp() {
        userRepository.deleteAll();
        badDataRepository.deleteAll();
    }


    @Test
    void testProcessFile_withValidAndInvalidRecords() throws Exception {
        // Arrange
        Path tempFile = Files.createTempFile("test", ".jsonl");
        List<String> records = List.of(
                "{\"first_name\":\"John\",\"middle_name\":\"A\",\"last_name\":\"Smith\",\"date_of_birth\":\"1985-04-12\",\"address\":{\"street\":\"123 Main St\",\"city\":\"Springfield\",\"state\":\"IL\",\"postal_code\":\"62704\",\"country\":\"USA\"}}",
                "{\"first_name\":\"Priya\",\"middle_name\":null,\"date_of_birth\":\"1990-08-25\",\"address\":{\"street\":\"45 MG Road\",\"city\":\"Bengaluru\",\"state\":\"Karnataka\",\"postal_code\":\"\",\"country\":\"India\"}}"  // Invalid postal_code
        );
        Files.write(tempFile, records);

        // Act
        fileProcessor.processFile(tempFile);

        // Assert
        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
        assertEquals("Smith", users.get(0).getLastName());

        List<BadData> badData = badDataRepository.findAll();
        assertEquals(1, badData.size());
        assertTrue(badData.get(0).getRawRecord().contains("Priya"));
    }

    @Test
    void testProcessingFile_allValid() throws Exception {
        Path tempFile = Files.createTempFile("test-all-valid", ".jsonl");
        List<String> records = List.of(
                "{\"first_name\":\"Alice\",\"middle_name\":null,\"last_name\":\"Brown\",\"date_of_birth\":\"1988-10-10\",\"address\":{\"street\":\"12 Oak Ave\",\"city\":\"Austin\",\"state\":\"TX\",\"postal_code\":\"73301\",\"country\":\"USA\"}}",
                "{\"first_name\":\"Bob\",\"middle_name\":\"M\",\"last_name\":\"Johnson\",\"date_of_birth\":\"1992-05-15\",\"address\":{\"street\":\"45 Pine Rd\",\"city\":\"Dallas\",\"state\":\"TX\",\"postal_code\":\"75201\",\"country\":\"USA\"}}"
        );
        Files.write(tempFile, records);

        fileProcessor.processFile(tempFile);

        List<User> users = userRepository.findAll();
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getLastName().equals("Brown")));
        assertTrue(users.stream().anyMatch(u -> u.getLastName().equals("Johnson")));
    }

    @Test
    void testProcessingFile_allInvalid() throws Exception {
        Path tempFile = Files.createTempFile("test-all-invalid", ".jsonl");
        List<String> records = List.of(
                "{\"first_name\":\"Charlie\",\"last_name\":\"\",\"date_of_birth\":\"1995-07-20\",\"address\":{\"street\":\"\",\"city\":\"\",\"state\":\"\",\"postal_code\":\"\",\"country\":\"\"}}",
                "{\"first_name\":\"Diana\",\"last_name\":null,\"date_of_birth\":\"\",\"address\":{\"street\":\"\",\"city\":\"\",\"state\":\"\",\"postal_code\":\"\",\"country\":\"\"}}"
        );
        Files.write(tempFile, records);

        fileProcessor.processFile(tempFile);

        List<User> users = userRepository.findAll();
        List<BadData> badDataList = badDataRepository.findAll();
        assertEquals(0, users.size());
        assertEquals(2, badDataList.size());
    }

    @Test
    void testProcessingFile_malformedJson() throws Exception {
        Path tempFile = Files.createTempFile("test-malformed", ".jsonl");
        List<String> records = List.of(
                "{\"first_name\":\"Eve\",\"last_name\":\"Smith\",\"date_of_birth\":\"1970-01-01\",\"address\":{\"street\":\"123 Elm St\",\"city\":\"NY\",\"state\":\"NY\",\"postal_code\":\"10001\",\"country\":\"USA\"}}",
                "{\"first_name\":\"Faulty\", \"last_name\":\"Record\", \"date_of_birth\":\"1980-12-12\", \"address\": { street: 321 }"  // Missing quotes around street key, malformed
        );
        Files.write(tempFile, records);

        fileProcessor.processFile(tempFile);

        List<User> users = userRepository.findAll();
        List<BadData> badDataList = badDataRepository.findAll();
        assertEquals(1, users.size());
        assertEquals(1, badDataList.size());
        assertTrue(badDataList.get(0).getRawRecord().contains("Faulty"));
    }

    @Test
    void testProcessAllRealJsonLFilesInDirectory() throws Exception {

        /**
         * Integration test to verify processing all JSON Lines (.jsonl) files present in the
         * 'jsonldata-directory' resource folder on the classpath. This test exercises the full
         * end-to-end file processing, including validation, batching, and persistence logic.
         * The JSONL files (e.g., sample1.jsonl, sample2.jsonl, sample3.jsonl, sample4.jsonl) each
         * contain 10 JSON lines of data.
         *
         * Since the resources are packaged inside the jar or container, the test loads the
         * resource directory using the classloader to ensure compatibility both in development
         * and when running from a packaged jar.
         *
         * The expected values for total valid users and total bad data records are derived
         * from a manual count of the valid and invalid records present in the JSONL files.
         *
         * - expectedTotalValidUsers = 29  // Number of records with valid mandatory fields
         * - expectedTotalBadData = 11     // Number of records missing mandatory fields or invalid
         *
         * The test asserts that the total number of User entities saved matches the expected
         * valid user count and that all bad records are correctly persisted as BadData entities.
         */


        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceUrl = classLoader.getResource("jsonldata-directory");
        assertNotNull(resourceUrl, "Resource directory not found");

        Path dir;
        try {
            dir = Paths.get(resourceUrl.toURI());
        } catch (FileSystemNotFoundException e) {
            // Resource is inside a jar, so copy files to temp directory
            FileSystem fileSystem = FileSystems.newFileSystem(resourceUrl.toURI(), Collections.emptyMap());
            dir = fileSystem.getPath("jsonldata-directory");
        }
        fileProcessor.processAllFilesInDirectory(dir);

        //
        int expectedTotalValidUsers = 29;
        int expectedTotalBadData =11;

        // Assert total expected users and bad data from all files combined
        List<User> users = userRepository.findAll();
        List<BadData> badData = badDataRepository.findAll();

        assertEquals(expectedTotalValidUsers, users.size());
        assertEquals(expectedTotalBadData, badData.size());
    }

    @AfterAll
    public static void tearDown() {
        postgres.stop(); // this stops and removes the container
    }

}
