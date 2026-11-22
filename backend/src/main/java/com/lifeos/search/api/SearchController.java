package com.lifeos.search.api;

import com.lifeos.search.application.SearchDtos;
import com.lifeos.search.application.SearchService;
import com.lifeos.shared.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;
    public SearchController(SearchService searchService) { this.searchService = searchService; }

    @GetMapping
    public SearchDtos.SearchResponse search(@RequestParam(required = false) String q) {
        return searchService.search(SecurityUtils.currentUserId(), q);
    }
}
