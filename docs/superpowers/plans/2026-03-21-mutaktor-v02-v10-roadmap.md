# Mutaktor v0.2.0 → v1.2.0 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evolve mutaktor from MVP (v0.1.0) to revenue-generating product (Pro/Enterprise tiers) with AI test generation, mutation intelligence, and enterprise integrations.

**Architecture:** Layered approach — each version adds one revenue tier capability. Core remains Apache 2.0. Pro features use feature flags. Enterprise features are separate modules.

**Tech Stack:** Kotlin 2.3, Gradle 9.4.1, PIT 1.23.0, JDK 17+, java.net.http.HttpClient, MCP SDK, Anthropic/OpenAI/Ollama APIs

---

## Version Map

```
v0.1.0 (DONE) → v0.2.0 → v0.3.0 → v0.4.0 → v0.5.0 → v1.0.0 → v1.1.0 → v1.2.0
  MVP         Foundation  AI-Native  Intel    Scale     GA       Pro       Enterprise
```

---

## v0.2.0 — Foundation (Sprints 9–10)

### Goal
Fix QA issues, add javaLauncher (GraalVM fix), per-package ratchet, @MutationCritical annotation.

### Sprint 9: QA Fixes + javaLauncher

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 9.1 | **Extract shared XML/JSON utilities** | Create: `mutaktor-gradle-plugin/src/main/kotlin/.../util/XmlParser.kt`, `util/JsonBuilder.kt` | 5 unit | Deduplicate escapeJson, textOf, XML parsing from 4 files |
| 9.2 | **Fix hardcoded `src/main/java/`** | Modify: `report/MutationElementsConverter.kt`, `report/SarifConverter.kt`, `report/QualityGate.kt` | 3 unit | Detect `.kt` → `src/main/kotlin/`, `.java` → `src/main/java/` |
| 9.3 | **Unit tests for MutaktorTask.buildPitArguments()** | Create: `test/.../MutaktorTaskArgumentsTest.kt` | 10 unit | Extract arg builder to internal fun, test all property combinations |
| 9.4 | **Unit tests for GithubChecksReporter** | Create: `test/.../report/GithubChecksReporterTest.kt` | 5 unit | Test JSON building, annotation batching, escaping |
| 9.5 | **javaLauncher property** | Modify: `MutaktorExtension.kt`, `MutaktorPlugin.kt` | 2 unit + 1 func | Wire `JavaExec.javaLauncher` from extension. Fixes GraalVM jrt:// [#1] |
| 9.6 | **Fix git flag injection** | Modify: `git/GitDiffAnalyzer.kt` | 1 unit | Prepend `--` before positional args in git command |
| 9.7 | **Handle TIMED_OUT as killed in QualityGate** | Modify: `report/QualityGate.kt` | 2 unit | TIMED_OUT counted as killed, not survived |

**Exit criteria:** 75+ tests total, all QA issues from code review resolved.

### Sprint 10: Per-Package Ratchet + @MutationCritical

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 10.1 | **Per-package mutation ratchet** | Create: `ratchet/MutationRatchet.kt`, `ratchet/RatchetBaseline.kt` | 6 unit | Parse PIT XML by package, compare with `.mutaktor-baseline.json`, fail if score drops |
| 10.2 | **DSL: `mutaktor { ratchet { enabled.set(true) } }`** | Modify: `MutaktorExtension.kt` | 2 unit | Nested extension block with `enabled`, `baselineFile`, `autoUpdate` properties |
| 10.3 | **Ratchet Gradle task** | Create: `MutaktorRatchetTask.kt` | 1 func | `./gradlew mutateRatchet` — evaluate + update baseline |
| 10.4 | **`mutaktor-annotations` module** | Create new module: `mutaktor-annotations/` | — | Tiny JAR: `@MutationCritical`, `@SuppressMutations` annotations |
| 10.5 | **@MutationCritical interceptor** | Modify: `mutaktor-pitest-filter/KotlinJunkFilter.kt` or new: `AnnotationInterceptor.kt` | 4 unit | Read annotations from bytecode, enforce 100% mutation score on annotated code |
| 10.6 | **@SuppressMutations interceptor** | Same file as 10.5 | 3 unit | Exclude annotated methods/classes from mutation |
| 10.7 | **Configuration cache fix** | Modify: `MutaktorPlugin.kt`, `MutaktorAggregatePlugin.kt` | 1 func | Enable and pass the configuration cache functional test |
| 10.8 | **Update Containerfile.dev** | Modify: `deployment/containerfiles/Containerfile.dev` | — | Add Azul Zulu JDK 25 alongside GraalVM |

