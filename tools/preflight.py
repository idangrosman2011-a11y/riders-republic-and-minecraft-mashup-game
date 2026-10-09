#!/usr/bin/env python3
"""Lay every sheet over each other: report empty cells, unresolved cross-sheet references,
unverified Riders Republic rows and hooks whose code is missing. Exit 1 if anything blocks a build."""
import json, os, sys, glob
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
S = {os.path.basename(p)[:-5]: json.load(open(p)) for p in glob.glob(os.path.join(ROOT, "sheets", "*.json"))}
errors, unverified = [], []
def ids(name): return {r["id"] for r in S[name]["rows"]}

for name, sh in S.items():
    for r in sh["rows"]:
        for c in sh["columns"]:
            v = r.get(c)
            if v is None or v == "" or v == []:
                errors.append(f"{name}.{r.get('id')}.{c}: empty")
        for c in r:
            if c not in sh["columns"]:
                errors.append(f"{name}.{r.get('id')}.{c}: column not declared")

rr, controls = ids("rr_content"), ids("controls")
sports = {r["sport"] for r in S["gear"]["rows"]}
for g in S["gear"]["rows"]:
    for col in ("rrModel", "rrSound"):
        if g[col] != "none" and g[col] not in rr:
            errors.append(f"gear.{g['id']}.{col}: '{g[col]}' not in rr_content")
for t in S["tricks"]["rows"]:
    for s in t["sports"]:
        if s not in sports: errors.append(f"tricks.{t['id']}.sports: unknown sport '{s}'")
    if t["input"] != "none":
        for alt in t["input"].split("|"):
            for k in alt.split("+"):
                if k not in controls: errors.append(f"tricks.{t['id']}.input: unknown control '{k}'")
    src = t["rrCallout"].split("#")[0]
    if src not in rr: errors.append(f"tricks.{t['id']}.rrCallout: '{src}' not in rr_content")
# two tricks of the same sport must not share an input
seen = {}
for t in S["tricks"]["rows"]:
    if t["kind"] in ("flip", "spin", "meta", "proximity"): continue
    for s in t["sports"]:
        for alt in t["input"].split("|"):
            key = (s, "+".join(sorted(alt.split("+"))))
            if key in seen: errors.append(f"tricks.{t['id']}.input: same input as {seen[key]} for {s}")
            seen[key] = t["id"]
for h in S["hooks"]["rows"]:
    p = os.path.join(ROOT, h["sourceFile"])
    if not os.path.exists(p): errors.append(f"hooks.{h['id']}.sourceFile: {h['sourceFile']} missing")
    elif h["anchor"] not in open(p, encoding="utf-8").read(): errors.append(f"hooks.{h['id']}.anchor: '{h['anchor']}' not in file")
for r in S["rr_content"]["rows"]:
    if r["verified"] is not True: unverified.append(f"rr_content.{r['id']}: not yet extracted from a real Riders Republic install")

print(f"{len(S)} sheets, {sum(len(s['rows']) for s in S.values())} rows")
for e in errors: print("BLOCK  ", e)
for u in unverified: print("UNVERIFIED", u)
print("preflight:", "CLEAN" if not errors else f"{len(errors)} blocking", f"/ {len(unverified)} unverified")
sys.exit(1 if errors else 0)
