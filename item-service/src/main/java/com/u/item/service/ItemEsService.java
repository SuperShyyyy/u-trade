package com.u.item.service;

import com.u.item.domain.es.ItemDocument;
import org.springframework.data.domain.Page;

public interface ItemEsService {

    void save(ItemDocument item);
    void delete(Long id);
    Page<ItemDocument> searchByTitle(String keyword, int page, int size);
}