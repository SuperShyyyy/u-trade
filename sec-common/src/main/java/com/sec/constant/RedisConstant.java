package com.sec.constant;
public class RedisConstant {

    private RedisConstant() {}

    // 登录 token
    public static final String LOGIN_USER_KEY = "login:user:";

    // 验证码
    public static final String VERIFY_CODE_KEY = "verify:code:";

    // 商品缓存
    public static final String ITEM_CACHE_KEY = "cache:item:";

    // 用户收藏
    public static final String USER_FAVORITE_KEY = "favorite:user:";

    // 库存
    public static final String ITEM_STOCK_KEY = "stock:item:";

    // 分布式锁
    public static final String LOCK_ITEM_KEY = "lock:item:";

    //用户个人信息
    public static final String USER_INFO_KEY = "user:info:";

    // 订单分页查询缓存键
    public static final String ORDER_PAGE_QUERY_BUYER = "order:page:query:buyer:";
    public static final String ORDER_PAGE_QUERY_SELLER = "order:page:query:seller:";
    public static final String ORDER_DETAILS = "order:page:query:";


    // 订单详情缓存键
    public static final String ORDER_DETAILS_BUYER = "order:details:buyer:";
    public static final String ORDER_DETAILS_SELLER = "order:details:seller:";

    // 防止创建实例

}