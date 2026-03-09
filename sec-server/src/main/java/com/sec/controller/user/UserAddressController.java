package com.sec.controller.user;


import com.sec.domain.dto.UserAddressDTO;
import com.sec.domain.vo.UserAddressVO;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.IUserAddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户收货地址表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/user/address")
@Api(tags = "用户地址接口")
public class UserAddressController {
    @Autowired
    private IUserAddressService userAddressService;
    @ApiOperation("分页查询用户地址")
    @GetMapping("/list")
    public Result<PageDTO<UserAddressVO>> listAddresses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userAddressService.listUserAddresses(page,pageSize));
    }
    @ApiOperation("根据id查询地址详情")
    @GetMapping("/{id}")
    public Result<UserAddressVO> getAddress(@PathVariable Long id) {
        return Result.success(userAddressService.getAddress(id));
    }
    @ApiOperation("新增地址")
    @PostMapping
    public Result addAddress(@RequestBody UserAddressDTO dto) {
        userAddressService.addAddress(dto);
        return Result.success();
    }

    @ApiOperation("更新地址")
    @PutMapping
    public Result updateAddress(@RequestBody UserAddressDTO dto) {
        userAddressService.updateAddress(dto);
        return Result.success();
    }
    @ApiOperation("删除地址")
    @DeleteMapping("/{id}")
    public Result deleteAddress(@PathVariable Long id) {
        userAddressService.deleteAddress(id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result setDefault(@PathVariable Long id) {
        userAddressService.setDefaultAddress(id);
        return Result.success();
    }
}
