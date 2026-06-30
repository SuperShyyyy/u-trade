package com.u.api.client.fallback;

import com.u.api.client.user.UserClient;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import com.u.api.dto.user.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * UserClient 降级工厂
 */
@Component
@Slf4j
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        log.error("user-service 调用失败，触发降级: {}", cause.getMessage());
        return new UserClient() {
            @Override
            public Result<PageDTO<UserDTO>> pageQuery(int page, int pageSize) {
                return Result.error(ResultCode.ERROR, "用户服务不可用，分页查询失败");
            }

            @Override
            public Result<Void> updateStatus(Long id, Integer status) {
                return Result.error(ResultCode.ERROR, "用户服务不可用，更新状态失败");
            }

            @Override
            public Result<Map<Long, Integer>> getUserCreditScores(List<Long> userIds) {
                log.warn("用户服务不可用，信用分查询降级返回空，userIds={}", userIds);
                return Result.success(Map.of());
            }

            @Override
            public Result<Void> resetPassword(Long userId, String newPassword) {
                return Result.error(ResultCode.ERROR, "用户服务不可用，重置密码失败");
            }
        };
    }
}
