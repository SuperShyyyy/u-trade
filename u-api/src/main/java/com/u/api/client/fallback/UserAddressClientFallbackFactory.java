package com.u.api.client.fallback;

import com.u.api.client.user.UserAddressClient;
import com.u.api.dto.user.UserAddressDTO;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * UserAddressClient 降级工厂
 */
@Component
@Slf4j
public class UserAddressClientFallbackFactory implements FallbackFactory<UserAddressClient> {

    @Override
    public UserAddressClient create(Throwable cause) {
        log.error("user-service (地址) 调用失败，触发降级: {}", cause.getMessage());
        return new UserAddressClient() {
            @Override
            public Result<UserAddressDTO> getAddress(Long id, Long userId) {
                return Result.error(ResultCode.ERROR, "用户服务不可用，查询地址失败");
            }
        };
    }
}
