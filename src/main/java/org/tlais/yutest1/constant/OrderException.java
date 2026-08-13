package org.tlais.yutest1.constant;

public class OrderException {
    //更新订单状态失败，卖家不同
    public static final String SELLER_DIFFERENT = "更新订单状态失败，卖家不同";
    //更新订单状态失败，买家不同
    public static final String BUYER_DIFFERENT = "更新订单状态失败，买家不同";
    //卖家和买家相同
    public static final String SELLER_AND_BUYER_SAME = "更新订单状态失败，卖家和买家相同";
    //用户不是卖家也不是买家
    public static final String USER_NOT_SLLER_OR_BUYER = "更新订单状态失败，用户不是卖家也不是买家";

    //订单不存在
    public static final String ORDER_NOT_EXIST = "订单不存在";
    //更新订单状态失败，订单状态异常
    public static final String ORDER_STATUS_EXCEPTION = "更新订单状态失败，订单状态异常";

    //用户不存在
    public static final String USER_NOT_EXIST = "用户不存在";
    //书籍不存在
    public static final String BOOK_NOT_EXIST = "书籍不存在";
}
