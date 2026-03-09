package com.sec.controller.admin;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.domain.dto.AdminDTO;
import com.sec.domain.dto.LoginDTO;
import com.sec.domain.vo.AdminVO;
import com.sec.domain.vo.LoginVO;
import com.sec.domain.vo.UserVO;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.IAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@Api(tags = "管理端接口")
public class AdminController {

    @Autowired
    private IAdminService adminService;

    /**
     * B 端管理员登录 (兼容普通管理员和超级管理员)
     * POST /admin/login
     */
    @ApiOperation("管理员登录接口")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        log.info("管理员登录：{}", loginDTO.getUsername());

        LoginVO loginVO = adminService.adminLogin(loginDTO);

        return Result.success(loginVO);
    }
    @ApiOperation("分页查询管理员列表")
    @GetMapping ("/system/list")
    public Result<PageDTO<AdminVO>> pageQueryAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("分页查询管理员列表: 第{}页, 每页{}条", page, pageSize);
        PageDTO<AdminVO> pageResult = adminService.pageQuery(page, pageSize);
        return Result.success(pageResult);
    }

    @ApiOperation("创建管理员账号")
    @PostMapping("/system/user")
    public Result save(@RequestBody AdminDTO adminDTO) {
        log.info("创建新管理员账号: {}", adminDTO.getUsername());
        adminService.saveAdmin(adminDTO);
        return Result.success();
    }

    @ApiOperation("删除管理员账户")
    @DeleteMapping("/system/user/{id}")
    public Result delete(@PathVariable("id") Long adminId) {
        adminService.deleteAdminById(adminId);
        return Result.success();
    }
    
    @ApiOperation("修改管理员状态")
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
    @ApiOperation("管理端查询用户")
    @GetMapping("/user")
    public Result<PageDTO<UserVO>> pageQueryUser(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ){
        return Result.success(adminService.pageQueryUser(page,pageSize));
    }
    /*
    * 管理端修改用户状态
    * */
    @ApiOperation("管理端修改用户状态")
    @PutMapping("/user/{id}/status")
    public Result updateUserStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") Integer status
    ){
        adminService.updateUserStatus(id, status);
        return Result.success();
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