package com.lifeos.search.application;

import com.lifeos.document.infra.DocumentRepository;
import com.lifeos.household.domain.HouseholdMembership;
import com.lifeos.household.infra.HouseholdMembershipRepository;
import com.lifeos.obligation.infra.ObligationRepository;
import com.lifeos.record.infra.RecordRepository;
import com.lifeos.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {
    private final HouseholdMembershipRepository membershipRepository;
    private final RecordRepository recordRepository;
    private final DocumentRepository documentRepository;
    private final ObligationRepository obligationRepository;

    public SearchService(HouseholdMembershipRepository membershipRepository, RecordRepository recordRepository,
                         DocumentRepository documentRepository, ObligationRepository obligationRepository) {
        this.membershipRepository = membershipRepository;
        this.recordRepository = recordRepository;
        this.documentRepository = documentRepository;
        this.obligationRepository = obligationRepository;
    }

    @Transactional(readOnly = true)
    public SearchDtos.SearchResponse search(UUID userId, String q) {
        HouseholdMembership m = membershipRepository.findByUserId(userId).stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "HOUSEHOLD_REQUIRED", "Household membership required"));
        String query = q == null ? "" : q.trim();
        if (query.isBlank()) return new SearchDtos.SearchResponse(List.of(), List.of(), List.of());
        UUID hid = m.getHouseholdId();

        List<SearchDtos.SearchItem> records = recordRepository.search(hid, null, null, query).stream().limit(10)
                .map(r -> new SearchDtos.SearchItem(r.getId(), "RECORD", r.getTitle(), r.getCategory())).toList();
        List<SearchDtos.SearchItem> documents = documentRepository.findByHouseholdIdOrderByCreatedAtDesc(hid).stream()
                .filter(d -> contains(d.getOriginalFilename(), query) || contains(d.getExtractedText(), query))
                .limit(10)
                .map(d -> new SearchDtos.SearchItem(d.getId(), "DOCUMENT", d.getOriginalFilename(), d.getMimeType())).toList();
        List<SearchDtos.SearchItem> obligations = obligationRepository.findByHouseholdIdAndTitleContainingIgnoreCase(hid, query).stream().limit(10)
                .map(o -> new SearchDtos.SearchItem(o.getId(), "OBLIGATION", o.getTitle(), o.getStatus().name())).toList();
        return new SearchDtos.SearchResponse(records, documents, obligations);
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q.toLowerCase());
    }
}
