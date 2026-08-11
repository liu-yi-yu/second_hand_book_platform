package org.tlais.yutest1.constant;

public class SortBy {
    //`newest`（最新发布）
    // 、`price_asc`（价格升序）
    // 、`price_desc`（价格降序）
    // 、`popular`（浏览量最多） 按 `view_count` 降序排列
    public static final String NEWEST = "update_time desc";
    public static final String PRICE_ASC = "original_price asc";
    public static final String PRICE_DESC = "original_price desc";
    public static final String POPULAR = "view_count desc";
}
