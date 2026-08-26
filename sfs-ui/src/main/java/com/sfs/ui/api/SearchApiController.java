package com.sfs.ui.api;

import com.sfs.app.api.request.SearchApiRequest;
import com.sfs.app.api.response.SearchApiResponse;
import com.sfs.app.service.SearchApplicationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SearchApiController {

    private final SearchApplicationService searchApplicationService;

    public SearchApiController(SearchApplicationService searchApplicationService) {
        this.searchApplicationService = searchApplicationService;
    }

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SearchApiResponse search(@RequestBody SearchApiRequest request) {
        return searchApplicationService.search(request);
    }

    @GetMapping("/search")
    public SearchApiResponse searchByQuery(
            @RequestParam("q") String query,
            @RequestParam(name = "maxResults", required = false) Integer maxResults) {

        return searchApplicationService.search(new SearchApiRequest(query, maxResults));
    }
}
