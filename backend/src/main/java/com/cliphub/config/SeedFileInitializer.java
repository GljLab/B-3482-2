package com.cliphub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class SeedFileInitializer implements CommandLineRunner {

    @Value("${app.storage.root}")
    private String storageRoot;

    @Override
    public void run(String... args) {
        Path seedDir = Paths.get(storageRoot, "uploads", "seed");
        try {
            Files.createDirectories(seedDir);
            writeIfAbsent(seedDir.resolve("city-night-time-lapse.mp4"), "demo-video-content");
            writeIfAbsent(seedDir.resolve("upbeat-background-track.mp3"), "demo-audio-content");
            writeIfAbsent(seedDir.resolve("product-unbox-template.json"), "{\"template\":\"demo\"}");
        } catch (IOException ex) {
            log.warn("Initialize seed files failed: {}", ex.getMessage());
        }
    }

    private void writeIfAbsent(Path path, String content) throws IOException {
        if (Files.notExists(path)) {
            Files.writeString(path, content);
        }
    }
}
