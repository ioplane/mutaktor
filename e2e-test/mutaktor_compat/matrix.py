"""JDK vendor/version matrix and test project definitions."""

from dataclasses import dataclass


@dataclass(frozen=True)
class JDK:
    vendor: str
    version: int
    sdkman_id: str
    label: str
    variant: str = ""  # crac, nik, mandrel, etc.
    vm: str = "hotspot"  # hotspot | graal | openj9

    @property
    def key(self) -> str:
        suffix = f"-{self.variant}" if self.variant else ""
        return f"{self.vendor}-{self.version}{suffix}"


# ── JDK Matrix ────────────────────────────────────────────────
# 12 vendors, 38 JDK distributions inside oraclelinux:10 + SDKMAN.

JDKS: list[JDK] = [
    # ── HotSpot-based ──────────────────────────────────────────
    # Eclipse Temurin (de facto standard)
    JDK("temurin", 17, "17.0.18-tem", "Temurin 17"),
    JDK("temurin", 21, "21.0.10-tem", "Temurin 21"),
    JDK("temurin", 25, "25.0.2-tem", "Temurin 25"),
    # Azul Zulu (enterprise leader)
    JDK("zulu", 17, "17.0.18-zulu", "Zulu 17"),
    JDK("zulu", 21, "21.0.10-zulu", "Zulu 21"),
    JDK("zulu", 25, "25.0.2-zulu", "Zulu 25"),
    JDK("zulu", 25, "25.0.2.crac-zulu", "Zulu 25 CRaC", variant="crac"),
    # Amazon Corretto (AWS)
    JDK("corretto", 17, "17.0.18-amzn", "Corretto 17"),
    JDK("corretto", 21, "21.0.10-amzn", "Corretto 21"),
    JDK("corretto", 25, "25.0.2-amzn", "Corretto 25"),
    # BellSoft Liberica
    JDK("liberica", 17, "17.0.18-librca", "Liberica 17"),
    JDK("liberica", 21, "21.0.10-librca", "Liberica 21"),
    JDK("liberica", 25, "25.0.2-librca", "Liberica 25"),
    # Microsoft OpenJDK — not installed in container (disk)
    # SAP Machine — not installed in container (disk)
    # Oracle JDK (reference)
    JDK("oracle", 17, "17.0.12-oracle", "Oracle JDK 17"),
    JDK("oracle", 21, "21.0.10-oracle", "Oracle JDK 21"),
    JDK("oracle", 25, "25.0.2-oracle", "Oracle JDK 25"),
    # Tencent Kona (China, FIPS)
    JDK("kona", 17, "17.0.18-kona", "Kona 17"),
    JDK("kona", 21, "21.0.10-kona", "Kona 21"),
    # Huawei BiSheng (China, ARM-optimized)
    JDK("bisheng", 17, "17.0.18-bisheng", "BiSheng 17"),
    JDK("bisheng", 21, "21.0.10-bisheng", "BiSheng 21"),
    # Alibaba Dragonwell (China, Wisp coroutines)
    JDK("dragonwell", 17, "17.0.18-albba", "Dragonwell 17"),
    JDK("dragonwell", 21, "21.0.10-albba", "Dragonwell 21"),
    # ── Graal JIT-based ───────────────────────────────────────
    # Oracle GraalVM (proprietary, Graal JIT compiler)
    JDK("graal", 17, "17.0.12-graal", "GraalVM 17", vm="graal"),
    JDK("graal", 21, "21.0.10-graal", "GraalVM 21", vm="graal"),
    JDK("graal", 25, "25.0.2-graal", "GraalVM 25", vm="graal"),
    # Oracle GraalVM CE (community)
    JDK("graalce", 17, "17.0.9-graalce", "GraalVM CE 17", vm="graal"),
    JDK("graalce", 21, "21.0.2-graalce", "GraalVM CE 21", vm="graal"),
    JDK("graalce", 25, "25.0.2-graalce", "GraalVM CE 25", vm="graal"),
    # BellSoft Liberica NIK (Native Image Kit)
    JDK("nik", 21, "23.1.10.r21-nik", "Liberica NIK 21", vm="graal"),
    JDK("nik", 25, "25.0.2.r25-nik", "Liberica NIK 25", vm="graal"),
    # Red Hat Mandrel (Quarkus-optimized GraalVM)
    JDK("mandrel", 17, "22.3.5.r17-mandrel", "Mandrel 17", vm="graal"),
    JDK("mandrel", 21, "23.1.10.r21-mandrel", "Mandrel 21", vm="graal"),
    JDK("mandrel", 25, "25.0.2.r25-mandrel", "Mandrel 25", vm="graal"),
    # ── OpenJ9-based (different VM!) ──────────────────────────
    # IBM Semeru (Eclipse OpenJ9 VM)
    JDK("semeru", 17, "17.0.18-sem", "Semeru 17", vm="openj9"),
    JDK("semeru", 21, "21.0.10-sem", "Semeru 21", vm="openj9"),
    JDK("semeru", 25, "25.0.2-sem", "Semeru 25", vm="openj9"),
]


@dataclass(frozen=True)
class TestProject:
    name: str
    path: str
    lang: str
    framework: str


PROJECTS: list[TestProject] = [
    TestProject("java-baseline", "java-baseline", "java", "none"),
]


_DEFAULT_QUICK_VERSION = 21


def get_phase_combos(phase: str) -> list[tuple[JDK, TestProject]]:
    """Return (JDK, Project) combos for a given phase."""
    combos: list[tuple[JDK, TestProject]] = []
    temurin = [j for j in JDKS if j.vendor == "temurin"]
    hotspot_core = [
        j
        for j in JDKS
        if j.vendor in ("temurin", "zulu", "corretto", "liberica", "oracle") and not j.variant
    ]
    basic = [p for p in PROJECTS if p.framework == "none"]
    active = list(PROJECTS)

    match phase:
        case "baseline":
            combos = [(j, p) for j in temurin for p in basic]
        case "vendor-sweep":
            combos = [(j, p) for j in JDKS for p in basic]
        case "framework":
            combos = [(j, p) for j in hotspot_core for p in active]
        case "full":
            combos = [(j, p) for j in JDKS for p in active]
        case "quick":
            j21 = next(j for j in temurin if j.version == _DEFAULT_QUICK_VERSION)
            combos = [(j21, basic[0])]

    return combos
