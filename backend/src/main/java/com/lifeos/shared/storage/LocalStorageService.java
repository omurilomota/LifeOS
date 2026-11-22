package com.lifeos.shared.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${lifeos.storage.root:./docker-data/storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public StoredObject store(String storageKey, InputStream content, long sizeBytes, String contentType) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        long actual = Files.size(target);
        return new StoredObject(storageKey, actual);
    }

    @Override
    public InputStream load(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) throws IOException {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Invalid storage key");
        }
        return target;
    }
}
