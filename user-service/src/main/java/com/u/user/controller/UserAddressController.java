package com.u.user.controller;


import com.u.user.domain.dto.UserAddressDTO;
import com.u.user.domain.vo.UserAddressVO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.user.service.IUserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
@Tag(name = "用户地址接口")
@RequiredArgsConstructor
public class UserAddressController {

    private final IUserAddressService userAddressService;
    @Operation(summary = "分页查询用户地址")
    @GetMapping("/list")
    public Result<PageDTO<UserAddressVO>> listAddresses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userAddressService.listUserAddresses(page,pageSize));
    }
    @Operation(summary = "根据id查询地址详情")
    @GetMapping("/{id}")
    public Result<UserAddressVO> getAddress(@PathVariable Long id) {
        return Result.success(userAddressService.getAddress(id));
    }
    @Operation(summary = "新增地址")
    @PostMapping
    public Result addAddress(@RequestBody @Valid UserAddressDTO dto) {
        userAddressService.addAddress(dto);
        return Result.success();
    }

    @Operation(summary = "更新地址")
    @PutMapping
    public Result updateAddress(@RequestBody @Valid UserAddressDTO dto) {
        userAddressService.updateAddress(dto);
        return Result.success();
    }
    @Operation(summary = "删除地址")
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
