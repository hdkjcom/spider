package io.github.spider.core.metadata;

/**
 * Describes how a method parameter is bound to an HTTP request element.
 */
public class ParamBinding {

    public enum Kind {
        /** URI path variable: /users/{id} */
        PATH,
        /** URL query parameter: ?name=value */
        QUERY,
        /** HTTP header */
        HEADER,
        /** Request body */
        BODY
    }

    private final Kind kind;
    private final String name;  // parameter name (for PATH, QUERY, HEADER)
    private final int index;    // method parameter index (0-based)

    public ParamBinding(Kind kind, String name, int index) {
        this.kind = kind;
        this.name = name;
        this.index = index;
    }

    public Kind kind() { return kind; }
    public String name() { return name; }
    public int index() { return index; }
}
