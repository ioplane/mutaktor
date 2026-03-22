"""Run a single mutaktor compatibility test inside a podman container."""

from __future__ import annotations

import json
import logging
import time
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass, field
from typing import TYPE_CHECKING, Any

from podman import PodmanClient

if TYPE_CHECKING:
    from pathlib import Path

    from .matrix import JDK, TestProject

MUTAKTOR_VERSION = "0.1.0"
CONTAINER_TIMEOUT = 600
PODMAN_SOCKET = "unix:///run/podman/podman.sock"
MATRIX_IMAGE = "localhost/mutaktor-matrix:latest"
_QUALITY_GATE_KEYWORDS = ("quality gate", "score", "threshold", "below")
_KILLED_STATUSES = frozenset({"KILLED", "TIMED_OUT", "MEMORY_ERROR"})

log = logging.getLogger(__name__)


@dataclass
class RunResult:
    jdk: str = ""
    jdk_vendor: str = ""
    jdk_version: int = 0
    project: str = ""
    status: str = "SKIP"
    tests_pass: bool = False
    mutate_ok: bool = False
    mutations_total: int = 0
    mutations_killed: int = 0
    mutations_survived: int = 0
    mutation_score: int = 0
    has_xml: bool = False
    has_json: bool = False
    has_html: bool = False
    duration_ms: int = 0
    jvm_crash: bool = False
    java_version_string: str = ""
    errors: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def run_combo(
    jdk: JDK,
    project: TestProject,
    mutaktor_root: Path,
    results_dir: Path,
) -> RunResult:
    """Run mutaktor on a test project inside the matrix container."""
    result = RunResult(
        jdk=jdk.key,
        jdk_vendor=jdk.vendor,
        jdk_version=jdk.version,
        project=project.name,
    )

    project_dir = mutaktor_root / "e2e-test" / "projects" / project.path
    if not project_dir.exists():
        result.errors.append(f"project dir not found: {project_dir}")
        return result

    script = _build_script(jdk.sdkman_id, project.path)

    try:
        result = _run_in_container(result, mutaktor_root, script)
    except Exception as exc:
        result.status = "FAIL"
        result.errors.append(f"container error: {exc}")

    result_file = results_dir / f"{project.name}_{jdk.key}.json"
    result_file.write_text(json.dumps(result.to_dict(), indent=2))
    return result


def _run_in_container(
    result: RunResult,
    mutaktor_root: Path,
    script: str,
) -> RunResult:
    """Execute test inside the SDKMAN matrix container."""
    with PodmanClient(base_url=PODMAN_SOCKET) as client:
        start = time.monotonic()

        container = client.containers.run(
            image=MATRIX_IMAGE,
            command=["bash", "-c", script],
            volumes={
                str(mutaktor_root): {"bind": "/project", "mode": "Z"},
            },
            working_dir="/project",
            detach=True,
            remove=False,
        )

        container.wait(timeout=CONTAINER_TIMEOUT)
        result.duration_ms = int((time.monotonic() - start) * 1000)

        output = _collect_logs(container)
        _parse_output(result, output)
        _check_reports_from_output(result, output)
        container.remove(force=True)

    return result


def _collect_logs(container: Any) -> str:
    """Collect stdout+stderr from container."""
    raw = container.logs(stdout=True, stderr=True)
    if isinstance(raw, bytes):
        return raw.decode("utf-8", errors="replace")
    return "".join(
        chunk.decode("utf-8", errors="replace") if isinstance(chunk, bytes) else chunk
        for chunk in raw
    )


