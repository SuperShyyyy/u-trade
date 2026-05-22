package com.u.common.constant;

public class MessageConstant {

    // 用户和登录相关
    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String ROLE_FORBIDDEN = "无权限访问";
    public static final String USER_ALREADY_EXISTS = "用户已存在";
    public static final String TOKEN_EXPIRED = "登录已过期，请重新登录";
    public static final String TOKEN_INVALID = "无效的登录凭证";

    // 管理员相关
    public static final String ADMIN_NOT_FOUND = "管理员不存在";
    public static final String ADMIN_ROLE_ERROR = "管理员角色不正确";
    public static final String ADMIN_LOCKED = "管理员账号被锁定";

    // 订单相关
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";

    // 数据库 CRUD 相关
    public static final String SAVE_FAILED = "保存失败";
    public static final String UPDATE_FAILED = "更新失败";
    public static final String DELETE_FAILED = "删除失败";
    public static final String QUERY_FAILED = "查询失败";
    public static final String DATA_NOT_FOUND = "数据不存在";
    public static final String PARAM_INVALID = "参数不合法";
    public static final String OPERATION_FAILED = "操作失败";

    // 文件操作相关
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String FILE_NOT_FOUND = "文件不存在";
    public static final String FILE_TYPE_NOT_ALLOWED = "文件类型不允许";
    public static final String FILE_TOO_LARGE = "文件大小超出限制";
}