**Exit criteria:** 95+ tests, ratchet works, annotations work, config cache passes, GraalVM fixed.
**Actual:** 128 tests. Config cache disabled (PIT limitation). All other criteria met.
**Release:** Tag v0.2.0.

### Sprint 10+: GraalVM Auto-Detect — DONE 2026-03-21
**Result:** 3 tests, GraalVmDetector + auto-resolve standard JDK via Gradle Toolchain.

### Sprint 11a: Wire Post-Processing — DONE 2026-03-21
**Result:** 5 tests. MutaktorTask.postProcess() wires JSON + SARIF + quality gate + ratchet + GitHub Checks.

### Sprint 11b: Kotlin E2E + Validation — DONE 2026-03-21
**Result:** 2 tests. Kotlin data class E2E, empty targetClasses validation.

**CE FEATURE-COMPLETE: 135 tests, 0 fail, 1 disabled.**

---

## v0.3.0 — AI-Native (Sprints 11–12)

### Goal
MCP server + LLM-powered test generation for surviving mutants. Pro tier pilot.

### Sprint 11: MCP Server

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 11.1 | **Create `mutaktor-mcp` module** | New module with build.gradle.kts | — | Module compiles |
| 11.2 | **MCP tool: `list-surviving-mutants`** | Create: `mcp/MutaktorMcpServer.kt` | 3 unit | Parses PIT XML, returns structured mutant list |
| 11.3 | **MCP tool: `get-mutant-context`** | Same file | 3 unit | Returns source code + mutation + existing tests for a specific mutant |
| 11.4 | **MCP tool: `mutation-score`** | Same file | 2 unit | Returns per-package mutation scores |
| 11.5 | **MCP tool: `suggest-test`** | Same file | 2 unit | Builds LLM prompt from mutant context, returns structured prompt |
| 11.6 | **MCP stdio transport** | Create: `mcp/MutaktorMcpTransport.kt` | 1 integration | MCP server runs as stdio process, responds to JSON-RPC |
| 11.7 | **Gradle task: `mutaktorMcp`** | Create: register in plugin | 1 func | `./gradlew mutaktorMcp` starts MCP server |

**Exit criteria:** MCP server responds to all 4 tools via stdio transport.

### Sprint 12: LLM Mutant Killer

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 12.1 | **LLM client abstraction** | Create: `ai/LlmClient.kt`, `ai/LlmResponse.kt` | — | Interface for Anthropic/OpenAI/Ollama |
| 12.2 | **Ollama client** (free, local) | Create: `ai/OllamaClient.kt` | 3 unit | HTTP POST to localhost:11434/api/generate |
| 12.3 | **Anthropic client** | Create: `ai/AnthropicClient.kt` | 3 unit | java.net.http.HttpClient, API key from env |
| 12.4 | **OpenAI client** | Create: `ai/OpenAiClient.kt` | 3 unit | Same pattern as Anthropic |
| 12.5 | **Prompt builder for mutation killing** | Create: `ai/MutationKillerPrompt.kt` | 5 unit | Source + mutation + existing test → structured prompt → test code extraction |
| 12.6 | **DSL: `mutaktor { ai { provider.set("ollama") } }`** | Modify: `MutaktorExtension.kt` | 2 unit | Nested block: provider, model, apiKey, generateTests |
| 12.7 | **Gradle task: `mutateAndSuggest`** | Create: `MutaktorSuggestTask.kt` | 1 func | Runs mutate → parses survivors → calls LLM → writes suggested tests to build/mutaktor/suggestions/ |
| 12.8 | **GitHub PR comment with suggestions** | Modify: `report/GithubChecksReporter.kt` | 2 unit | Add suggested test code to Check Run annotations |

