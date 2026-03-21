# Mutaktor Licensing Strategy

## Model: Open Core + Gated Artifacts + Signed License

### Tier Architecture

```
Community (Apache 2.0)          Pro (Commercial)             Enterprise (Commercial)
──────────────────────         ────────────────────         ──────────────────────
Maven Central / Plugin Portal   Private Maven repo           Private Maven repo
mutaktor-gradle-plugin          mutaktor-pro                 mutaktor-enterprise
mutaktor-pitest-filter          mutaktor-mcp                 mutaktor-dashboard
                                mutaktor-ai                  mutaktor-analytics
```

### 4-Layer Protection

| Layer | Mechanism | Prevents | Effort |
|-------|-----------|----------|--------|
| **L1: Gated Repo** | Pro JARs in private Maven repo (Cloudsmith/GitHub Packages) | No code to pirate | 2-3 days |
| **L2: License Key** | Ed25519-signed JWT in `~/.mutaktor/license.key` | Casual sharing between orgs | 1-2 days |
| **L3: Obfuscation** | ProGuard on Pro JAR (string encryption, class renaming) | Trivial decompilation | 1 day |
| **L4: Refresh** | Optional HTTPS refresh every 7 days (like ArcMutate) | Revocation of leaked keys | 1-2 days |

### License Key Format (JWT)

```json
{
  "sub": "org-123",
  "iss": "mutaktor",
  "tier": "pro",
  "features": ["ai-suggest", "gitlab", "mcp", "flaky", "debt"],
  "seats": 50,
  "exp": 1743638400,
  "iat": 1711929600
}
```

Signed with Ed25519 private key. Validated offline with embedded public key.
Grace period: 30 days past expiry → degrades to Community.

### Customer Flow

```
1. Subscribe on website → get access token + license key
2. Add to ~/.gradle/gradle.properties:
     mutaktorProToken=mkt_abc123...
3. Add to build.gradle.kts:
     dependencies { mutaktorPro("io.github.ioplane:mutaktor-pro:1.0") }
4. Place license file:
     ~/.mutaktor/license.key
5. Pro features enabled automatically via ServiceLoader
```

### Feature Gating (ServiceLoader)

```kotlin
// Community plugin — extension point
interface MutaktorProFeatures {
    fun suggestTests(survivors: List<SurvivedMutant>): List<TestSuggestion>
    fun reportToGitlab(mutants: List<SurvivedMutant>, config: GitlabConfig)
}

// Pro JAR — implementation discovered via ServiceLoader
class MutaktorProFeaturesImpl : MutaktorProFeatures {
    init { validateLicense() }  // Check JWT on first use
    override fun suggestTests(...) = ...
    override fun reportToGitlab(...) = ...
}

// META-INF/services/io.github.ioplane.mutaktor.MutaktorProFeatures
// io.github.ioplane.mutaktor.pro.MutaktorProFeaturesImpl
```

### What NOT to Do

- Do NOT require internet on every build
- Do NOT use machine fingerprinting (breaks CI/CD)
- Do NOT over-invest — enterprise clients have compliance, not hackers
- Do NOT block Community features with Pro checks

### Competitor Comparison

| Tool | Distribution | License Check | Offline |
|------|-------------|--------------|---------|
| ArcMutate | Maven Central (public) | License file + 7-day refresh | 7 days cached |
| Develocity | Plugin Portal | Server-side key | N/A (server) |
| JetBrains | Marketplace | Signed key + daily check | Offline key option |
| Diffblue | Private | License key | Enterprise only |
| **mutaktor** | **Community: public, Pro: private repo** | **Signed JWT + optional refresh** | **Yes (30-day grace)** |

### Implementation Timeline

| Priority | Task | Sprint |
|----------|------|--------|
| P0 | ServiceLoader feature interface in community plugin | v0.3.0 (Sprint 11) |
| P0 | Private Maven repo setup (Cloudsmith or GitHub Packages) | v0.3.0 (Sprint 11) |
| P1 | JWT license validation (`jjwt` library) | v0.3.0 (Sprint 12) |
| P2 | ProGuard on Pro JAR | v1.0.0 (Sprint 17) |
| P3 | License refresh endpoint | v1.1.0 (Sprint 18) |
