"""Статичні метрики коду для порівняння версій «до/після» реінжинірингу.

Використання: python3 tools/metrics.py <корінь-проєкту> [<корінь-проєкту> ...]

Метрики:
  * кількість файлів/класів і рядків коду Java (без порожніх рядків і коментарів);
  * рядки шаблонів Thymeleaf і кількість копій навігаційної панелі;
  * цикломатична складність методів (наближено: 1 + кількість розгалужень);
  * залежності контролерів від репозиторіїв / JPA-сутностей;
  * еферентна (Ce) та аферентна (Ca) зв'язність пакетів і нестабільність I = Ce / (Ca + Ce) за Р. Мартіном;
  * секрети у конфігурації та коді;
  * кількість активних автоматизованих тестів.
"""
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

BRANCH = re.compile(r"\b(if|for|while|case|catch)\b|&&|\|\||\?(?!\s*>)")
METHOD = re.compile(r"^\s*(public|protected|private|static|\s)*[\w<>\[\],\s]+\s+(\w+)\s*\([^;]*\)\s*(throws [\w.,\s]+)?\{\s*$")


def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def code_lines(src: str) -> list[str]:
    return [l for l in strip_comments(src).splitlines() if l.strip()]


def methods_complexity(src: str) -> list[int]:
    """Наближений підрахунок складності: тіло методу визначається балансом фігурних дужок."""
    lines = strip_comments(src).splitlines()
    result, i = [], 0
    while i < len(lines):
        if METHOD.match(lines[i]) and not re.match(r"^\s*(if|for|while|switch|catch|else)\b", lines[i]):
            depth, body = 0, []
            for j in range(i, len(lines)):
                depth += lines[j].count("{") - lines[j].count("}")
                body.append(lines[j])
                if depth == 0:
                    break
            result.append(1 + sum(len(BRANCH.findall(l)) for l in body[1:]))
            i = j + 1
        else:
            i += 1
    return result


def package_of(path: Path, base: Path) -> str:
    rel = path.relative_to(base).parts[:-1]
    return ".".join(rel[:1]) if rel else "(root)"


def analyse(root: Path) -> dict:
    java_main = root / "src/main/java"
    base = next(p for p in java_main.rglob("*") if p.is_dir() and p.name == "mini_banking_app")
    files = sorted(base.rglob("*.java"))
    templates = sorted((root / "src/main/resources/templates").rglob("*.html"))
    tests = sorted((root / "src/test/java").rglob("*Test.java"))

    loc, complexities, per_file = 0, [], {}
    ce = defaultdict(set)
    controller_data_deps = []
    for f in files:
        src = f.read_text(encoding="utf-8")
        n = len(code_lines(src))
        loc += n
        per_file[str(f.relative_to(base))] = n
        complexities += methods_complexity(src)
        pkg = package_of(f, base)
        for imp in re.findall(r"^import\s+com\.mini_banking_app\.([\w.]+);", src, re.M):
            target = imp.split(".")[0]
            if target != pkg and not imp[0].isupper():
                ce[pkg].add(target)
        if "@Controller" in src:
            for imp in re.findall(r"^import\s+([\w.]+);", src, re.M):
                if ".repository." in imp or ".jpaentity." in imp or ".infrastructure." in imp \
                        or imp.startswith("jakarta.persistence") or imp.startswith("org.springframework.data"):
                    controller_data_deps.append(f"{f.name} -> {imp.split('.')[-1]}")

    packages = sorted({package_of(f, base) for f in files})
    ca = {p: {q for q in packages if p in ce[q]} for p in packages}
    coupling = {p: {"Ce": len(ce[p]), "Ca": len(ca[p]),
                    "I": round(len(ce[p]) / (len(ce[p]) + len(ca[p])), 2) if (ce[p] or ca[p]) else 0.0}
                for p in packages}

    tpl_loc = sum(len([l for l in t.read_text(encoding="utf-8").splitlines() if l.strip()]) for t in templates)
    navbars = sum(t.read_text(encoding="utf-8").count('<nav class="navbar') for t in templates)

    secrets = []
    for cfg in list((root / "src/main/resources").glob("application*.y*ml")) + files:
        for i, line in enumerate(cfg.read_text(encoding="utf-8").splitlines(), 1):
            if re.search(r"password:\s*[^\s$#{}]+\s*$", line) or re.search(r'encode\("[^"]+"\)', line):
                if "local" not in cfg.name and "Seeder" not in cfg.name:
                    secrets.append(f"{cfg.name}:{i}")
                elif "Seeder" in cfg.name:
                    secrets.append(f"{cfg.name}:{i} (лише профіль local)")

    active_tests = 0
    for t in tests:
        src = t.read_text(encoding="utf-8")
        if "@Disabled" in src:
            continue
        active_tests += len(re.findall(r"@Test\b", src))

    cc_sorted = sorted(complexities)
    return {
        "java_files": len(files),
        "java_loc": loc,
        "max_file_loc": max(per_file.items(), key=lambda kv: kv[1]),
        "methods": len(complexities),
        "cc_avg": round(sum(complexities) / len(complexities), 2),
        "cc_max": cc_sorted[-1],
        "methods_cc_gt_5": sum(1 for c in complexities if c > 5),
        "template_files": len(templates),
        "template_loc": tpl_loc,
        "navbar_copies": navbars,
        "controller_data_layer_deps": controller_data_deps,
        "packages": coupling,
        "secrets": secrets,
        "test_files": len(tests),
        "active_tests": active_tests,
    }


if __name__ == "__main__":
    out = {Path(p).name: analyse(Path(p)) for p in sys.argv[1:]}
    print(json.dumps(out, ensure_ascii=False, indent=2))
