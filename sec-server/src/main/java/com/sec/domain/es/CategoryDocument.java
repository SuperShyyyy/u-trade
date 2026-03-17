package com.sec.domain.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.LocalDateTime;

@Data
@Document(indexName = "category")
public class CategoryDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;

    @Field(type = FieldType.Integer)
    private Long parentId;

    @Field(type = FieldType.Integer)
    private Integer level;

    @Field(type = FieldType.Integer)
    private Integer sort;

    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
}