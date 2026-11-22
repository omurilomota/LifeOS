package com.lifeos.search.application;

import java.util.List;
import java.util.UUID;

public class SearchDtos {
    private SearchDtos() {}
    public record SearchItem(UUID id, String type, String title, String subtitle) {}
    public record SearchResponse(List<SearchItem> records, List<SearchItem> documents, List<SearchItem> obligations) {}
}
