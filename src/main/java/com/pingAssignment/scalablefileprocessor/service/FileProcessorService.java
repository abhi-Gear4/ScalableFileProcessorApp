package com.pingAssignment.scalablefileprocessor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingAssignment.scalablefileprocessor.model.Address;
import com.pingAssignment.scalablefileprocessor.model.BadData;
import com.pingAssignment.scalablefileprocessor.model.User;
import com.pingAssignment.scalablefileprocessor.repository.AddressRepository;
import com.pingAssignment.scalablefileprocessor.repository.BadDataRepository;
import com.pingAssignment.scalablefileprocessor.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileProcessorService {

    // batch size for saving users
    @Value("${fileprocessor.batchSize}")
    private int batchSize;

    // throttling rate for files
    @Value("${fileprocessor.maxFilesPerMinute}")
    private int maxFilesPerMinute;

    // thread pool for async processing
    @Autowired
    @Qualifier("fileProcessingExecutor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private AddressRepository addressRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private BadDataRepository badDataRepo;

    /**
     * Processes all .jsonl files found in the given directory.
     * Validates that directory exists and gathers all files before processing.
     *
     * @param dirPath Path to directory containing .jsonl files.
     * @throws IOException if directory listing or reading fails.
     * @throws InterruptedException if thread sleep is interrupted during throttling.
     */
    public void processAllFilesInDirectory(Path dirPath) throws IOException, InterruptedException {
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Not a directory: " + dirPath.toString());
        }
        List<Path> files = Files.list(dirPath)
                .filter(f -> f.toString().endsWith(".jsonl"))
                .collect(Collectors.toList());
        processFiles(files);
    }

    /**
     * Processes a given list of file paths asynchronously with throttling
     * according to maxFilesPerMinute property.
     * Uses CompletableFuture with a thread pool executor.
     *
     * @param files List of file paths to process.
     * @throws InterruptedException if thread sleep is interrupted during throttling.
     */
    @Transactional
    public void processFiles(List<Path> files) throws InterruptedException {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long intervalMillis = 60000 / maxFilesPerMinute;

        for (Path filePath : files) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processFile(filePath), executor);
            futures.add(future);

            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Processes a single JSON lines file containing user data.
     * Each line is parsed and validated; valid user records are batched and saved.
     * Invalid or malformed records are stored as BadData entities.
     *
     * @param filePath Path to the .jsonl file to process.
     */
    public void processFile(Path filePath) {
        ObjectMapper mapper = new ObjectMapper();

        try (Stream<String> lines = Files.lines(filePath)) {
            List<User> userBatch = new ArrayList<>();
            List<BadData> badRecords = new ArrayList<>();

            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                try {
                    JsonNode node = mapper.readTree(line);

                    Optional<BadData> badDataOpt = validateRecord(line, node);
                    if (badDataOpt.isPresent()) {
                        badRecords.add(badDataOpt.get());
                        continue; // Skip to next line
                    }

                    Address address = getOrCreateAddress(node.get("address"));
                    User user = new User();
                    user.setFirstName(node.hasNonNull("first_name") ? node.get("first_name").asText() : null);
                    user.setMiddleName(node.hasNonNull("middle_name") && !node.get("middle_name").isNull()
                            ? node.get("middle_name").asText() : null);
                    user.setLastName(node.get("last_name").asText());
                    user.setDateOfBirth(LocalDate.parse(node.get("date_of_birth").asText()));
                    user.setAddress(address);
                    userBatch.add(user);

                    if (userBatch.size() >= batchSize) {
                        userRepo.saveAll(userBatch);
                        userBatch.clear();
                    }
                } catch (Exception e) {
                    badRecords.add(new BadData(line, "Exception during processing: " + e.getMessage()));
                }
            }

            if (!userBatch.isEmpty())
                userRepo.saveAll(userBatch);
            if (!badRecords.isEmpty())
                badDataRepo.saveAll(badRecords);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Address getOrCreateAddress(JsonNode addressNode) {
        String street = getSafeText(addressNode, "street");
        String city = getSafeText(addressNode, "city");
        String state = getSafeText(addressNode, "state");
        String postalCode = getSafeText(addressNode, "postalCode");
        String country = getSafeText(addressNode, "country");

        Optional<Address> optAddr = addressRepo.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                street, city, state, postalCode, country);
        if (optAddr.isPresent()) {
            return optAddr.get();
        } else {
            Address address = new Address();
            address.setStreet(street);
            address.setCity(city);
            address.setState(state);
            address.setPostalCode(postalCode);
            address.setCountry(country);
            return addressRepo.save(address);
        }
    }

    protected String getSafeText(JsonNode node, String fieldName) {
        if (node == null) return null;
        JsonNode fnode = node.get(fieldName);
        if (fnode == null || fnode.isNull()) return null;
        String val = fnode.asText();
        return val.isEmpty() ? null : val;
    }

    private Optional<BadData> validateRecord(String line, JsonNode node) {
        String lastName = node.hasNonNull("last_name") ? node.get("last_name").asText() : null;
        String dob = node.hasNonNull("date_of_birth") ? node.get("date_of_birth").asText() : null;
        JsonNode addressNode = node.get("address");
        String postalCode = (addressNode != null && addressNode.hasNonNull("postal_code"))
                ? addressNode.get("postal_code").asText()
                : null;

        List<String> missingFields = new ArrayList<>();
        if (lastName == null || lastName.isEmpty()) missingFields.add("last_name");
        if (dob == null || dob.isEmpty()) missingFields.add("date_of_birth");
        if (postalCode == null || postalCode.isEmpty()) missingFields.add("postal_code");

        if (!missingFields.isEmpty()) {
            String errorReason = "Missing mandatory fields: " + String.join(", ", missingFields);
            return Optional.of(new BadData(line, errorReason));
        }

        return Optional.empty();
    }
}
