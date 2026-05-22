package com.u.api.user;
import com.u.api.dto.user.UserDTO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 内部管理系统用户接口
 */
@RequestMapping("/inner/admin/users")
public interface InternalAdminUserApi {

    /**
     * 分页查询用户列表
     *
     * @param page     当前页
     * @param pageSize 每页大小
     * @return 用户分页信息
     */
    @GetMapping("/page")
    Result<PageDTO<UserDTO>> pageQuery(
            @RequestParam("page") int page,
            @RequestParam("pageSize") int pageSize
    );

    /**
     * 更新用户状态
     *
     * @param id     用户ID
     * @param status 用户状态
     * @return void
     */
    @PostMapping("/status")
    Result<Void> updateStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") Integer status
    );
}