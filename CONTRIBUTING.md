# Contributing to Mutaktor

Thank you for your interest in contributing. This document explains the process and standards.

---

## Quick Start

```bash
git clone https://github.com/dantte-lp/mutaktor.git && cd mutaktor
./gradlew check    # build + unit tests + functional tests
```

## Development Workflow

1. Fork the repository
2. Create a feature branch from `main`
3. Make changes with tests
4. Run `./gradlew check` — all tests must pass
5. Open a Pull Request

## Build Commands

| Command | What it does |
|---------|-------------|
| `./gradlew check` | Build + unit tests + functional tests |
| `./gradlew test` | Unit tests only |
| `./gradlew functionalTest` | Gradle TestKit tests |
| `./gradlew :mutaktor-pitest-filter:test` | Filter module tests |

## Code Standards

- **Language**: Kotlin only. No Groovy, no Java in production code.
- **Gradle API**: Use Provider API (`Property<T>`, `SetProperty<T>`, etc.). No eager `.get()` at configuration time.
- **Dependencies**: Zero external dependencies. Use JDK stdlib only (e.g., `java.net.http.HttpClient`, `javax.xml.parsers`).
- **Tests**: JUnit 5 + Kotest assertions. Gradle TestKit for functional tests.
- **Configuration cache**: All task properties must be serializable. No `Project` references in task fields.
- **Naming**: Follow Kotlin conventions. Packages use underscores: `io.github.dantte_lp.mutaktor`.

## PR Checklist

- [ ] Tests added or updated
- [ ] `./gradlew check` passes
- [ ] CHANGELOG.md updated (if user-facing change)
- [ ] Documentation updated (if applicable)
- [ ] Commit messages are descriptive

## Versioning

- [Semantic Versioning 2.0.0](https://semver.org/)
- [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)
- `0.x.y` — pre-1.0, API may change
- `1.0.0` — stable public API

## License

By contributing, you agree that your contributions will be licensed under [Apache License 2.0](LICENSE).
