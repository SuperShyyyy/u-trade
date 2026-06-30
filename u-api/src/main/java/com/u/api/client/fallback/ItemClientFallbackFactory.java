package com.u.api.client.fallback;

import com.u.api.client.item.ItemClient;
import com.u.api.dto.item.ItemTradeDTO;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * ItemClient 降级工厂
 * 当 item-service 不可用时提供兜底响应，避免级联故障
 */
@Component
@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {

    @Override
    public ItemClient create(Throwable cause) {
        log.error("item-service 调用失败，触发降级: {}", cause.getMessage());
        return new ItemClient() {
            @Override
            public Result<Void> lockItem(Long id) {
                return Result.error(ResultCode.ERROR, "商品服务不可用，锁定商品失败");
            }

            @Override
            public Result<Void> markItemSold(Long id) {
                return Result.error(ResultCode.ERROR, "商品服务不可用，标记已售失败");
            }

            @Override
            public Result<Void> releaseItem(Long id) {
                log.warn("商品服务不可用，释放商品降级处理(静默)，itemId={}", id);
                return Result.success();
            }

            @Override
            public Result<ItemTradeDTO> getItemTrade(Long id) {
                return Result.error(ResultCode.ERROR, "商品服务不可用，查询商品信息失败");
            }
        };
    }
}
