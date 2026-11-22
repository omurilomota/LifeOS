package com.lifeos.document.api;

import com.lifeos.document.application.DocumentDtos;
import com.lifeos.document.application.DocumentService;
import com.lifeos.document.domain.DocumentStatus;
import com.lifeos.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentDtos.DocumentResponse> list(@RequestParam(required = false) DocumentStatus status) {
        return documentService.list(SecurityUtils.currentUserId(), status);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDtos.DocumentResponse upload(@RequestParam("file") MultipartFile file) {
        return documentService.upload(SecurityUtils.currentUserId(), file);
    }

    @GetMapping("/{id}")
    public DocumentDtos.DocumentResponse get(@PathVariable UUID id) {
        return documentService.get(SecurityUtils.currentUserId(), id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) throws IOException {
        DocumentService.DocumentDownload download = documentService.download(SecurityUtils.currentUserId(), id);
        InputStream in = download.inputStream();
        InputStreamResource resource = new InputStreamResource(in);
        ContentDisposition disposition = ContentDisposition.attachment().filename(download.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        documentService.delete(SecurityUtils.currentUserId(), id);
    }

    @PostMapping("/{id}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDtos.DocumentLinkResponse link(@PathVariable UUID id, @Valid @RequestBody DocumentDtos.LinkDocumentRequest request) {
        return documentService.linkToRecord(SecurityUtils.currentUserId(), id, request);
    }

    @GetMapping("/{id}/links")
    public List<DocumentDtos.DocumentLinkResponse> links(@PathVariable UUID id) {
        return documentService.links(SecurityUtils.currentUserId(), id);
    }
}
