package io.github.spider.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SpiderCodegenTest {

    @Test
    void testGenerateFromOpenApiJson(@TempDir Path tempDir) throws Exception {
        // Write a minimal OpenAPI 3.0 JSON
        File specFile = tempDir.resolve("openapi.json").toFile();
        try (PrintWriter w = new PrintWriter(new FileWriter(specFile))) {
            w.println("{");
            w.println("  \"openapi\": \"3.0.0\",");
            w.println("  \"info\": { \"title\": \"Test API\", \"version\": \"1.0\" },");
            w.println("  \"paths\": {");
            w.println("    \"/users/{id}\": {");
            w.println("      \"get\": {");
            w.println("        \"operationId\": \"getUser\",");
            w.println("        \"tags\": [\"user\"],");
            w.println("        \"parameters\": [");
            w.println("          { \"name\": \"id\", \"in\": \"path\", \"required\": true,");
            w.println("            \"schema\": { \"type\": \"integer\", \"format\": \"int64\" } }");
            w.println("        ],");
            w.println("        \"responses\": {");
            w.println("          \"200\": { \"description\": \"OK\", \"content\": {");
            w.println("            \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/User\" } }");
            w.println("          }}}}},");
            w.println("    \"/users\": {");
            w.println("      \"post\": {");
            w.println("        \"operationId\": \"createUser\",");
            w.println("        \"tags\": [\"user\"],");
            w.println("        \"requestBody\": { \"content\": {");
            w.println("          \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/CreateUserRequest\" } }");
            w.println("        } },");
            w.println("        \"responses\": {");
            w.println("          \"201\": { \"description\": \"Created\", \"content\": {");
            w.println("            \"application/json\": { \"schema\": { \"$ref\": \"#/components/schemas/User\" } }");
            w.println("          }}}}}},");
            w.println("  \"components\": {");
            w.println("    \"schemas\": {");
            w.println("      \"User\": { \"type\": \"object\", \"properties\": {");
            w.println("        \"id\": { \"type\": \"integer\", \"format\": \"int64\" },");
            w.println("        \"name\": { \"type\": \"string\" }");
            w.println("      } },");
            w.println("      \"CreateUserRequest\": { \"type\": \"object\", \"properties\": {");
            w.println("        \"name\": { \"type\": \"string\" },");
            w.println("        \"email\": { \"type\": \"string\" }");
            w.println("      } }");
            w.println("    }");
            w.println("  }");
            w.println("}");
        }

        File outDir = tempDir.resolve("generated").toFile();
        outDir.mkdirs();

        SpiderCodegen codegen = new SpiderCodegen()
                .basePackage("com.example.client")
                .outputDir(outDir.getAbsolutePath());
        codegen.generate(specFile);

        // Verify generated files exist
        File clientFile = new File(outDir, "com/example/client/UserClient.java");
        assertTrue(clientFile.exists(), "UserClient.java should be generated");
        File dtoFile = new File(outDir, "com/example/client/dto/User.java");
        assertTrue(dtoFile.exists(), "User.java DTO should be generated");
        File reqFile = new File(outDir, "com/example/client/dto/CreateUserRequest.java");
        assertTrue(reqFile.exists(), "CreateUserRequest.java DTO should be generated");

        // Verify client file content
        String clientContent = new String(java.nio.file.Files.readAllBytes(clientFile.toPath()));
        assertTrue(clientContent.contains("@SpiderClient"));
        assertTrue(clientContent.contains("@SpiderGet(\"/users/{id}\")"));
        assertTrue(clientContent.contains("@SpiderPost(\"/users\")"));
        assertTrue(clientContent.contains("User getUser(@Path(\"id\") Long id)"));
    }

    @Test
    void testRejectsNonOpenApi3x(@TempDir Path tempDir) throws Exception {
        File specFile = tempDir.resolve("swagger2.json").toFile();
        try (PrintWriter w = new PrintWriter(new FileWriter(specFile))) {
            w.println("{\"swagger\": \"2.0\"}");
        }
        assertThrows(java.io.IOException.class, () ->
                new SpiderCodegen().generate(specFile));
    }
}
