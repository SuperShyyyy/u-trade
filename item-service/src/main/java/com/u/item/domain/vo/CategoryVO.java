package com.u.item.domain.vo;

import lombok.Data;
import java.util.List;

@Data
public class CategoryVO {

    private Integer id;

    private String name;

    private Integer parentId;

    private Integer level;

    private Integer sort;

    private List<CategoryVO> children;

}