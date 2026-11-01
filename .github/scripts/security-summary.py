#!/usr/bin/env python3
"""Print a Markdown summary of the reports written by the `security` job of build.yml.

Usage: security-summary.py <reports-dir>

Reads whatever exists in the folder (gitleaks.json, npm-audit-prod.json, npm-audit-all.json,
osv.json, sbom-*.cdx.json) and never fails because a report is missing: a step that did not run
shows up as "no report".
"""
import json
import sys
from collections import Counter
from pathlib import Path

SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW", "UNKNOWN"]
COMPONENTS = ["escala-api", "elegibilidade-worker", "web"]
MAX_ROWS = 100


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def severity_from_score(score):
    try:
        value = float(score)
    except (TypeError, ValueError):
        return "UNKNOWN"
    if value >= 9.0:
        return "CRITICAL"
    if value >= 7.0:
        return "HIGH"
    if value >= 4.0:
        return "MEDIUM"
    return "LOW"


def component_of(source_path):
    for name in COMPONENTS:
        if name in source_path:
            return name
    return source_path


def fixed_versions(vuln, package):
    name = package["name"]
    fixed = []
    for affected in vuln.get("affected", []):
        if affected.get("package", {}).get("name") != name:
            continue
        for rng in affected.get("ranges", []):
            fixed += [e["fixed"] for e in rng.get("events", []) if "fixed" in e]
    return ", ".join(sorted(set(fixed))) or "—"


def osv_rows(osv):
    """One row per (component, package, vulnerability)."""
    rows = []
    for result in osv.get("results", []):
        component = component_of(result["source"]["path"])
        for pkg in result.get("packages", []):
            package = pkg["package"]
            score_of = {}
            for group in pkg.get("groups", []):
                for vuln_id in group.get("ids", []):
                    score_of[vuln_id] = group.get("max_severity")
            for vuln in pkg.get("vulnerabilities", []):
                score = score_of.get(vuln["id"])
                cve = next((a for a in vuln.get("aliases", []) if a.startswith("CVE-")), "")
                rows.append({
                    "component": component,
                    "package": package["name"],
                    "version": package["version"],
                    "id": vuln["id"],
                    "cve": cve,
                    "score": score or "",
                    "severity": severity_from_score(score),
                    "fixed": fixed_versions(vuln, package),
                })
    return rows


def count_line(counter):
    return ", ".join(f"{n} {s.lower()}" for s in SEVERITIES if (n := counter.get(s, 0))) or "none"


def main(folder):
    out = ["## Security checks (non-blocking)", ""]
    table = [("Tool", "Scope", "Findings", "Detail"), ("---", "---", "---:", "---")]

    gitleaks = load(folder / "gitleaks.json")
    if gitleaks is None:
        table.append(("gitleaks", "history of HEAD", "no report", ""))
    else:
        by_rule = Counter(f["RuleID"] for f in gitleaks)
        table.append(("gitleaks", "history of HEAD", len(gitleaks),
                      ", ".join(f"{n} {r}" for r, n in by_rule.most_common()) or "none"))

    for label, name in (("production deps", "npm-audit-prod.json"), ("all deps", "npm-audit-all.json")):
        audit = load(folder / name)
        if audit is None or "metadata" not in audit:
            table.append(("npm audit", f"web, {label}", "no report", ""))
            continue
        counts = audit["metadata"]["vulnerabilities"]
        detail = ", ".join(f"{counts.get(k, 0)} {k}" for k in ("critical", "high", "moderate", "low") if counts.get(k, 0))
        table.append(("npm audit", f"web, {label}", counts.get("total", 0), detail or "none"))

    osv = load(folder / "osv.json")
    rows = osv_rows(osv) if osv is not None else []
    for component in COMPONENTS:
        mine = [r for r in rows if r["component"] == component]
        if osv is None:
            table.append(("OSV-Scanner", component, "no report", ""))
        else:
            table.append(("OSV-Scanner", component, len(mine), count_line(Counter(r["severity"] for r in mine))))

    out += ["| " + " | ".join(str(c) for c in row) + " |" for row in table]

    serious = [r for r in rows if r["severity"] in ("CRITICAL", "HIGH")]
    out += ["", f"### HIGH and CRITICAL (OSV-Scanner): {len(serious)}", ""]
    if serious:
        serious.sort(key=lambda r: (SEVERITIES.index(r["severity"]), r["component"], r["package"], r["id"]))
        out += ["| Component | Package | Version | ID | CVE | Severity | Fixed in |", "|---|---|---|---|---|---|---|"]
        for r in serious[:MAX_ROWS]:
            out.append(f"| {r['component']} | {r['package']} | {r['version']} | {r['id']} | {r['cve']} "
                       f"| {r['severity']} ({r['score']}) | {r['fixed']} |")
        if len(serious) > MAX_ROWS:
            out.append(f"\n{len(serious) - MAX_ROWS} more rows are in `osv.json`.")
    else:
        out.append("None." if osv is not None else "No OSV report.")

    out += ["", "### SBOM (CycloneDX)", ""]
    boms = sorted(folder.glob("sbom-*.cdx.json"))
    if not boms:
        out.append("No SBOM was produced.")
    else:
        out += ["| File | Components | Without a declared licence |", "|---|---:|---:|"]
        for path in boms:
            bom = load(path) or {}
            components = bom.get("components", [])
            out.append(f"| {path.name} | {len(components)} | {sum(1 for c in components if not c.get('licenses'))} |")

    print("\n".join(out))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: security-summary.py <reports-dir>")
    main(Path(sys.argv[1]))
