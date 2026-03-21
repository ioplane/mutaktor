# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 0.x.y | Yes (current) |

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly:

1. **Do NOT open a public issue**
2. Email: use [GitHub Security Advisories](https://github.com/dantte-lp/mutaktor/security/advisories/new)
3. Include: description, reproduction steps, impact assessment

We will acknowledge within 48 hours and provide a fix timeline.

## Security Considerations

### XML Parsing
All XML parsing (PIT reports) uses `DocumentBuilderFactory` with DTD processing disabled:
```kotlin
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
```

### Git Execution
`GitDiffAnalyzer` executes `git diff` via `ProcessBuilder`. The `sinceRef` parameter is passed as a CLI argument — ensure it comes from trusted configuration (DSL property), not user input.

### GitHub API
`GithubChecksReporter` uses `java.net.http.HttpClient` with Bearer token authentication. Tokens should be provided via environment variables (`GITHUB_TOKEN`), never hardcoded.

### Dependencies
The plugin has zero external runtime dependencies beyond Gradle API and PIT. This minimizes supply chain attack surface.