def _build_script(sdkman_id: str, project_path: str) -> str:
    """Shell script: switch JDK via SDKMAN, publish, test, mutate."""
    return f"""
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java {sdkman_id}

echo "@@JDK_VERSION@@"
java -version 2>&1
echo "@@END_JDK@@"

# Publish mutaktor to mavenLocal (using current JDK from SDKMAN)
if [ ! -d "$HOME/.m2/repository/io/github/ioplane/mutaktor/mutaktor-gradle-plugin/0.1.0" ]; then
    echo "@@PUBLISH@@"
    sdk use java 25.0.2-tem
    cd /project && ./gradlew publishToMavenLocal -Pversion=0.1.0 --no-daemon -q 2>&1
    sdk use java {sdkman_id}
fi

cd /project/e2e-test/projects/{project_path}
cp /project/gradlew . 2>/dev/null
cp -r /project/gradle . 2>/dev/null
chmod +x gradlew 2>/dev/null
rm -rf build .gradle

echo "@@TESTS_START@@"
./gradlew test --no-daemon 2>&1
echo "@@TESTS_EXIT=$?@@"

echo "@@MUTATE_START@@"
./gradlew mutate --no-daemon 2>&1
echo "@@MUTATE_EXIT=$?@@"

# Report files check
echo "@@REPORTS@@"
find build/reports -name "mutations.xml" -exec echo "HAS_XML:{{}}" \\;
find build/reports -name "*.json" -exec echo "HAS_JSON:{{}}" \\;
find build/reports -name "index.html" -exec echo "HAS_HTML:{{}}" \\;

# Mutation counts from XML
XML_FILE=$(find build/reports -name "mutations.xml" 2>/dev/null | head -1)
if [ -n "$XML_FILE" ]; then
    echo "@@XML_CONTENT@@"
    cat "$XML_FILE"
    echo "@@END_XML@@"
fi

if ls hs_err_pid*.log 2>/dev/null; then
    echo "@@JVM_CRASH@@"
fi

exit 0
"""


def _parse_output(result: RunResult, output: str) -> None:
    """Parse container output markers."""
    lines = output.split("\n")
    result.java_version_string = _extract_jdk_version(lines)
    _parse_test_exit(result, lines)
    _parse_mutate_exit(result, lines, output)

    if "@@JVM_CRASH@@" in output:
        result.jvm_crash = True
        result.errors.append("JVM crash (hs_err_pid)")

    has_failure = result.jvm_crash or not result.tests_pass or not result.mutate_ok
    result.status = "FAIL" if has_failure else "PASS"


def _extract_jdk_version(lines: list[str]) -> str:
    """Extract JDK version from output markers."""
    jdk_lines: list[str] = []
    in_jdk = False
    for line in lines:
        if "@@JDK_VERSION@@" in line:
            in_jdk = True
        elif "@@END_JDK@@" in line:
            in_jdk = False
        elif in_jdk and line.strip():
            jdk_lines.append(line.strip())
    return " | ".join(jdk_lines)


def _parse_test_exit(result: RunResult, lines: list[str]) -> None:
    """Parse test exit code from markers."""
    for line in lines:
        if "@@TESTS_EXIT=" in line:
            code = line.split("=")[1].rstrip("@").strip()
            result.tests_pass = code == "0"
            if not result.tests_pass:
                result.errors.append("unit tests failed")


def _parse_mutate_exit(
    result: RunResult,
    lines: list[str],
    output: str,
) -> None:
    """Parse mutate exit code from markers."""
    lower_output = output.lower()
    for line in lines:
        if "@@MUTATE_EXIT=" in line:
            code = line.split("=")[1].rstrip("@").strip()
            is_qg_fail = any(kw in lower_output for kw in _QUALITY_GATE_KEYWORDS)
            result.mutate_ok = code == "0" or is_qg_fail
            if not result.mutate_ok:
                result.errors.append("mutate task failed")


def _check_reports_from_output(result: RunResult, output: str) -> None:
    """Check reports presence from output markers."""
    if not result.mutate_ok:
        return

    result.has_xml = "HAS_XML:" in output
    result.has_json = "HAS_JSON:" in output
    result.has_html = "HAS_HTML:" in output

    # Parse mutations.xml from output
    if "@@XML_CONTENT@@" in output and "@@END_XML@@" in output:
        xml_start = output.index("@@XML_CONTENT@@") + len("@@XML_CONTENT@@")
        xml_end = output.index("@@END_XML@@")
        xml_content = output[xml_start:xml_end].strip()
        _parse_mutations_xml(result, xml_content)


def _parse_mutations_xml(result: RunResult, xml_content: str) -> None:
    """Parse PIT mutations.xml for counts."""
    try:
        root = ET.fromstring(xml_content)  # noqa: S314
    except ET.ParseError:
        result.errors.append("invalid mutations.xml")
        return

    total = 0
    killed = 0
    survived = 0

    for mutation in root.findall("mutation"):
        total += 1
        status = mutation.get("status", "")
        if status in _KILLED_STATUSES:
            killed += 1
        elif status == "SURVIVED":
            survived += 1

    result.mutations_total = total
    result.mutations_killed = killed
    result.mutations_survived = survived
    result.mutation_score = (killed * 100 // total) if total > 0 else 0

    if total == 0 and result.mutate_ok:
        result.status = "WARN"
        result.errors.append("zero mutations generated")
