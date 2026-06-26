package io.github.spider.core.invocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 不可变的有序 filter 链。
 *
 * <p>每次调用 {@link #next(SpiderInvocationContext)} 都会将内部游标向前推进。
 * 当游标超出 filter 列表时，表示链执行结束并返回 null。
 *
 * <p>线程安全说明：每个调用都应创建新的 SpiderFilterChain 实例。
 * 此设计是有状态的（内部游标），不可在调用之间共享。
 */
public class SpiderFilterChain {

    private final List<SpiderInvocationFilter> filters;
    private int index;

    /**
     * 使用给定的 filter 列表创建链。
     * 列表会被防御性复制。
     */
    public SpiderFilterChain(List<SpiderInvocationFilter> filters) {
        this.filters = Collections.unmodifiableList(new ArrayList<>(filters));
        this.index = 0;
    }

    /**
     * 调用链中的下一个 filter。
     *
     * @param ctx 调用上下文
     * @return 链末尾返回 null，或返回最后一个 filter 的结果
     * @throws Throwable 由 filter 或远程调用抛出
     */
    public Object next(SpiderInvocationContext ctx) throws Throwable {
        if (index < filters.size()) {
            SpiderInvocationFilter filter = filters.get(index++);
            return filter.filter(ctx, this);
        }
        return null;
    }

    /** 返回此链中 filter 的总数。 */
    public int size() {
        return filters.size();
    }

    /** 返回此链中 filter 的不可变列表，用于创建新的链实例。 */
    public List<SpiderInvocationFilter> filters() {
        return filters;
    }
}
