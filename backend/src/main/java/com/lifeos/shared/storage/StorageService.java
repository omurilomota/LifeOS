package com.lifeos.shared.storage;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    StoredObject store(String storageKey, InputStream content, long sizeBytes, String contentType) throws IOException;
    InputStream load(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;

    record StoredObject(String storageKey, long sizeBytes) {}
}
