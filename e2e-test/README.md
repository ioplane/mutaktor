# Mutaktor JDK Compatibility Matrix

Automated compatibility testing of Mutaktor CE across **36 JDK distributions** from **12 vendors** on **3 LTS versions** (17, 21, 25).

**Latest result: [36/36 PASS](COMPATIBILITY_REPORT.md)**

## Quick Start

```bash
# Install Python dependencies
uv sync

# Build the matrix container (OracleLinux 10 + SDKMAN + 36 JDKs)
podman build -f Containerfile.matrix -t mutaktor-matrix:latest .

# Run a single quick test (Temurin 21)
uv run python -m mutaktor_compat.cli quick

# Run baseline (Temurin 17/21/25)
uv run python -m mutaktor_compat.cli baseline

# Run full vendor sweep (all 36 JDKs)
uv run python -m mutaktor_compat.cli vendor-sweep

# List all available JDKs and phases
uv run python -m mutaktor_compat.cli list
```

## JDK Vendors

| Vendor | VM | JDK 17 | JDK 21 | JDK 25 |
|--------|----|:------:|:------:|:------:|
| Eclipse Temurin | HotSpot | + | + | + |
| Azul Zulu | HotSpot | + | + | + |
| Azul Zulu CRaC | HotSpot | | | + |
| Amazon Corretto | HotSpot | + | + | + |
| BellSoft Liberica | HotSpot | + | + | + |
| Oracle JDK | HotSpot | + | + | + |
| Tencent Kona | HotSpot | + | + | |
| Huawei BiSheng | HotSpot | + | + | |
| Alibaba Dragonwell | HotSpot | + | + | |
| Oracle GraalVM | Graal JIT | + | + | + |
| GraalVM CE | Graal JIT | + | + | + |
| BellSoft Liberica NIK | Graal JIT | | + | + |
| Red Hat Mandrel | Graal JIT | + | + | + |
| IBM Semeru | OpenJ9 | + | + | + |

## Phases

| Phase | Combos | What it tests |
|-------|:------:|---------------|
| `quick` | 1 | Temurin 21 only (smoke test) |
| `baseline` | 3 | Temurin 17/21/25 (establish baseline) |
| `vendor-sweep` | 36 | All vendors x all versions |
| `framework` | 15 | Top 5 vendors x all projects |
| `full` | 36+ | Everything x everything |

## Architecture

```
Python 3.14 (uv + podman-py + rich)
  |
  +-- podman run mutaktor-matrix:latest
        |
        +-- SDKMAN: sdk use java {version}-{vendor}
        +-- Gradle 9.4.1
        +-- ./gradlew publishToMavenLocal (Temurin 25)
        +-- ./gradlew test (unit tests)
        +-- ./gradlew mutate (mutation testing)
        +-- Parse: mutations.xml -> JSON result
```

Each combination runs in a fresh container. The matrix container has all 36 JDKs pre-installed via SDKMAN. Mutaktor is published to `mavenLocal` once (using Temurin 25), then tested on each JDK.

## Project Structure

```
e2e-test/
  mutaktor_compat/          # Python test harness
    cli.py                  #   CLI entry point (phases, parallel, rich output)
    matrix.py               #   JDK vendor/version definitions
    runner.py               #   Container runner (podman-py)
  projects/
    java-baseline/          # Test project (3 classes, 56 tests)
  Containerfile.matrix      # OracleLinux 10 + SDKMAN + 36 JDKs
  pyproject.toml            # Python 3.14 + uv + ruff strict
  COMPATIBILITY_REPORT.md   # Full results with mermaid diagrams
```

## Linting

```bash
uv run ruff check mutaktor_compat/    # 40+ rule sets, strict mode
uv run ruff format --check mutaktor_compat/
```

## Adding a New JDK

1. Find the SDKMAN identifier: `sdk list java | grep vendor`
2. Add `RUN` layer in `Containerfile.matrix`
3. Add `JDK(...)` entry in `mutaktor_compat/matrix.py`
4. Rebuild container, run `vendor-sweep`
