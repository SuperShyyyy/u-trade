package com.sec.domain.dto;


import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WalletLogQueryDTO {
    private String bizOrderNo;       // 可选按流水号查询
    private LocalDateTime startTime; // 开始时间
    private LocalDateTime endTime;   // 结束时间
    private Integer page = 1;        // 分页
    private Integer pageSize = 20;   // 分页
}