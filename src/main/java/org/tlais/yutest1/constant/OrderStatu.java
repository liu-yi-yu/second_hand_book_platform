package org.tlais.yutest1.constant;

public class OrderStatu {
    // pending | 待卖家确认 | 买家：取消 / 卖家：确认、取消 |
    //| confirmed | 卖家已确认，等待发货 | 卖家：发货 |
    //| shipped | 卖家已发货，等待收货 | 买家：确认收货 |
    //| received | 买家已收货，等待自动完成 | 系统：24h 后自动完成 |
    //| completed | 交易完成 | 双方：互相评价 |
    //| cancelled | 已取消 | 不可操作 |
    public static final String PENDING = "pending";
    public static final String CONFIRMED = "confirmed";
    public static final String SHIPPED = "shipped";
    public static final String RECEIVED = "received";
    public static final String COMPLETED = "completed";
    public static final String CANCELLED = "cancelled";
}
