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
    private final String contentType;  // 请求体 Content-Type（仅 BODY 绑定有意义，其余为 null）

    public ParamBinding(Kind kind, String name, int index) {
        this(kind, name, index, null);
    }

    /** 带请求体 Content-Type 的构造函数（用于 BODY 绑定覆盖 encoder 默认媒体类型）。 */
    public ParamBinding(Kind kind, String name, int index, String contentType) {
        this.kind = kind;
        this.name = name;
        this.index = index;
        this.contentType = contentType;
    }

    public Kind kind() { return kind; }
    public String name() { return name; }
    public int index() { return index; }
    public String contentType() { return contentType; }
}
