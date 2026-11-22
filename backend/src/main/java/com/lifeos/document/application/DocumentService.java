package com.lifeos.document.application;

import com.lifeos.activity.application.AuditService;
import com.lifeos.document.domain.Document;
import com.lifeos.document.domain.DocumentReviewStatus;
import com.lifeos.document.domain.DocumentStatus;
import com.lifeos.document.domain.ExtractionJob;
import com.lifeos.document.domain.RecordDocument;
import com.lifeos.document.infra.DocumentRepository;
import com.lifeos.document.infra.ExtractionJobRepository;
import com.lifeos.document.infra.RecordDocumentRepository;
import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.domain.HouseholdRole;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.record.infra.RecordRepository;
import com.lifeos.shared.error.ApiException;
import com.lifeos.shared.observability.LifeOsMetrics;
import com.lifeos.shared.storage.FileTypeDetector;
import com.lifeos.shared.storage.StorageService;
import com.lifeos.shared.time.TimeProvider;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

@Service
public class DocumentService {
    private static final String ENTITY_TYPE = "DOCUMENT";
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME = Set.of("application/pdf", "image/png", "image/jpeg", "text/plain");

    private final DocumentRepository documentRepository;
    private final RecordDocumentRepository recordDocumentRepository;
    private final ExtractionJobRepository extractionJobRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final RecordRepository recordRepository;
    private final StorageService storageService;
    private final FileTypeDetector fileTypeDetector;
    private final AuditService auditService;
    private final TimeProvider timeProvider;
    private final LifeOsMetrics metrics;

