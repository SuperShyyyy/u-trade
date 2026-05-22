package com.u.search.service;

import com.u.search.domain.es.ItemDocument;
import org.springframework.data.domain.Page;

public interface ItemEsService {

    void save(ItemDocument item);
    void delete(Long id);
    Page<ItemDocument> searchByTitle(String keyword, int page, int size);
}