**Exit criteria:** `./gradlew mutateAndSuggest` generates test files for surviving mutants.
**Release:** Tag v0.3.0.

---

## v0.4.0 — Intelligence (Sprints 13–14)

### Goal
Mutation debt tracker, difficulty scoring, flaky mutant detection.

### Sprint 13: Mutation Debt Tracker

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 13.1 | **Mutation identity hashing** | Create: `debt/MutantIdentity.kt` | 4 unit | Stable hash from class+method+mutator+line (survives refactoring within method) |
| 13.2 | **Debt database (JSON file)** | Create: `debt/MutationDebtStore.kt` | 5 unit | Read/write `.mutaktor-debt.json`: mutant ID, first seen, status, owner (git blame) |
| 13.3 | **Debt diff: new/fixed/ongoing** | Create: `debt/MutationDebtAnalyzer.kt` | 4 unit | Compare current run with stored debt → new survivors, fixed (now killed), ongoing |
| 13.4 | **Debt report** | Create: `debt/MutationDebtReport.kt` | 3 unit | Summary: "This sprint: +12 new, -8 fixed, net +4" |
| 13.5 | **Gradle task: `mutateDebt`** | Register in plugin | 1 func | `./gradlew mutateDebt` — run mutations + update debt file + print report |
| 13.6 | **DSL: `mutaktor { debt { enabled.set(true) } }`** | Modify: `MutaktorExtension.kt` | 2 unit | Nested block: enabled, file, autoUpdate, failOnIncrease |

### Sprint 14: Difficulty Scoring + Flaky Detection

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 14.1 | **Difficulty scorer** | Create: `scoring/DifficultyScorer.kt` | 5 unit | Score 1-5 based on: tests run against mutant, similar mutants killed, coverage proximity |
| 14.2 | **`--easy-wins` filter** | Modify: `report/QualityGate.kt` | 2 unit | Filter survivors to difficulty 1-2 only |
| 14.3 | **Difficulty in reports** | Modify: `report/MutationElementsConverter.kt`, `report/SarifConverter.kt` | 2 unit | Add difficulty score to JSON/SARIF output |
| 14.4 | **Flaky mutant detector** | Create: `flaky/FlakyMutantDetector.kt` | 4 unit | Run PIT N times (configurable), compare results, flag non-deterministic |
| 14.5 | **DSL: `mutaktor { flaky { runs.set(3) } }`** | Modify: `MutaktorExtension.kt` | 1 unit | Nested block: runs, reportFile |
| 14.6 | **Flaky report** | Create: `flaky/FlakyReport.kt` | 2 unit | List of flaky mutants with test names and pass/fail ratio |

**Exit criteria:** Debt tracking, difficulty scoring, flaky detection all work.
**Release:** Tag v0.4.0.

---

## v0.5.0 — Scale (Sprints 15–16)

### Goal
GitLab/Bitbucket integration, multi-module analytics.

### Sprint 15: GitLab + Bitbucket

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 15.1 | **GitLab MR Notes API** | Create: `report/GitlabReporter.kt` | 3 unit | POST /api/v4/projects/:id/merge_requests/:iid/notes |
| 15.2 | **Bitbucket PR Comments API** | Create: `report/BitbucketReporter.kt` | 3 unit | POST /2.0/repositories/:workspace/:slug/pullrequests/:id/comments |
| 15.3 | **DSL: `mutaktor { ci { provider.set("gitlab") } }`** | Modify: `MutaktorExtension.kt` | 2 unit | Auto-detect from env vars (GITLAB_CI, BITBUCKET_PIPELINE) |
| 15.4 | **CI provider auto-detection** | Create: `ci/CiDetector.kt` | 5 unit | Detect GitHub/GitLab/Bitbucket from env vars |

