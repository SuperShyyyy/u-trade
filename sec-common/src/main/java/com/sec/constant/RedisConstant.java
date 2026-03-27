package com.sec.constant;
public class RedisConstant {

    private RedisConstant() {}

    // 登录 token
    public static final String LOGIN_USER_KEY = "login:user:";

    // 验证码
    public static final String VERIFY_CODE_KEY = "verify:code:";

    // 商品VO
    public static final String ITEM_KEY = "cache:item:";
    // 商品状态
    public static final String ITEM_STATUS_KEY = "cache:order:";

    // 用户收藏
    public static final String USER_FAVORITE_KEY = "favorite:user:";

    // 库存
    public static final String ITEM_STOCK_KEY = "stock:item:";

    // 分布式锁
    public static final String LOCK_ITEM_KEY = "lock:item:";

    //用户个人信息
    public static final String USER_INFO_KEY = "user:info:";

    //订单分页查询

    // 买家订单分页
    public static final String ORDER_PAGE_QUERY_BUYER = "order:page:buyer:";//+ buyerId + ":" + page + ":" + size;

    // 卖家订单分页
    public static final String ORDER_PAGE_QUERY_SELLER = "order:page:seller:";//+ sellerId + ":" + page + ":" + size;
    public static final String ORDER_DETAILS = "order:detail:"; // 加orderId

    //商品缓存
    public static final String ITEM_DETAIL = "item:detail:";
    public static final Long ITEM_TTL = 30L;
    public static final String ITEM_VIEW_COUNT = "item:view:";
    public static final String ITEM_VIEW_IDS = "item:view:ids";

    //卖家查询商品列表 分页
    public static final String ITEM_LIST_SELLER = "item:list:seller:";  // + userId: + page: + size:

    //首页推荐商品列表 (分页)
    public static final String ITEM_HOME_RECOMMEND = "item:home:recommend:";

    //商品搜索列表 分页
    public static final String ITEM_SEARCH = "item:search:";

    //搜索记录
    public static final String HISTORY_SEARCH = "history:search:";



    //redis自动确认收货
    public static final String AUTO_CONFIRM_KEY = "order:auto_confirm:queue";

    //mq结算
    public static final String WALLET_SETTLE = "wallet:settle:";
}