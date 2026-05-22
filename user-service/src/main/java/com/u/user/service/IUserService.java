package com.u.user.service;

import com.u.user.domain.dto.LoginDTO;
import com.u.user.domain.dto.RegisterDTO;
import com.u.user.domain.dto.UserDTO;
import com.u.user.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.user.domain.vo.LoginVO;
import com.u.user.domain.vo.UserVO;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface IUserService extends IService<User> {
    /**
     *用户登录
     */
    LoginVO userLogin(LoginDTO loginDTO);
    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);

    UserVO queryMyInformation();

    void updateUserInfo(UserDTO userDTO);
}
