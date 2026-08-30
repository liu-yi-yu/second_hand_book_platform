package org.tlais.yutest1.constant;

public class NotificationsType {
    //| order_created | 有人购买你的书 |
    //| order_confirmed | 卖家已确认订单 |
    //| order_shipped | 卖家已发货 |
    //| order_received | 买家已收货 |
    //| order_completed | 交易完成 |
    //| order_cancelled | 订单被取消 |
    //| new_message | 收到新消息（聚合：5 分钟内同一订单只发一条） |
    //| review_received | 收到新评价 |
    //| book_sold_out | 你收藏的书被买走了 |
    /** 订单创建 */
    public static final String ORDER_CREATED = "order_created";

    /** 订单确认 */
    public static final String ORDER_CONFIRMED = "order_confirmed";

    /** 订单发货 */
    public static final String ORDER_SHIPPED = "order_shipped";

    /** 订单收货 */
    public static final String ORDER_RECEIVED = "order_received";

    /** 订单完成 */
    public static final String ORDER_COMPLETED = "order_completed";

    /** 订单取消 */
    public static final String ORDER_CANCELLED = "order_cancelled";

    /** 新消息 */
    public static final String NEW_MESSAGE = "new_message";

    /** 评价收到 */
    public static final String REVIEW_RECEIVED = "review_received";

    /** 书被买走了 */
    public static final String BOOK_SOLD_OUT = "book_sold_out";
}
