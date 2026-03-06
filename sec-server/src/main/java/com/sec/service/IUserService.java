package com.sec.service;

import com.sec.domain.dto.LoginDTO;
import com.sec.domain.dto.RegisterDTO;
import com.sec.domain.dto.UserDTO;
import com.sec.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sec.domain.vo.LoginVO;
import com.sec.domain.vo.UserVO;

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
