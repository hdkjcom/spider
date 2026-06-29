# Contributing to Spider

Spider aims to become an Apache top-level project. We welcome contributions!

## Development Environment

- JDK 8+
- Maven 3.6+
- Git

## Build & Test

```bash
mvn clean install        # Build all modules
mvn test                  # Run all tests
mvn test -pl spider-core -Dtest=MethodMetadataTest  # Run single test
```

## Code Style

- Follow Java conventions (4-space indent, camelCase)
- All public APIs must have Javadoc
- Target Java 8 (no `var`, `record`, `sealed`, module system, or Java 9+ APIs)
- Annotations on interfaces, not implementations

## Module Conventions

- `spider-core`: Zero Spring dependency. All SPIs live here.
- `spider-*`: Implementations. May depend on third-party libraries.
- New transport/codec modules: implement the SPI from spider-core.

## Pull Request Process

1. Fork and create a feature branch
2. Add tests for new functionality
3. Run `mvn test` — all modules must pass
4. Update README.md if adding user-facing features
5. Submit PR with clear description

## Adding a New Module

1. Create `spider-<name>/pom.xml` with parent `io.github.hdkjcom.spider:spider:0.1.8`
2. Add `<module>spider-<name></module>` to root `pom.xml`
3. Implement the appropriate SPI from `spider-core`
4. Add unit tests

## Governance

This project follows the Apache Way:
- Community over code
- Meritocracy
- Consensus-based decision making
