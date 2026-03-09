package com.sec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.context.BaseContext;
import com.sec.domain.dto.UserAddressDTO;
import com.sec.domain.po.UserAddress;
import com.sec.domain.vo.UserAddressVO;
import com.sec.mapper.UserAddressMapper;
import com.sec.result.PageDTO;
import com.sec.service.IUserAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.PartialResultException;

/**
 * <p>
 * 用户收货地址表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements IUserAddressService {
    private final UserAddressMapper userAddressMapper;
    @Override
    public PageDTO<UserAddressVO> listUserAddresses(int page,int pageSize){
        Long userId = BaseContext.getCurrentId();
        Page<UserAddress> pageParam = new Page<>(page,pageSize);
        LambdaQueryWrapper<UserAddress> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAddress::getUserId,userId).orderByDesc(UserAddress::getCreateTime);
        IPage<UserAddress> userAddressIPage = userAddressMapper.selectPage(pageParam,queryWrapper);
        IPage<UserAddressVO> userAddressVOIpage = userAddressIPage.convert(addr->{
            UserAddressVO userAddressVO = new UserAddressVO();
            BeanUtils.copyProperties(addr,userAddressVO);
            return userAddressVO;
        });
        return PageDTO.of(userAddressVOIpage);
    };
    @Override
    public UserAddressVO getAddress(Long id){
        if (id == null) {
            throw new RuntimeException("参数为空，无法查询");
        }
        Long userId = BaseContext.getCurrentId();
        UserAddress address = lambdaQuery().eq(UserAddress::getId, id).eq(UserAddress::getUserId,userId).one();
        if (address == null) {
            throw new RuntimeException("地址不存在或无权限访问");
        }
        UserAddressVO vo = new UserAddressVO();
        BeanUtils.copyProperties(address, vo);

        return vo;
    };
    @Override
    @Transactional
    public void addAddress(UserAddressDTO dto){
        Long userId = BaseContext.getCurrentId();
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(dto, address, "id");
        address.setUserId(userId);
        //如果新增地址是默认地址 将原默认地址设为非默认地址
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            this.update(new LambdaUpdateWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .set(UserAddress::getIsDefault, 0));
        }

        this.save(address);
    }
    @Transactional
    @Override
    public void updateAddress(UserAddressDTO dto){
        Long userId = BaseContext.getCurrentId();
        UserAddress userAddress = this.getById(dto.getId());
        if (userAddress == null || !userAddress.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在或无权修改");
        }
        //将所有地址设为非默认
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            this.update().set("is_default", 0).eq("user_id", userId).update();
        }
        BeanUtils.copyProperties(dto, userAddress, "id", "userId");
        this.updateById(userAddress);
    };
    @Override
    public void deleteAddress(Long id){
        Long userId = BaseContext.getCurrentId();
       boolean success = this.remove(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId,userId)
                .eq(UserAddress::getId,id)
        );
       if (!success){ throw  new RuntimeException("删除失败");}
    };
    @Override
    @Transactional
    public void setDefaultAddress(Long id) {
        Long userId = BaseContext.getCurrentId();

        UserAddress address = lambdaQuery()
                .eq(UserAddress::getId, id)
                .eq(UserAddress::getUserId, userId)
                .one();

        if (address == null) {
            throw new RuntimeException("地址不存在或无权限");
        }

        this.update(new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, 0));



        address.setIsDefault(1);
        boolean success = this.updateById(address);

        if (!success) {
            throw new RuntimeException("设置默认地址失败");
        }
    }
}
