package com.u.api.internal.user;

import com.u.api.dto.user.UserDTO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内部管理系统用户接口
 */
@RequestMapping("/inner/admin/users")
public interface InternalAdminUserApi {

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    Result<PageDTO<UserDTO>> pageQuery(
            @RequestParam("page") int page,
            @RequestParam("pageSize") int pageSize
    );

    /**
     * 更新用户状态
     */
    @PostMapping("/status")
    Result<Void> updateStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") Integer status
    );

    /**
     * 批量查询用户信用分
     */
    @GetMapping("/credit-scores")
    Result<Map<Long, Integer>> getUserCreditScores(@RequestParam("userIds") List<Long> userIds);

    /**
     * 重置用户密码
     */
    @PostMapping("/reset-password")
    Result<Void> resetPassword(@RequestParam("userId") Long userId, @RequestParam("newPassword") String newPassword);
}