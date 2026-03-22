"""CLI for mutaktor compatibility matrix."""

from __future__ import annotations

import json
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

from rich.console import Console
from rich.panel import Panel
from rich.table import Table

from .matrix import JDKS, PROJECTS, get_phase_combos
from .runner import RunResult, run_combo

console = Console()
MUTAKTOR_ROOT = Path(__file__).resolve().parent.parent.parent
RESULTS_DIR = MUTAKTOR_ROOT / "e2e-test" / "results"
_ARGV_PARALLEL_INDEX = 2


def main() -> None:
    phase = sys.argv[1] if len(sys.argv) > 1 else "quick"
    parallel = int(sys.argv[_ARGV_PARALLEL_INDEX]) if len(sys.argv) > _ARGV_PARALLEL_INDEX else 1

    if phase == "list":
        _list_matrix()
        return

    combos = get_phase_combos(phase)
    if not combos:
        console.print(f"[red]Unknown phase: {phase}[/red]")
        console.print("Phases: quick, baseline, vendor-sweep, framework, full, list")
        sys.exit(1)

    RESULTS_DIR.mkdir(parents=True, exist_ok=True)

    console.print(
        Panel(
            f"[bold]Mutaktor Compatibility Matrix[/bold]\n"
            f"Phase: [cyan]{phase}[/cyan]  "
            f"Combos: [green]{len(combos)}[/green]  "
            f"Parallel: [yellow]{parallel}[/yellow]",
            border_style="blue",
        )
    )

    results: list[RunResult] = []
    start = time.monotonic()

    if parallel <= 1:
        for i, (jdk, project) in enumerate(combos, 1):
            console.print(f"\n[dim][{i}/{len(combos)}][/dim] {jdk.label} x {project.name}")
            r = run_combo(jdk, project, MUTAKTOR_ROOT, RESULTS_DIR)
            results.append(r)
            _print_result(r)
    else:
        with ThreadPoolExecutor(max_workers=parallel) as pool:
            futures = {
                pool.submit(run_combo, jdk, project, MUTAKTOR_ROOT, RESULTS_DIR): (jdk, project)
                for jdk, project in combos
            }
            for future in as_completed(futures):
                jdk, project = futures[future]
                try:
                    r = future.result()
                except Exception as exc:
                    r = RunResult(
                        jdk=jdk.key,
                        project=project.name,
                        status="FAIL",
                        errors=[str(exc)],
                    )
                results.append(r)
                _print_result(r)

    elapsed = time.monotonic() - start
    _print_summary(results, phase, elapsed)

    agg_file = RESULTS_DIR / f"matrix_{phase}.json"
    agg_file.write_text(json.dumps([r.to_dict() for r in results], indent=2))
    console.print(f"\n[dim]Results saved to {agg_file}[/dim]")

    failures = sum(1 for r in results if r.status == "FAIL")
    sys.exit(1 if failures > 0 else 0)


def _print_result(r: RunResult) -> None:
    status_map = {
        "PASS": "green",
        "FAIL": "red",
        "WARN": "yellow",
        "SKIP": "dim",
    }
    style = status_map.get(r.status, "white")
    extra = ""
    if r.mutate_ok:
        extra = f"  mutations={r.mutations_total} score={r.mutation_score}%  {r.duration_ms}ms"
    if r.errors:
        extra += f"  [dim]{'; '.join(r.errors)}[/dim]"
    console.print(f"  [{style}]{r.status}[/{style}] {r.jdk} x {r.project}{extra}")


def _print_summary(results: list[RunResult], phase: str, elapsed: float) -> None:
    passed = sum(1 for r in results if r.status == "PASS")
    failed = sum(1 for r in results if r.status == "FAIL")
    warned = sum(1 for r in results if r.status == "WARN")
    skipped = sum(1 for r in results if r.status == "SKIP")

    console.print()

    table = Table(
        title=f"Compatibility Matrix -- {phase}",
        border_style="blue",
    )
    table.add_column("JDK", style="cyan")
    projects_in_run = sorted({r.project for r in results})
    for p in projects_in_run:
        table.add_column(p, justify="center")

    jdks_in_run = sorted({r.jdk for r in results})
    style_map = {
        "PASS": "[green]PASS[/green]",
        "FAIL": "[red]FAIL[/red]",
        "WARN": "[yellow]WARN[/yellow]",
        "SKIP": "[dim]SKIP[/dim]",
    }
    for jdk in jdks_in_run:
        row: list[str] = [jdk]
        for proj in projects_in_run:
            matches = [r for r in results if r.jdk == jdk and r.project == proj]
            if matches:
                r = matches[0]
                cell = style_map.get(r.status, r.status)
                if r.mutation_score > 0:
                    cell = f"{cell}\n{r.mutation_score}%"
                row.append(cell)
            else:
                row.append("[dim]--[/dim]")
        table.add_row(*row)

    console.print(table)

    console.print(
        Panel(
            f"[green]PASS: {passed}[/green]  "
            f"[red]FAIL: {failed}[/red]  "
            f"[yellow]WARN: {warned}[/yellow]  "
            f"[dim]SKIP: {skipped}[/dim]  "
            f"Total: {len(results)}  Duration: {elapsed:.0f}s",
            title="Summary",
            border_style="green" if failed == 0 else "red",
        )
    )

    if failed > 0:
        console.print("\n[red bold]FAILURES:[/red bold]")
        for r in results:
            if r.status == "FAIL":
                errs = "; ".join(r.errors)
                console.print(f"  [red]x[/red] {r.jdk} x {r.project}: {errs}")


def _list_matrix() -> None:
    """List all available JDKs and projects."""
    console.print("\n[bold]Available JDKs:[/bold]")
    table = Table()
    table.add_column("Vendor")
    table.add_column("Version")
    table.add_column("Image")
    for j in JDKS:
        table.add_row(j.vendor, str(j.version), j.image)
    console.print(table)

    console.print("\n[bold]Available Projects:[/bold]")
    for p in PROJECTS:
        console.print(f"  {p.name} ({p.lang}, {p.framework})")

    console.print("\n[bold]Phases:[/bold]")
    for phase in ("quick", "baseline", "vendor-sweep", "framework", "full"):
        combos = get_phase_combos(phase)
        console.print(f"  {phase}: {len(combos)} combinations")


if __name__ == "__main__":
    main()
