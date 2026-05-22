package com.u.order.controller;


import com.u.order.domain.dto.OrderPaymentDTO;
import com.u.order.domain.dto.OrderSubmitDTO;
import com.u.order.domain.vo.OrderPaymentVO;
import com.u.order.domain.vo.OrderSubmitVO;
import com.u.order.domain.vo.OrderVO;
import com.u.order.domain.vo.ShipmentVO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.order.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 订单表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
@RestController
@RequestMapping("/user/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final IOrderService orderService;
    @PostMapping("/submit")
    @Operation(summary = "用户下单")
    public Result<OrderSubmitVO> submit(@Valid @RequestBody OrderSubmitDTO ordersSubmitDTO){
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.orderSubmit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    @PostMapping("/payment")
    @Operation(summary = "订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrderPaymentDTO orderPaymentDTO) throws Exception {
        log.info("订单支付：{}", orderPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(orderPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }



    @GetMapping("/historyOrders")
    @Operation(summary = "历史订单查询")
    public Result<PageDTO<OrderVO>> page(
           @RequestParam(defaultValue = "1") int page,
           @RequestParam(defaultValue = "10") int pageSize,
           Integer status) {
        PageDTO<OrderVO> pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "查询订单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result cancel(@RequestParam("id") Long id) throws Exception {
        orderService.userCancelById(id);
        return Result.success();
    }
    @Operation(summary = "订单发货")
    @PutMapping("shipment")
    public Result shipment(
            @RequestParam("orderId") Long orderId,
            @RequestParam(required = false) String logisticsCompany,
            @RequestParam(required = false) String trackingNumber) {
        orderService.shipment(orderId, logisticsCompany, trackingNumber);
        return Result.success();
    }



    @Operation(summary = "买家确认收货")
    @PostMapping("/confirm")
    public Result confirm(@RequestParam("id") Long id) {
        orderService.confirm(id);
        return Result.success();
    }

    @Operation(summary = "根据物流号查询物流信息")
    @GetMapping
    public Result<ShipmentVO> queryShipmentByOrderId(Long orderId){
        return Result.success(orderService.queryShipmentByOrderId(orderId));
    }


}
