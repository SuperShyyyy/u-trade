package com.sec.service;

import com.sec.domain.es.ItemDocument;
import org.springframework.data.domain.Page;

public interface ItemEsService {

    void save(ItemDocument item);
    void delete(Long id);
    Page<ItemDocument> searchByTitle(String keyword, int page, int size);
}