---
id: changelog
title: Changelog Guide
sidebar_label: Changelog
---

# Changelog Guide

![Keep a Changelog](https://img.shields.io/badge/Keep_a_Changelog-1.1.0-orange?style=flat-square)
![SemVer](https://img.shields.io/badge/SemVer-2.0.0-blue?style=flat-square)
![GitHub Releases](https://img.shields.io/badge/GitHub-Releases-181717?style=flat-square&logo=github)

This document explains the changelog format, versioning policy, and the automated release process for mutaktor.

---

## Format

mutaktor's `CHANGELOG.md` follows [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/).

### Structure

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- New feature or enhancement

### Changed
- Change to existing behavior

### Deprecated
- Features that will be removed in a future release

### Removed
- Features removed in this release

### Fixed
- Bug fixes

### Security
- Security fixes

## [1.2.0] — 2026-04-01

### Added
- ...

[Unreleased]: https://github.com/dantte-lp/mutaktor/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/dantte-lp/mutaktor/compare/v1.1.0...v1.2.0
```

### Section usage rules

| Section | When to use |
|---------|-------------|
| `Added` | New features, new DSL properties, new report formats |
| `Changed` | Behavior changes in existing features, default value changes |
| `Deprecated` | Properties or tasks scheduled for removal |
| `Removed` | Properties or tasks that were previously deprecated |
| `Fixed` | Bug fixes — reference the issue number if applicable |
| `Security` | Any fix with security implications |

Every user-facing change requires a `CHANGELOG.md` entry. Internal refactoring that does not affect the plugin API does not require an entry.

---

## Versioning Policy

mutaktor uses [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

### Version format

```
MAJOR.MINOR.PATCH[-SNAPSHOT]
```

Examples:

| Version | Meaning |
|---------|---------|
| `0.1.0-SNAPSHOT` | Pre-release development build (current) |
| `0.1.0` | First public release |
| `0.2.0` | New backward-compatible feature |
| `1.0.0` | Stable public API, first major release |
| `1.1.0` | New DSL property added (backward-compatible) |
| `2.0.0` | Breaking change to DSL or task API |

### Breaking vs. non-breaking changes

| Change type | Version bump |
|-------------|-------------|
| Add new optional DSL property with convention | MINOR |
| Add new task | MINOR |
| Remove or rename existing DSL property | MAJOR |
| Change default value of existing property | MAJOR (if behavior changes) |
| Bug fix that does not change API surface | PATCH |
| New report format as opt-in | MINOR |
| Require newer minimum Gradle/JDK version | MAJOR |

### Pre-1.0 policy

While the version is `0.x.y`, the public API is not yet stable. MINOR version bumps (`0.1.0` → `0.2.0`) may include breaking changes. The DSL will stabilize at `1.0.0`.

---

## Current Version

The version is declared in `gradle.properties`:

```properties
version=0.1.0-SNAPSHOT
group=io.github.dantte-lp.mutaktor
```

Snapshot builds are not published to the Gradle Plugin Portal. Only tagged releases produce published artifacts.

---

## Release Process

### Overview

```kroki-mermaid
flowchart TD
    dev["Developer merges PR to main"] --> update["Update CHANGELOG.md:\nmove [Unreleased] → [X.Y.Z]"]
    update --> bump["Update gradle.properties:\nversion=X.Y.Z"]
    bump --> commit["git commit -m 'Release X.Y.Z'"]
    commit --> tag["git tag vX.Y.Z"]
    tag --> push["git push origin main --tags"]
    push --> trigger["release.yml triggers\non tag v*"]

    subgraph "GitHub Actions: release.yml"
        trigger --> matrix["Matrix build\nJDK 17 + 25"]
        matrix --> testok{All tests pass?}
        testok -- No --> fail["Workflow fails\nNo release created"]
        testok -- Yes --> jars["Collect JARs\n(JDK 17 build)"]
        jars --> extract["Extract release notes\nfrom CHANGELOG.md (awk)"]
        extract --> ghrel["gh release create vX.Y.Z\n+ attach JARs\n+ release notes"]
    end

    ghrel --> done["GitHub Release published"]

    style trigger fill:#e37400,color:#fff
    style ghrel fill:#181717,color:#fff
    style fail fill:#cc3333,color:#fff
    style done fill:#2d8a4e,color:#fff
```

### Step-by-step instructions

#### 1. Prepare the changelog

Move all entries from `[Unreleased]` to a new dated version section:

```markdown
## [Unreleased]

## [0.2.0] — 2026-04-15

### Added
- Quality gate: fail build if mutation score below threshold
- GitHub Checks API reporter with inline PR annotations

### Fixed
- SARIF converter handles mutations with no source file gracefully

[Unreleased]: https://github.com/dantte-lp/mutaktor/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/dantte-lp/mutaktor/compare/v0.1.0...v0.2.0
```

Keep `[Unreleased]` at the top — always empty after a release.

#### 2. Bump the version

```properties
# gradle.properties
version=0.2.0
```

Remove the `-SNAPSHOT` suffix. The release workflow strips the `v` prefix from the tag and passes it to Gradle via `-Pversion="${VERSION}"`.

#### 3. Commit and tag

```bash
git add CHANGELOG.md gradle.properties
git commit -m "Release 0.2.0"
git tag v0.2.0
git push origin main --tags
```

The tag must match the pattern `v*` exactly. The workflow trigger is:

```yaml
on:
  push:
    tags:
      - "v*"
```

#### 4. Verify the release workflow

Navigate to **Actions → Release** in the GitHub repository. The workflow:

1. Runs `./gradlew check -Pversion="0.2.0"` on JDK 17 and 25
2. Uploads JARs from the JDK 17 build as a workflow artifact
3. Extracts the `[0.2.0]` section from `CHANGELOG.md` using the `awk` script
4. Creates a GitHub Release named `mutaktor v0.2.0` with the extracted notes and JARs attached

If the `awk` script finds no matching section, it falls back to a link to `CHANGELOG.md`.

#### 5. Post-release: restore SNAPSHOT

After the release workflow completes, bump the version back to the next SNAPSHOT:

```bash
# gradle.properties
version=0.3.0-SNAPSHOT
```

```bash
git add gradle.properties
git commit -m "Begin 0.3.0-SNAPSHOT development"
git push origin main
```

---

## Release Notes Extraction

The release workflow extracts the matching changelog section automatically:

```bash
VERSION="${GITHUB_REF_NAME#v}"   # strips leading 'v'

awk -v ver="$VERSION" '
  /^## / { if (found) exit; if ($0 ~ ver) { found=1; next } }
  found { print }
' CHANGELOG.md > release-notes.md
```

This prints all lines between the `## [X.Y.Z]` heading and the next `## ` heading. The output is used verbatim as the GitHub Release body.

Example — for tag `v0.2.0` and this changelog:

```markdown
## [0.2.0] — 2026-04-15

### Added
- Quality gate

## [0.1.0] — 2026-03-21
```

The script produces:

```markdown

### Added
- Quality gate

```

---

## Changelog Best Practices

### Write entries as user-facing descriptions

```markdown
# Good — explains what the user gets
- Git-diff scoped analysis: `mutaktor { since.set("main") }` — only mutates changed classes

# Too internal — describes implementation, not user impact
- Added `GitDiffAnalyzer.changedClasses()` method
```

### Reference sprint or issue numbers for traceability

```markdown
### Added
- Extreme mutation mode: 6 method-body removal mutators, `extreme.set(true)` (Sprint 7)
- GitHub Checks API reporter with inline PR annotations for survived mutants (Sprint 6, #42)
```

### Group related entries

Keep all changes under the correct section header within the same version block. Do not add free-form text outside sections.

### Do not edit released sections

Once a version is tagged and released, its changelog section is immutable. If a released note contains an error, add a correction entry under the next version.

---

## Current Unreleased Entries

From `CHANGELOG.md` at the time this documentation was written:

| Section | Entry |
|---------|-------|
| Added | Quality gate: fail build if mutation score below threshold (Sprint 6) |
| Added | Multi-module aggregation: `mutateAggregate` task (Sprint 8) |
| Added | Release workflow: GitHub Actions with JDK 17+25 matrix (Sprint 8) |
| Added | Extreme mutation mode: 6 method-body removal mutators (Sprint 7) |
| Added | GitHub Checks API reporter with inline PR annotations (Sprint 6) |
| Added | mutation-testing-elements JSON report converter (Sprint 5) |
| Added | SARIF 2.1.0 report converter (Sprint 5) |
| Added | Git-diff scoped analysis: `since` property (Sprint 4) |
| Added | Type-safe Kotlin DSL with 24 managed properties (Sprint 2) |
| Added | PIT execution via JavaExec (Sprint 2) |
| Added | Kotlin junk mutation filter with 5 filter patterns (Sprint 3) |
| Added | Project scaffold: Kotlin 2.3, Gradle 9.4.1, JDK 25 (Sprint 1) |
| Added | GitHub Actions CI workflow with JDK 17/21/25 matrix (Sprint 1) |

All entries will move to `[0.1.0]` when the first release tag is pushed.

---

## See Also

- [07-ci-cd.md](07-ci-cd.md) — Release workflow implementation details
- `CHANGELOG.md` — The actual changelog
- `gradle.properties` — Current version declaration
- [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)
- [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html)
