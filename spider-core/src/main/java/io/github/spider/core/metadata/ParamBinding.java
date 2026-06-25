package io.github.spider.core.metadata;

/**
 * 描述方法参数如何绑定到 HTTP 请求元素。
 */
public class ParamBinding {

    public enum Kind {
        /** URI 路径变量，例如 /users/{id} */
        PATH,
        /** URL 查询参数，例如 ?name=value */
        QUERY,
        /** HTTP 头 */
        HEADER,
        /** 请求体 */
        BODY
    }

    private final Kind kind;
    private final String name;  // 参数名称（用于 PATH、QUERY、HEADER）
    private final int index;    // 方法参数索引（从 0 开始）

    public ParamBinding(Kind kind, String name, int index) {
        this.kind = kind;
        this.name = name;
        this.index = index;
    }

    public Kind kind() { return kind; }
    public String name() { return name; }
    public int index() { return index; }
}
