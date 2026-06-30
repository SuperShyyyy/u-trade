package com.u.user.service;

import com.u.user.domain.dto.ChangePasswordDTO;
import com.u.user.domain.dto.LoginDTO;
import com.u.user.domain.dto.RegisterDTO;
import com.u.user.domain.dto.UserDTO;
import com.u.user.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.user.domain.vo.LoginVO;
import com.u.user.domain.vo.UserVO;

public interface IUserService extends IService<User> {

    LoginVO userLogin(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    UserVO queryMyInformation();

    void updateUserInfo(UserDTO userDTO);

    /** 修改密码（用户主动操作） */
    void changePassword(ChangePasswordDTO changePasswordDTO);

    /**
     * 重置密码（管理端内部调用）
     * 也会递增 tokenVersion，使所有设备 Token 失效
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 用户登出：tokenVersion+1 + 清除 session
     */
    void logout(Long userId);
}
