package org.tlais.yutest1.context;   // 或放 util/service 包，看你的习惯

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class BookViewCounter {

    // 用 AtomicReference 包一层：drain 时整体「换新 map」，避免边清空边累加丢计数
    private final AtomicReference<ConcurrentHashMap<String, Long>> buffer =
            new AtomicReference<>(new ConcurrentHashMap<>());

    /** 详情页每访问一次 +1（只动内存，不写库） */
    public void increment(String bookId) {
        buffer.get().merge(bookId, 1L, Long::sum);
    }

    /** 取出当天累计并重置，交给 0 点定时任务写库 */
    public Map<String, Long> drain() {
        //顺序：读取旧值 → 设置新值 → 返回旧值。
        return buffer.getAndSet(new ConcurrentHashMap<>());
    }
}