### Sprint 16: Multi-Module Analytics

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 16.1 | **Aggregate mutation report (JSON)** | Modify: `MutaktorAggregatePlugin.kt` | 3 unit | Combine mutation-testing-elements JSONs from subprojects |
| 16.2 | **Cross-module summary** | Create: `aggregate/AggregateReport.kt` | 3 unit | Per-module score table, total score, worst modules |
| 16.3 | **Prometheus metrics export** | Create: `metrics/PrometheusExporter.kt` | 2 unit | Write `mutaktor_mutation_score{module="x"}` to file |
| 16.4 | **Grafana dashboard template** | Create: `docs/grafana-dashboard.json` | — | Ready-to-import Grafana dashboard |

**Release:** Tag v0.5.0.

---

## v1.0.0 — GA (Sprint 17)

### Goal
Stable API, Gradle Plugin Portal publication, full docs.

| # | Task | Files | Tests | Exit criteria |
|---|------|-------|-------|---------------|
| 17.1 | **API stability review** | All public API | — | Mark `@Incubating` or `@Stable` |
| 17.2 | **Kotlin ABI validation** | `build-logic/` | — | `checkLegacyAbi` baseline created |
| 17.3 | **Gradle Plugin Portal publication** | `publish-conventions.gradle.kts` | — | `./gradlew publishPlugins` works |
| 17.4 | **Documentation update** | `docs/en/`, `docs/ru/` | — | All 8 sprints documented |
| 17.5 | **CHANGELOG for v1.0.0** | `CHANGELOG.md` | — | Keep a Changelog format |
| 17.6 | **README badges** | `README.md` | — | Plugin Portal badge, CI badge |

**Release:** Tag v1.0.0 — first stable release.

---

## v1.1.0 — Pro Launch (Sprints 18–20)

### Goal
Test effectiveness dashboard (SaaS or self-hosted), incremental mutation intelligence.

| Sprint | Tasks | Key deliverable |
|--------|-------|-----------------|
| 18 | Dashboard backend: FastAPI or Quarkus, PostgreSQL schema for mutation history | REST API for mutation data |
| 19 | Dashboard frontend: React/Vite, chart.js, mutation score trends, oracle gap | Interactive web UI |
| 20 | Incremental intelligence: content-hash based caching, equivalent mutant heuristics | 50-80% runtime reduction |

**Release:** Tag v1.1.0 — Pro tier available.

---

## v1.2.0 — Enterprise (Sprints 21–24)

| Sprint | Tasks | Key deliverable |
|--------|-------|-----------------|
| 21 | Risk-based prioritization: git blame integration, bug history correlation | Critical code gets more mutations |
| 22 | Predictive mutation selection: ML model trained on mutation history | Skip likely-killed mutants |
| 23 | SSO/SAML integration for dashboard, audit logging | Enterprise auth |
| 24 | Performance: parallel test runner with work-stealing scheduler | 5-10x throughput |

**Release:** Tag v1.2.0 — Enterprise tier available.

---

## Quality Gates (all sprints)

| Gate | Requirement |
|------|-------------|
| Unit tests | 80%+ line coverage (JaCoCo) |
| Functional tests | All Gradle TestKit tests pass |
| Configuration cache | `--configuration-cache` on all funcTests |
| Deprecation warnings | 0 on `--warning-mode=all` |
| CI | GitHub Actions green (JDK 17/21/25) |
| CHANGELOG | Updated per sprint |
| Zero external deps | Core plugin — only Gradle API + PIT |

## Test Count Projections

| Version | New tests | Cumulative |
|---------|-----------|------------|
| v0.1.0 (done) | 57 | 57 |
| v0.2.0 | +38 | **95** |
| v0.3.0 | +25 | **120** |
| v0.4.0 | +28 | **148** |
| v0.5.0 | +16 | **164** |
| v1.0.0 | +5 | **169** |
| v1.1.0 | +30 | **199** |
| v1.2.0 | +20 | **219** |
