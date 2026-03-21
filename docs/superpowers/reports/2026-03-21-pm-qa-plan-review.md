# PM + QA Plan Review — Mutaktor

## PM Assessment

### Scope Validation

**Is the scope realistic for a solo developer?**

| Sprint | Effort estimate | Risk |
|--------|----------------|------|
| 1. Scaffold | 2-3 days | LOW — boilerplate |
| 2. Plugin Core | 5-7 days | MEDIUM — PIT CLI arg mapping is complex |
| 3. Kotlin Filter | 3-4 days | HIGH — bytecode pattern matching requires deep ASM knowledge |
| 4. Git-Diff | 2-3 days | LOW — git plumbing well-documented |
| 5. Reporting | 3-4 days | LOW — XML→JSON transformation |
| 6. CI/CD | 3-4 days | MEDIUM — GitHub API auth edge cases |
| 7. Extreme + Incremental | 3-4 days | MEDIUM — PIT mutator config, build cache serialization |
| 8. Multi-module + Polish | 5-7 days | MEDIUM — variant-aware configs are complex |
| **Total** | **~30-40 days** | |

**Verdict:** Realistic for 8-10 weeks of focused work. Sprint 3 (Kotlin filters) is the highest-risk sprint — requires bytecode-level understanding of Kotlin compiler output.

### Dependencies

| Dependency | Status | Risk |
|------------|--------|------|
| PIT 1.23.0 MutationFilter SPI | Stable API | LOW |
| mutation-testing-elements JSON schema | v3.4.0 stable | LOW |
| SARIF 2.1.0 schema | OASIS standard | LOW |
| GitHub Checks API | GA, well-documented | LOW |
| Gradle TestKit | Stable since Gradle 4 | LOW |
| Kotlin 2.3 compiler output patterns | May change in 2.4 | MEDIUM |

### Critical Path

```
Sprint 1 (scaffold) → Sprint 2 (core) → Sprint 3 (kotlin filter)
                                        ↘ Sprint 4 (git-diff) → Sprint 6 (CI/CD)
                                        ↘ Sprint 5 (reporting) → Sprint 6 (CI/CD)
```

Sprints 3, 4, 5 can be **parallelized** after Sprint 2. Sprint 6 depends on 4+5.

---

## QA Assessment

### Test Strategy

| Level | Framework | What to test |
|-------|-----------|-------------|
| Unit | JUnit 5 + Kotest assertions | Extension defaults, arg builder, git parser, XML→JSON converter |
| Integration | Gradle TestKit | Plugin applies, task runs, config cache, build cache |
| Functional | TestKit + real PIT | End-to-end: mutation report generated, filters work |
| Contract | Schema validation | mutation-testing-elements JSON, SARIF |

### Test Matrix

| Axis | Values |
|------|--------|
| Gradle version | 9.4.1 (primary), 9.3.0, 9.0.0 |
| JDK | 25 (primary), 21, 17 |
| PIT version | 1.23.0 (primary), 1.22.0 |
| Kotlin project type | Pure Kotlin, Mixed Java+Kotlin, Multiplatform (JVM) |
| Build features | Configuration cache ON/OFF, Build cache ON/OFF |

### Risk-Based Test Priority

| Feature | Priority | Why |
|---------|----------|-----|
| PIT execution produces correct report | P0 | Core functionality |
| Configuration cache doesn't break | P0 | Gradle 9 expectation |
| Kotlin junk filter accuracy | P0 | Key differentiator |
| Git-diff scoped analysis correctness | P1 | Headline feature |
| mutation-testing-elements JSON validity | P1 | Interop with Stryker Dashboard |
| GitHub Checks API annotations | P2 | CI/CD feature |
| Extreme mutation mode | P2 | Advanced feature |
| Multi-module aggregation | P2 | Scale feature |

### Acceptance Criteria (MVP — v2.0.0)

- [ ] `./gradlew mutate` produces PIT HTML + mutation-testing-elements JSON report
- [ ] `./gradlew mutate --mutaktor-since=main` only mutates changed code
- [ ] Kotlin coroutine/sealed/inline/data class junk filtered by default
- [ ] `--configuration-cache` works
- [ ] Build cache hit on re-run with no source changes
- [ ] 0 deprecation warnings on Gradle 9.4.1
- [ ] 80%+ unit test coverage
- [ ] All funcTests green on JDK 17, 21, 25
