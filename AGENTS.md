# Repository Guidelines

## Project Structure & Module Organization

Spider is a multi-module Maven project for Java microservice invocation governance. The root `pom.xml` aggregates modules such as `spider-core`, `spider-http`, `spider-grpc`, `spider-jackson`, `spider-resilience`, `spider-console`, `spider-spring-boot-starter`, and `spider-demo`.

Each module follows the standard Maven layout: production code in `src/main/java`, tests in `src/test/java`, and resources in `src/main/resources` or `src/test/resources`. Keep shared SPIs and Spring-free core behavior in `spider-core`; place transport, codec, metrics, discovery, and integration implementations in their dedicated `spider-*` modules. Documentation lives in `docs/`, with top-level usage material in `README.md` and `README_CN.md`.

## Build, Test, and Development Commands

- `mvn clean install`: builds and installs all modules locally.
- `mvn test`: runs the full test suite.
- `mvn test -pl spider-core`: runs tests for one module.
- `mvn test -pl spider-core -Dtest=DefaultMethodMetadataParserTest`: runs a single test class.
- `mvn exec:java -pl spider-console -Dexec.mainClass=io.github.spider.console.SpiderConsoleApplication`: starts the console application when needed.

Use JDK 8+ and Maven 3.6+.

## Coding Style & Naming Conventions

Use Java 8 syntax only; do not introduce `var`, records, sealed classes, modules, or Java 9+ APIs. Follow standard Java conventions with 4-space indentation, `camelCase` methods and fields, `PascalCase` types, and package names under `io.github.spider`. Public APIs should include Javadoc. Prefer interface annotations for Spider clients and keep implementation-specific dependencies out of `spider-core`.

## Testing Guidelines

Tests use JUnit 5 via Maven Surefire. Name test classes with the `*Test` suffix and keep them beside the module they verify, for example `spider-http/src/test/java/.../OkHttpSpiderTransportTest.java`. Add focused unit tests for new behavior and integration tests when changing cross-module contracts, transports, or Spring Boot auto-configuration.

## Commit & Pull Request Guidelines

Recent commits use concise prefixes such as `fix:`, `docs:`, and `release:` followed by a short description. Keep commits scoped and imperative where possible. Pull requests should describe the change, list affected modules, mention linked issues, and include screenshots only for console UI changes. Run `mvn test` before opening a PR and update README or docs for user-facing behavior.

## Security & Configuration Tips

Do not commit build output, local IDE files, logs, secrets, or generated data. Keep sensitive configuration outside the repository and document required properties with safe example values.
