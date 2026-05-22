package com.u.user.service;

import com.u.user.domain.dto.UserAddressDTO;
import com.u.user.domain.po.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.user.domain.vo.UserAddressVO;
import com.u.common.result.PageDTO;

/**
 * <p>
 * 用户收货地址表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface IUserAddressService extends IService<UserAddress> {
    PageDTO<UserAddressVO> listUserAddresses(int page,int pageSize);

    UserAddressVO getAddress(Long id);

    void addAddress(UserAddressDTO dto);

    void updateAddress(UserAddressDTO dto);

    void deleteAddress(Long id);

    void setDefaultAddress(Long id);
}
