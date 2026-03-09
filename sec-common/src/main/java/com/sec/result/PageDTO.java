package com.sec.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDTO<T> implements Serializable {
    private Long total;     // 总记录数
    private Long pages;     // 总页数
    private Long current;   // 当前页码
    private List<T> items;   // 当前页数据
    public static <T> PageDTO<T> of(IPage<T> page) {
        return new PageDTO<>(
                page.getTotal(),
                page.getSize(),
                page.getCurrent(),
                page.getRecords()
        );
    }
}
