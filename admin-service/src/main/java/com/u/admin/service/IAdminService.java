package com.u.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.u.api.dto.user.UserDTO;
import com.u.admin.domain.dto.AdminDTO;
import com.u.admin.domain.dto.LoginDTO;
import com.u.admin.domain.po.Admin;
import com.u.admin.domain.vo.LoginVO;
import com.u.common.result.PageDTO;

/**
 * <p>
 * 系统管理员表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface IAdminService extends IService<Admin> {

    LoginVO adminLogin(LoginDTO loginDTO);

    PageDTO pageQuery(int page, int pageSize);

    void saveAdmin(AdminDTO adminDTO);

    void updateAdminStatus(Long id, Integer status);

    void deleteAdminById(Long adminId);

    void updateUserStatus(Long id, Integer status);

    PageDTO<UserDTO> pageQueryUser(int page, int pageSize);
}
