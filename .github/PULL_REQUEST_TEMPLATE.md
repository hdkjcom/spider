## What does this PR do

<!-- Brief description of the change -->

## Why / Related Issue

<!-- Background, motivation; reference Issue #xxx -->

## Change type

- [ ] feat new feature
- [ ] fix bug fix
- [ ] docs documentation
- [ ] refactor
- [ ] test
- [ ] chore build/dependency

## Tests

- [ ] `mvn test -pl <module>` for affected module passes
- [ ] `mvn test` (full) passes for cross-module changes

## Checklist

- [ ] PR targets the `dev` branch
- [ ] Commit messages follow `type: description`
- [ ] No Spring dependency introduced into `spider-core`
- [ ] Java 8 compatible (no `var` / `record` / `sealed` / Java 9+ API)
- [ ] Public API has Javadoc
- [ ] New feature has unit tests
