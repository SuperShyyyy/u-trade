package com.u.admin.controller;


import com.u.api.dto.user.UserDTO;
import com.u.admin.domain.dto.AdminDTO;
import com.u.admin.domain.dto.LoginDTO;
import com.u.admin.domain.dto.ResetPasswordDTO;
import com.u.admin.domain.vo.AdminVO;
import com.u.admin.domain.vo.LoginVO;
import com.u.admin.service.IAdminService;
import com.u.common.context.BaseContext;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 系统管理员表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/admin")
@Slf4j
@Tag(name = "管理端接口")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    /**
     * B 端管理员登录 (兼容普通管理员和超级管理员)
     * POST /admin/login
     */
    @Operation(summary = "管理员登录接口")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO loginDTO) {
        log.info("管理员登录：{}", loginDTO.getUsername());

        LoginVO loginVO = adminService.adminLogin(loginDTO);

        return Result.success(loginVO);
    }

    @Operation(summary = "管理员登出")
    @PostMapping("/logout")
    public Result<String> logout() {
        Long adminId = BaseContext.getCurrentId();
        adminService.logout(adminId);
        return Result.success("已登出");
    }

    @Operation(summary = "分页查询管理员列表")
    @GetMapping ("/system/list")
    public Result<PageDTO<AdminVO>> pageQueryAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("分页查询管理员列表: 第{}页, 每页{}条", page, pageSize);
        PageDTO<AdminVO> pageResult = adminService.pageQuery(page, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "创建管理员账号")
    @PostMapping("/system/user")
    public Result save(@RequestBody @Valid AdminDTO adminDTO) {
        log.info("创建新管理员账号: {}", adminDTO.getUsername());
        adminService.saveAdmin(adminDTO);
        return Result.success();
    }

    @Operation(summary = "删除管理员账户")
    @DeleteMapping("/system/user/{id}")
    public Result delete(@PathVariable("id") Long adminId) {
        adminService.deleteAdminById(adminId);
        return Result.success();
    }

    @Operation(summary = "修改管理员状态")
    @PutMapping("/system/user/{id}/status")
    public Result updateAdminStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        log.info("修改管理员状态：id={}, status={}", id, status);
        adminService.updateAdminStatus(id, status);
        return Result.success();
    }


    /*
    * 管理端查询用户
    * */
    @Operation(summary = "管理端查询用户")
    @GetMapping("/user")
    public Result<PageDTO<UserDTO>> pageQueryUser(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ){
        return Result.success(adminService.pageQueryUser(page,pageSize));
    }
    /*
    * 管理端修改用户状态
    * */
    @Operation(summary = "管理端修改用户状态")
    @PutMapping("/user/{id}/status")
    public Result updateUserStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") Integer status
    ){
        adminService.updateUserStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/user/reset-password")
    public Result<String> resetUserPassword(@RequestBody @Valid ResetPasswordDTO resetPasswordDTO) {
        log.info("管理员重置用户密码：userId={}", resetPasswordDTO.getUserId());
        adminService.resetUserPassword(resetPasswordDTO.getUserId(), resetPasswordDTO.getNewPassword());
        return Result.success("密码重置成功");
    }


    /*
    管理端审核用户商品
    * */



    /*
    * 管理端订单处理
    *
    * */


    /*
    * 统计商品销量热度
    *
    * */


    /*
    * 统计用户交易收入
    * */
}
