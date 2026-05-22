package com.u.order.service.payment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayExecuteResult {
    private Integer payType;
    private String prepayInfo;
}
