package com.lifeos.shared.storage;

import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class FileTypeDetector {
    private final Tika tika = new Tika();

    public String detect(InputStream inputStream, String filename) throws IOException {
        return tika.detect(inputStream, filename);
    }
}
