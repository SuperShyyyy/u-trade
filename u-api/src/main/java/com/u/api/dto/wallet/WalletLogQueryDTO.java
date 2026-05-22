package com.u.api.dto.wallet;

import lombok.Data;

@Data
public class WalletLogQueryDTO {
    private String bizOrderNo;
    private java.time.LocalDateTime startTime;
    private java.time.LocalDateTime endTime;
    private Integer page = 1;
    private Integer pageSize = 20;
}