    public DocumentService(DocumentRepository documentRepository, RecordDocumentRepository recordDocumentRepository,
                           ExtractionJobRepository extractionJobRepository, HouseholdMembershipRepository membershipRepository,
                           RecordRepository recordRepository, StorageService storageService,
                           FileTypeDetector fileTypeDetector, AuditService auditService, TimeProvider timeProvider,
                           LifeOsMetrics metrics) {
        this.documentRepository = documentRepository;
        this.recordDocumentRepository = recordDocumentRepository;
        this.extractionJobRepository = extractionJobRepository;
        this.membershipRepository = membershipRepository;
        this.recordRepository = recordRepository;
        this.storageService = storageService;
        this.fileTypeDetector = fileTypeDetector;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentResponse> list(UUID userId, DocumentStatus status) {
        HouseholdMembership membership = requireMembership(userId);
        List<Document> docs = status == null
                ? documentRepository.findByHouseholdIdOrderByCreatedAtDesc(membership.getHouseholdId())
                : documentRepository.findByHouseholdIdAndExtractionStatusOrderByCreatedAtDesc(membership.getHouseholdId(), status);
        return docs.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DocumentDtos.DocumentResponse upload(UUID userId, MultipartFile file) {
        HouseholdMembership membership = requireWritableMembership(userId);
        if (file == null || file.isEmpty()) {
            metrics.documentUpload("rejected");
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "File is required");
        }
        if (file.getSize() > MAX_SIZE) {
            metrics.documentUpload("rejected");
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "File exceeds max size");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String detectedMime = detectMime(file, originalFilename);
        if (!ALLOWED_MIME.contains(detectedMime)) {
            metrics.documentUpload("rejected");
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE", "File type is not supported");
        }

        UUID docId = UUID.randomUUID();
        String ext = extensionFor(detectedMime, originalFilename);
        String storageKey = membership.getHouseholdId() + "/" + docId + ext;
        String checksum;
        long storedSize;
        try (InputStream in = file.getInputStream(); DigestInputStream din = new DigestInputStream(in, sha256())) {
            StorageService.StoredObject stored = storageService.store(storageKey, din, file.getSize(), detectedMime);
            storedSize = stored.sizeBytes();
            checksum = HexFormat.of().formatHex(din.getMessageDigest().digest());
        } catch (IOException e) {
            metrics.documentUpload("storage_error");
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", "Failed to store file");
        }

        DocumentStatus extractionStatus = DocumentStatus.PENDING;
        Document document = new Document(membership.getHouseholdId(), userId, originalFilename, storageKey, detectedMime, storedSize, checksum, extractionStatus);
        document.setId(docId);
        documentRepository.save(document);

        ExtractionJob job = new ExtractionJob(UUID.randomUUID(), membership.getHouseholdId(), docId, extractionStatus, timeProvider.now());
        extractionJobRepository.save(job);

        runExtraction(document, job);

        auditService.record(membership.getHouseholdId(), userId, ENTITY_TYPE, docId, "DOCUMENT_UPLOADED",
                java.util.Map.of("filename", originalFilename, "mimeType", detectedMime));
        metrics.documentUpload("stored");
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public DocumentDtos.DocumentResponse get(UUID userId, UUID documentId) {
        HouseholdMembership membership = requireMembership(userId);
        return toResponse(requireDocument(membership.getHouseholdId(), documentId));
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(UUID userId, UUID documentId) {
        HouseholdMembership membership = requireMembership(userId);
        Document doc = requireDocument(membership.getHouseholdId(), documentId);
        try {
            return new DocumentDownload(doc.getOriginalFilename(), doc.getMimeType(), storageService.load(doc.getStorageKey()));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", "Failed to read file");
        }
    }

    @Transactional
    public void delete(UUID userId, UUID documentId) {
        HouseholdMembership membership = requireWritableMembership(userId);
        Document doc = requireDocument(membership.getHouseholdId(), documentId);
        recordDocumentRepository.deleteByHouseholdIdAndDocumentId(membership.getHouseholdId(), documentId);
        documentRepository.delete(doc);
        try { storageService.delete(doc.getStorageKey()); } catch (IOException ignored) {}
        auditService.record(membership.getHouseholdId(), userId, ENTITY_TYPE, documentId, "DOCUMENT_DELETED", java.util.Map.of("filename", doc.getOriginalFilename()));
    }

    @Transactional
    public DocumentDtos.DocumentLinkResponse linkToRecord(UUID userId, UUID documentId, DocumentDtos.LinkDocumentRequest request) {
        HouseholdMembership membership = requireWritableMembership(userId);
        Document doc = requireDocument(membership.getHouseholdId(), documentId);
        recordRepository.findByIdAndHouseholdId(request.recordId(), membership.getHouseholdId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECORD_NOT_FOUND", "Record not found"));
        String linkType = request.linkType().trim().toUpperCase(Locale.ROOT);
        if (recordDocumentRepository.existsByRecordIdAndDocumentIdAndLinkType(request.recordId(), documentId, linkType)) {
            throw new ApiException(HttpStatus.CONFLICT, "LINK_EXISTS", "Document already linked to record");
        }
        RecordDocument link = new RecordDocument(membership.getHouseholdId(), request.recordId(), doc.getId(), linkType);
        recordDocumentRepository.save(link);
        auditService.record(membership.getHouseholdId(), userId, ENTITY_TYPE, documentId, "DOCUMENT_LINKED",
                java.util.Map.of("recordId", request.recordId().toString(), "linkType", linkType));
        return new DocumentDtos.DocumentLinkResponse(link.getId(), link.getRecordId(), link.getDocumentId(), link.getLinkType(), link.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentLinkResponse> links(UUID userId, UUID documentId) {
        HouseholdMembership membership = requireMembership(userId);
        requireDocument(membership.getHouseholdId(), documentId);
        return recordDocumentRepository.findByHouseholdIdAndDocumentId(membership.getHouseholdId(), documentId).stream()
                .map(l -> new DocumentDtos.DocumentLinkResponse(l.getId(), l.getRecordId(), l.getDocumentId(), l.getLinkType(), l.getCreatedAt()))
                .toList();
    }

    private void runExtraction(Document document, ExtractionJob job) {
        job.setStatus(DocumentStatus.PROCESSING);
        job.setStartedAt(timeProvider.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        document.setExtractionStatus(DocumentStatus.PROCESSING);
        try (InputStream in = storageService.load(document.getStorageKey())) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(20000);
            Metadata metadata = new Metadata();
            parser.parse(in, handler, metadata);
            String text = handler.toString().trim();
            document.setExtractedText(text.isBlank() ? null : text);
            document.setExtractionStatus(DocumentStatus.READY);
            document.setExtractionError(null);
            job.setStatus(DocumentStatus.READY);
            job.setCompletedAt(timeProvider.now());
            job.setResultJson("{\"provider\":\"tika\"}");
        } catch (IOException | SAXException | TikaException | RuntimeException e) {
            document.setExtractionStatus(DocumentStatus.FAILED);
            document.setExtractionError("Extraction failed");
            job.setStatus(DocumentStatus.FAILED);
            job.setCompletedAt(timeProvider.now());
            job.setErrorCode("EXTRACTION_FAILED");
        }
        documentRepository.save(document);
        extractionJobRepository.save(job);
    }

    private String detectMime(MultipartFile file, String filename) {
        try (InputStream in = file.getInputStream()) {
            String detected = fileTypeDetector.detect(in, filename);
            if (detected == null) return normalizeMime(file.getContentType(), filename);
            if (detected.startsWith("text/")) return "text/plain";
            if (detected.equals("application/pdf") || detected.equals("image/png") || detected.equals("image/jpeg")) return detected;
            return normalizeMime(detected, filename);
        } catch (IOException e) {
            return normalizeMime(file.getContentType(), filename);
        }
    }

    private HouseholdMembership requireMembership(UUID userId) {
        return membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "HOUSEHOLD_REQUIRED", "Household membership required"));
    }

    private HouseholdMembership requireWritableMembership(UUID userId) {
        HouseholdMembership membership = requireMembership(userId);
        if (membership.getRole() == HouseholdRole.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Viewer role cannot modify documents");
        }
        return membership;
    }

    private Document requireDocument(UUID householdId, UUID documentId) {
        return documentRepository.findByIdAndHouseholdId(documentId, householdId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found"));
    }

    private DocumentDtos.DocumentResponse toResponse(Document d) {
        return new DocumentDtos.DocumentResponse(d.getId(), d.getHouseholdId(), d.getUploadedByUserId(), d.getOriginalFilename(),
                d.getMimeType(), d.getSizeBytes(), d.getChecksumSha256(), d.getExtractionStatus(), d.getExtractedText(),
                d.getExtractionError(), d.getReviewStatus(), d.getCreatedAt(), d.getUpdatedAt());
    }

    private String sanitizeFilename(String filename) {
        String name = filename == null || filename.isBlank() ? "upload" : filename.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank()) name = "upload";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String normalizeMime(String contentType, String filename) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
        String lower = filename.toLowerCase(Locale.ROOT);
        if (ct.startsWith("text/plain") || lower.endsWith(".txt")) return "text/plain";
        if (ct.equals("application/pdf") || lower.endsWith(".pdf")) return "application/pdf";
        if (ct.equals("image/png") || lower.endsWith(".png")) return "image/png";
        if (ct.equals("image/jpeg") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return ct;
    }

    private String extensionFor(String mimeType, String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (mimeType.equals("application/pdf") || lower.endsWith(".pdf")) return ".pdf";
        if (mimeType.equals("image/png") || lower.endsWith(".png")) return ".png";
        if (mimeType.equals("image/jpeg") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ".jpg";
        if (mimeType.equals("text/plain") || lower.endsWith(".txt")) return ".txt";
        return "";
    }

    private MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }

    public record DocumentDownload(String filename, String mimeType, InputStream inputStream) {}
}
