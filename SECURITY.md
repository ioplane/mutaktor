# Security Policy

## Supported Versions

| Version | Support status |
|---|---|
| 0.x.y (current) | Actively maintained — security fixes released |
| < 0.1.0 | Not supported |

Once 1.0.0 is released, the two most recent minor versions will receive security fixes.

---

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Report vulnerabilities through [GitHub Security Advisories](https://github.com/ioplane/mutaktor/security/advisories/new). This creates a private disclosure thread visible only to maintainers.

Include in your report:

- A clear description of the vulnerability
- Steps to reproduce or a proof-of-concept
- The affected version(s)
- Your assessment of impact and severity
- Any suggested remediation, if you have one

---

## Response Timeline

| Stage | Target |
|---|---|
| Acknowledge receipt | Within 48 hours |
| Confirm or dismiss | Within 5 business days |
| Share remediation plan | Within 7 days of confirmation |
| Release fix | Within 30 days of confirmation |
| Public disclosure | After fix is released, coordinated with reporter |

If a fix requires more than 30 days, the maintainers will communicate the revised timeline to the reporter before the original deadline.

---

## Security Measures

### XML parsing — DTD disabled

All XML parsing of PIT mutation reports uses `javax.xml.parsers.DocumentBuilderFactory` with DTD processing fully disabled to prevent XML External Entity (XXE) injection:

```kotlin
val factory = DocumentBuilderFactory.newInstance()
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
factory.isExpandEntityReferences = false
```

Mutation report XML is written by PIT to `build/reports/mutaktor/` and consumed by the same build process. It is treated as untrusted input nonetheless.

### Git execution — input validation

`GitDiffAnalyzer` executes `git diff` via `ProcessBuilder`. The `since` property (git ref) is passed as a discrete command argument, not interpolated into a shell string:

```kotlin
ProcessBuilder("git", "diff", "--name-only", "--diff-filter=ACMR", sinceRef, "HEAD")
```

This prevents shell injection. The `since` property must come from the build script DSL — it is a trusted configuration value, not end-user input. Do not pass unsanitized external values (e.g., environment variables from untrusted sources) to `since`.

### GitHub API — Bearer token authentication

`GithubChecksReporter` uses `java.net.http.HttpClient` with Bearer token authentication against the GitHub Checks API. The token is read from the `GITHUB_TOKEN` environment variable:

```kotlin
request.header("Authorization", "Bearer ${System.getenv("GITHUB_TOKEN")}")
```

Security requirements:

- Never hardcode a token in build scripts or source code
- Use GitHub Actions' built-in `GITHUB_TOKEN` secret for CI environments
- The token is only sent over HTTPS (`https://api.github.com`)
- Tokens are not logged or written to report files

### Zero external runtime dependencies

The plugin has no external runtime dependencies beyond the Gradle API and PIT. All HTTP, XML, and JSON processing uses JDK standard library classes (`java.net.http`, `javax.xml.parsers`, `StringBuilder`).

This eliminates supply chain risk from third-party library vulnerabilities entirely. Dependency updates are limited to PIT itself (a well-maintained, widely-used Java tool) and the Gradle API (controlled by Gradle Inc.).

---

## Dependency Policy

| Dependency | Scope | Rationale |
|---|---|---|
| Gradle API | `compileOnly` | Host build system — not bundled |
| PIT (`pitest-command-line`) | `mutaktor` config | Core mutation engine — resolved at runtime by consumer |
| `pitest-junit5-plugin` | `mutaktor` config | JUnit 5 integration — resolved at runtime by consumer |
| JDK stdlib | implicit | Zero transitive exposure |

No dependencies are bundled (shadowed) into the plugin JAR. PIT and its JUnit plugin are resolved from Maven Central by the consumer project's dependency resolution, not packaged with mutaktor.

Dependency versions are pinned in `gradle.properties` and updated deliberately. Renovate or Dependabot PRs for PIT updates are reviewed for security advisories before merging.
