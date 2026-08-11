package org.tlais.yutest1.enumeration;

/**
 * 通知类型枚举
 */
public enum NotificationType {

    /**
     * 订单已创建
     */
    ORDER_CREATED,

    /**
     * 订单已确认
     */
    ORDER_CONFIRMED,

    /**
     * 订单已发货
     */
    ORDER_SHIPPED,

    /**
     * 订单已收货
     */
    ORDER_RECEIVED,

    /**
     * 订单已完成
     */
    ORDER_COMPLETED,

    /**
     * 订单已取消
     */
    ORDER_CANCELLED,

    /**
     * 新消息
     */
    NEW_MESSAGE,

    /**
     * 系统通知
     */
    SYSTEM

}
