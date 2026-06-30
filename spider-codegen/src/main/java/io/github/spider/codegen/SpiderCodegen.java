package io.github.spider.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 解析 OpenAPI 3.0 JSON 规范并生成 @SpiderClient 接口及 DTO 类。
 *
 * <pre>{@code
 * SpiderCodegen codegen = new SpiderCodegen();
 * codegen.generate(new File("openapi.json"));
 * }</pre>
 */
public class SpiderCodegen {

    private final ObjectMapper mapper = new ObjectMapper();
    private String basePackage = "com.example.client";
    private String outputDir = "target/generated-sources/spider";

    public SpiderCodegen basePackage(String basePackage) { this.basePackage = basePackage; return this; }
    public SpiderCodegen outputDir(String outputDir) { this.outputDir = outputDir; return this; }

    /**
     * 从 OpenAPI 3.0 JSON 文件生成 Java 源代码。
     *
     * @param openApiFile OpenAPI 3.0 规范的 JSON 文件
     */
    public void generate(File openApiFile) throws IOException {
        JsonNode root = mapper.readTree(openApiFile);
        if (!root.has("openapi") || !root.get("openapi").asText().startsWith("3.")) {
            throw new IOException("Only OpenAPI 3.x specs are supported");
        }

        // Collect all paths and schemas
        JsonNode paths = root.get("paths");
        JsonNode schemas = root.has("components") ? root.get("components").get("schemas") : null;

        if (paths == null || paths.size() == 0) {
            throw new IOException("No paths found in OpenAPI spec");
        }

        // Group operations by tag → service name
        Map<String, List<OperationInfo>> services = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> pathIt = paths.fields();
        while (pathIt.hasNext()) {
            Map.Entry<String, JsonNode> entry = pathIt.next();
            String path = entry.getKey();
            JsonNode methods = entry.getValue();
            Iterator<Map.Entry<String, JsonNode>> methodIt = methods.fields();
            while (methodIt.hasNext()) {
                Map.Entry<String, JsonNode> m = methodIt.next();
                String httpMethod = m.getKey().toUpperCase();
                JsonNode op = m.getValue();
                if (op.has("operationId")) {
                    String tag = op.has("tags") && op.get("tags").size() > 0
                            ? op.get("tags").get(0).asText() : "Default";
                    String serviceName = tag + "-service";
                    services.computeIfAbsent(serviceName, k -> new ArrayList<>())
                            .add(new OperationInfo(httpMethod, path, op));
                }
            }
        }

        // Generate one interface per service
        for (Map.Entry<String, List<OperationInfo>> svc : services.entrySet()) {
            String serviceName = svc.getKey();
            String interfaceName = toClassName(serviceName.replace("-service", "")) + "Client";
            String packagePath = outputDir + "/" + basePackage.replace('.', '/');
            Files.createDirectories(Paths.get(packagePath));

            File javaFile = new File(packagePath, interfaceName + ".java");
            try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(javaFile), StandardCharsets.UTF_8))) {
                writeInterface(w, serviceName, interfaceName, svc.getValue(), schemas);
            }
        }

        // Generate DTOs from schemas
        if (schemas != null) {
            String packagePath = outputDir + "/" + basePackage.replace('.', '/') + "/dto";
            Files.createDirectories(Paths.get(packagePath));
            Iterator<Map.Entry<String, JsonNode>> schemaIt = schemas.fields();
            while (schemaIt.hasNext()) {
                Map.Entry<String, JsonNode> s = schemaIt.next();
                String dtoName = toClassName(s.getKey());
                JsonNode schema = s.getValue();
                if ("object".equals(schema.has("type") ? schema.get("type").asText() : "object")) {
                    File dtoFile = new File(packagePath, dtoName + ".java");
                    try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dtoFile), StandardCharsets.UTF_8))) {
                        writeDTO(w, dtoName, schema);
                    }
                }
            }
        }

        System.out.println("Generated " + services.size() + " client interfaces to " + outputDir);
    }

    // ---- Interface generation ----

    private void writeInterface(PrintWriter w, String serviceName, String interfaceName,
                                 List<OperationInfo> ops, JsonNode schemas) {
        w.println("package " + basePackage + ";");
        w.println();
        w.println("import io.github.spider.core.annotation.*;");
        w.println("import " + basePackage + ".dto.*;");
        w.println();
        w.println("@SpiderClient(name = \"" + serviceName + "\", url = \"${" + serviceName + ".url}\")");
        w.println("public interface " + interfaceName + " {");
        w.println();

        for (OperationInfo op : ops) {
            String returnType = resolveReturnType(op, schemas);
            w.print("    @" + annotationFor(op.httpMethod) + "(\"" + op.path + "\")");
            w.println();
            w.print("    " + returnType + " " + op.operationId + "(");

            // Parameters
            List<String> params = resolveParams(op, schemas);
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) w.print(", ");
                w.print(params.get(i));
            }

            w.println(");");
            w.println();
        }

        w.println("}");
    }

    private String annotationFor(String httpMethod) {
        switch (httpMethod) {
            case "GET": return "SpiderGet";
            case "POST": return "SpiderPost";
            case "PUT": return "SpiderPut";
            case "DELETE": return "SpiderDelete";
            default: return "SpiderGet";
        }
    }

    private String resolveReturnType(OperationInfo op, JsonNode schemas) {
        // OpenAPI permits 200, 201, 202 etc. as success responses; pick the
        // first 2xx response that declares a JSON schema.
        JsonNode ok = firstSuccessResponse(op);
        if (ok != null && ok.has("content")) {
            JsonNode schema = extractJsonSchema(ok.get("content"));
            if (schema != null) {
                if (schema.has("$ref")) {
                    return schemaNameFromRef(schema.get("$ref").asText());
                }
                String type = schema.has("type") ? schema.get("type").asText() : "";
                if ("array".equals(type)) {
                    JsonNode items = schema.get("items");
                    if (items != null && items.has("$ref")) {
                        return schemaNameFromRef(items.get("$ref").asText()) + "[]";
                    }
                    // primitive arrays: keep Object[] to stay Java-8 safe without guessing wrappers
                    return "Object[]";
                }
                if ("string".equals(type)) return "String";
                if ("integer".equals(type)) return "Integer";
                if ("number".equals(type)) return "Double";
                if ("boolean".equals(type)) return "Boolean";
                if ("object".equals(type)) return "java.util.Map<String, Object>";
            }
        }
        return "void";
    }

    /**
     * Find the first 2xx response declared on the operation (200, 201, 202, ...).
     */
    private JsonNode firstSuccessResponse(OperationInfo op) {
        JsonNode responses = op.rawOp.has("responses") ? op.rawOp.get("responses") : null;
        if (responses == null) return null;
        // Prefer 200 explicitly, then any other 2xx in declaration order.
        JsonNode explicit = responses.get("200");
        if (explicit != null) return explicit;
        Iterator<Map.Entry<String, JsonNode>> it = responses.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String code = e.getKey();
            if (code.length() == 3 && code.startsWith("2")) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Pull the JSON schema node from a content map, tolerating missing or
     * wildcard media types. Falls back to the first available entry.
     */
    private JsonNode extractJsonSchema(JsonNode content) {
        if (content == null) return null;
        JsonNode json = content.get("application/json");
        if (json == null) json = content.get("*/*");
        if (json == null && content.size() > 0) {
            json = content.iterator().next();
        }
        return (json != null && json.has("schema")) ? json.get("schema") : null;
    }

    private List<String> resolveParams(OperationInfo op, JsonNode schemas) {
        List<String> params = new ArrayList<>();
        // Path parameters
        if (op.rawOp.has("parameters")) {
            for (JsonNode param : op.rawOp.get("parameters")) {
                String in = param.get("in").asText();
                String name = param.get("name").asText();
                String varName = sanitizeIdentifier(name);
                String type = paramType(param);
                switch (in) {
                    case "path":
                        params.add("@Path(\"" + name + "\") " + type + " " + varName);
                        break;
                    case "query":
                        params.add("@Query(\"" + name + "\") " + type + " " + varName);
                        break;
                    case "header":
                        params.add("@Header(\"" + name + "\") " + type + " " + varName);
                        break;
                }
            }
        }
        // Request body — @Body parameter
        JsonNode body = op.requestBody();
        if (body != null && body.has("content")) {
            JsonNode schema = extractJsonSchema(body.get("content"));
            if (schema != null) {
                String typeName = bodyTypeName(schema);
                if (typeName != null) {
                    params.add("@Body " + typeName + " body");
                }
            }
        }
        return params;
    }

    /**
     * Resolve a Java type name for a request body schema. Returns null when the
     * schema is something we should not turn into a @Body parameter (e.g. empty).
     */
    private String bodyTypeName(JsonNode schema) {
        if (schema.has("$ref")) {
            return schemaNameFromRef(schema.get("$ref").asText());
        }
        String type = schema.has("type") ? schema.get("type").asText() : "";
        if ("object".equals(type)) return "java.util.Map<String, Object>";
        if ("array".equals(type)) {
            JsonNode items = schema.get("items");
            if (items != null && items.has("$ref")) {
                return "java.util.List<" + schemaNameFromRef(items.get("$ref").asText()) + ">";
            }
            return "java.util.List<Object>";
        }
        if ("string".equals(type)) return "String";
        if ("integer".equals(type)) return "Integer";
        if ("number".equals(type)) return "Double";
        if ("boolean".equals(type)) return "Boolean";
        return "Object";
    }

    private String paramType(JsonNode param) {        JsonNode schema = param.has("schema") ? param.get("schema") : param;
        if (schema.has("$ref")) return schemaNameFromRef(schema.get("$ref").asText());
        String type = schema.has("type") ? schema.get("type").asText() : "string";
        switch (type) {
            case "integer": return schema.has("format") && "int64".equals(schema.get("format").asText()) ? "Long" : "Integer";
            case "number": return "Double";
            case "boolean": return "Boolean";
            default: return "String";
        }
    }

    // ---- DTO generation ----

    private void writeDTO(PrintWriter w, String className, JsonNode schema) {
        w.println("package " + basePackage + ".dto;");
        w.println();
        w.println("public class " + className + " {");
        w.println();
        JsonNode props = schema.get("properties");
        if (props != null) {
            Iterator<Map.Entry<String, JsonNode>> it = props.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> prop = it.next();
                String fieldName = prop.getKey();
                JsonNode fieldSchema = prop.getValue();
                String javaType = toJavaType(fieldSchema);
                w.println("    private " + javaType + " " + fieldName + ";");
            }
            w.println();
            // Getters and setters
            it = props.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> prop = it.next();
                String fieldName = prop.getKey();
                String cap = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                String javaType = toJavaType(prop.getValue());
                w.println("    public " + javaType + " get" + cap + "() { return " + fieldName + "; }");
                w.println("    public void set" + cap + "(" + javaType + " " + fieldName + ") { this." + fieldName + " = " + fieldName + "; }");
            }
        }
        w.println("}");
    }

    private String toJavaType(JsonNode schema) {
        if (schema.has("$ref")) return schemaNameFromRef(schema.get("$ref").asText());
        String type = schema.has("type") ? schema.get("type").asText() : "string";
        switch (type) {
            case "integer": return schema.has("format") && "int64".equals(schema.get("format").asText()) ? "Long" : "Integer";
            case "number": return "Double";
            case "boolean": return "Boolean";
            case "array":
                JsonNode items = schema.get("items");
                return items.has("$ref") ? "java.util.List<" + schemaNameFromRef(items.get("$ref").asText()) + ">" : "java.util.List<String>";
            default: return "String";
        }
    }

    private static String schemaNameFromRef(String ref) {
        return toClassName(ref.substring(ref.lastIndexOf('/') + 1));
    }

    private static String toClassName(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.split("[-_]")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    /**
     * Convert an OpenAPI parameter name (which may contain '-', '.', or other
     * characters illegal in a Java identifier) into a valid Java variable name.
     * Non-identifier characters collapse to underscores; a leading digit is
     * prefixed with an underscore.
     */
    private static String sanitizeIdentifier(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0 && Character.isDigit(c)) {
                sb.append('_');
            }
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        if (sb.length() == 0) return "_";
        // collapse repeated underscores for readability
        return sb.toString().replaceAll("_+", "_");
    }

    // ---- Inner types ----

    static class OperationInfo {
        final String httpMethod;
        final String path;
        final String operationId;
        final JsonNode rawOp;

        OperationInfo(String httpMethod, String path, JsonNode rawOp) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.operationId = rawOp.get("operationId").asText();
            this.rawOp = rawOp;
        }

        JsonNode response(String statusCode) {
            return rawOp.has("responses") ? rawOp.get("responses").get(statusCode) : null;
        }

        JsonNode requestBody() {
            return rawOp.has("requestBody") ? rawOp.get("requestBody") : null;
        }
    }
}
