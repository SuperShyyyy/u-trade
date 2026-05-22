package com.u.search.controller;

import com.u.common.result.Result;
import com.u.search.domain.es.ItemDocument;
import com.u.search.service.ItemEsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ItemEsService itemEsService;

    @GetMapping("/item")
    public Result<Page<ItemDocument>> searchItem(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ItemDocument> result = itemEsService.searchByTitle(keyword, page, size);
        return Result.success(result);
    }
}
