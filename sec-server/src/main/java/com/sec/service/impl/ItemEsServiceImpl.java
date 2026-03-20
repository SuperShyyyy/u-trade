package com.sec.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.sec.constant.ItemStatusConstant;
import com.sec.domain.es.ItemDocument;
import com.sec.exception.BusinessException;
import com.sec.service.ItemEsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemEsServiceImpl implements ItemEsService {

    private final ElasticsearchClient esClient;

    @Value("${elasticsearch.index.item:item}")
    private String indexName;

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_DEPTH = 10000;

    /**
     * 索引初始化（带 mapping）
     */
    @PostConstruct
    public void init() {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                esClient.indices().create(c -> c
                        .index(indexName)
                        .mappings(m -> m
                                .properties("title", p -> p.text(t -> t.analyzer("ik_max_word")))
                                .properties("description", p -> p.text(t -> t.analyzer("ik_max_word")))
                                .properties("price", p -> p.double_(d -> d))
                                .properties("createTime", p -> p.date(d -> d))
                                .properties("status", p -> p.integer(i -> i))
                                .properties("isDeleted", p -> p.integer(i -> i))
                        )
                );
                log.info("ES索引 {} 创建成功（带mapping）", indexName);
            }
        } catch (IOException e) {
            log.error("ES索引初始化失败", e);
        }
    }

    /**
     * 新增 / 更新
     */
    @Override
    public void save(ItemDocument item) {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException("ItemDocument 或 ID 为空");
        }

        try {
            IndexResponse response = esClient.index(i -> i
                    .index(indexName)
                    .id(item.getId().toString())
                    .document(item)
            );

            log.info("ES index success, id={}, result={}", item.getId(), response.result());

        } catch (IOException e) {
            log.error("ES 保存失败", e);
            throw new BusinessException("ES服务异常");
        }
    }

    /**
     * 删除
     */
    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID 为空");
        }

        try {
            DeleteResponse response = esClient.delete(d -> d
                    .index(indexName)
                    .id(id.toString())
            );

            log.info("ES delete success, id={}, result={}", id, response.result());

        } catch (IOException e) {
            log.error("ES 删除失败", e);
            throw new BusinessException("ES服务异常");
        }
    }

    /**
     * 搜索
     */
    @Override
    public Page<ItemDocument> searchByTitle(String keyword, int page, int size) {

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词为空");
        }

        int currentPage = Math.max(page, 0);
        int currentSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int from = currentPage * currentSize;

        // 深分页提示
        if (from > MAX_DEPTH) {
            log.warn("深度分页 from={}，建议使用 search_after", from);
        }

        // 查询条件
        Query query = Query.of(q -> q
                .bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm
                                .query(keyword)
                                .fields("title^2", "description") // title权重更高
                        ))
                        .filter(f -> f.term(t -> t.field("status")
                                .value(ItemStatusConstant.ON_SALE)))
                        .filter(f -> f.term(t -> t.field("isDeleted")
                                .value(0)))
                )
        );

        try {
            SearchResponse<ItemDocument> response = esClient.search(s -> s
                            .index(indexName)
                            .query(query)
                            .from(from)
                            .size(currentSize)
                            .sort(sort -> sort.field(f -> f
                                    .field("createTime")
                                    .order(SortOrder.Desc)))
                    ,
                    ItemDocument.class
            );

            List<ItemDocument> items = response.hits().hits().stream()
                    .map(hit -> {
                        ItemDocument doc = hit.source();
                        if (doc != null && hit.id() != null) {
                            doc.setId(Long.valueOf(hit.id()));
                        }
                        return doc;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            long total = response.hits().total() != null
                    ? response.hits().total().value()
                    : 0;

            return new PageImpl<>(
                    items,
                    PageRequest.of(currentPage, currentSize),
                    total
            );

        } catch (IOException e) {
            log.error("ES 搜索失败", e);
            throw new BusinessException("搜索服务异常");
        }
    }
}