package com.sec.service;

import com.sec.constant.ItemStatusConstant;
import com.sec.domain.es.ItemDocument;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemEsService {

    private final ElasticsearchClient esClient;
    private static final String INDEX = "item";

    //新增或更新文档
    public void save(ItemDocument item) throws IOException {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException("ItemDocument 或 ID 为空");
        }

        IndexRequest<ItemDocument> request = IndexRequest.of(i -> i
                .index(INDEX)
                .id(item.getId().toString())
                .document(item)
        );
        IndexResponse response = esClient.index(request);
        log.info("ES 索引结果：" + response.result());
    }

    //删除文档
    public void delete(Long id) throws IOException {
        if (id == null) {
            throw new IllegalArgumentException("ID 为空，无法删除 ES 文档");
        }

        DeleteRequest request = DeleteRequest.of(d -> d
                .index(INDEX)
                .id(id.toString())
        );
        DeleteResponse response = esClient.delete(request);
        log.info("ES 删除结果：" + response.result());
    }


    // 分页搜索 按标题匹配
   /* public Page<ItemDocument> searchByTitle(String keyword, int page, int size) throws IOException {
        if (keyword == null || keyword.isEmpty()) {
            throw new IllegalArgumentException("搜索关键词为空");
        }

        int currentPage = Math.max(page, 1);
        int from = (currentPage - 1) * size;

        Query query = Query.of(q -> q
                .bool(b -> b
                        .must(m -> m.match(mt -> mt.field("title").query(keyword)))
                        .filter(f -> f.term(t -> t.field("status").value(ItemStatusConstant.ON_SALE)))
                        .filter(f -> f.term(t -> t.field("isDeleted").value(0)))
                )
        );

        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX)
                .query(query)
                .from(from)
                .size(size)
        );

        SearchResponse<ItemDocument> response = esClient.search(request, ItemDocument.class);

        List<ItemDocument> items = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

        long totalHits = response.hits().total().value();
        long totalPages = (totalHits + size - 1) / size;

        return new PageImpl<>(items, PageRequest.of(currentPage - 1, size), totalHits) {
            @Override
            public int getTotalPages() {
                return (int) totalPages;
            }
        };
    }*/
    public Page<ItemDocument> searchByTitle(String keyword, int page, int size) throws IOException {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词为空");
        }

        // ← 修复：page 从 0 开始计算
        int currentPage = Math.max(page, 0);
        int from = currentPage * size;

        Query query = Query.of(q -> q
                .bool(b -> b
                        .must(m -> m.match(mt -> mt.field("title").query(keyword)))
                        .filter(f -> f.term(t -> t.field("status").value(ItemStatusConstant.ON_SALE)))
                        .filter(f -> f.term(t -> t.field("isDeleted").value(0)))
                )
        );

        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX)
                .query(query)
                .from(from)
                .size(size)
        );

        SearchResponse<ItemDocument> response = esClient.search(request, ItemDocument.class);

        List<ItemDocument> items = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

        long totalHits = response.hits().total().value();

        return new PageImpl<>(items, PageRequest.of(currentPage, size), totalHits);
    }
}