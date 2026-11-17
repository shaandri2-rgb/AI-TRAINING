#!/usr/bin/env python3
"""Build a compact, real BANC v888 leg-sensor/interneuron/motor subnetwork.

The large public source tables are fetched during CI. Only the selected neuron
IDs and their measured directed synapse counts are packaged in the APK.
"""
from __future__ import annotations
import argparse
import random
import struct
from pathlib import Path

import pandas as pd

parser = argparse.ArgumentParser()
parser.add_argument("--meta", required=True)
parser.add_argument("--edges", required=True)
parser.add_argument("--out", required=True)
args = parser.parse_args()

meta = pd.read_feather(args.meta)
edges = pd.read_feather(args.edges)
required_meta = {"root_id", "body_part_sensory", "cell_function", "body_part_effector"}
required_edges = {"pre", "post", "count"}
if not required_meta.issubset(meta.columns):
    raise SystemExit(f"BANC metadata is missing columns: {sorted(required_meta - set(meta.columns))}")
if not required_edges.issubset(edges.columns):
    raise SystemExit(f"BANC edge table is missing columns: {sorted(required_edges - set(edges.columns))}")

for col in ("root_id",):
    meta[col] = meta[col].astype(str)
for col in ("pre", "post"):
    edges[col] = edges[col].astype(str)

pre_ids = set(edges["pre"])
post_ids = set(edges["post"])

sensory_mask = (
    meta["body_part_sensory"].isin(["front_leg", "middle_leg", "hind_leg"])
    | meta["cell_function"].isin(["tactile", "proprioception"])
)
motor_mask = (
    (meta["cell_function"] == "leg_motor")
    | meta["body_part_effector"].isin(["front_leg", "middle_leg", "hind_leg"])
)
sensory = sorted(set(meta.loc[sensory_mask, "root_id"]) & pre_ids)
motor = sorted(set(meta.loc[motor_mask, "root_id"]) & post_ids)

if len(sensory) < 18 or len(motor) < 12:
    raise SystemExit(f"Need 18 leg sensory and 12 leg motor cells; found sensory={len(sensory)}, motor={len(motor)}")

# Keep the selected circuit small enough for real-time mobile simulation.
sensory = sensory[:18]
sensory_set = set(sensory)
motor = [nid for nid in motor if nid not in sensory_set][:12]
if len(motor) < 12:
    raise SystemExit(f"Could not select 12 distinct motor cells; found {len(motor)}")
posts_of_sensory = set(edges.loc[edges["pre"].isin(sensory), "post"])
pres_of_motor = set(edges.loc[edges["post"].isin(motor), "pre"])
inter_candidates = sorted(posts_of_sensory & pres_of_motor - set(sensory) - set(motor))
if len(inter_candidates) < 20:
    raise SystemExit(f"Need 20 connected sensory-to-motor interneurons; found {len(inter_candidates)}.")
rng = random.Random(7)
inter = sorted(rng.sample(inter_candidates, 20))

ids = sensory + inter + motor
n_s, n_i, n_m = len(sensory), len(inter), len(motor)
n = len(ids)
index = {nid: i for i, nid in enumerate(ids)}

sub = edges[edges["pre"].isin(ids) & edges["post"].isin(ids)]
weights = [[0.0 for _ in range(n)] for _ in range(n)]
for row in sub.itertuples(index=False):
    i, j = index[str(row.pre)], index[str(row.post)]
    weights[i][j] += float(row.count)

# Normalise each presynaptic neuron's outgoing synaptic counts. The LIF
# dynamics and gain are explicitly a calibrated simulation, not biological
# time constants or a claim that the whole fly brain is reproduced.
for i in range(n):
    total = sum(weights[i])
    if total > 0:
        weights[i] = [v / total for v in weights[i]]

# Each motor neuron is assigned to one of six fly leg groups:
# front-left/right, middle-left/right, hind-left/right.
meta_by_id = meta.drop_duplicates("root_id").set_index("root_id")
motor_leg = []
for k, nid in enumerate(motor):
    part = str(meta_by_id.loc[nid].get("body_part_effector", "")) if nid in meta_by_id.index else ""
    base = 0 if part == "front_leg" else 2 if part == "middle_leg" else 4
    motor_leg.append(base + (k % 2))

out = Path(args.out)
out.parent.mkdir(parents=True, exist_ok=True)
with out.open("wb") as f:
    f.write(b"BANC")
    f.write(struct.pack("<IIII", 1, n_s, n_i, n_m))
    for nid in ids:
        f.write(struct.pack("<Q", int(nid)))
    for leg in motor_leg:
        f.write(struct.pack("<i", int(leg)))
    for row in weights:
        f.write(struct.pack("<" + "f" * n, *row))

print(f"Created {out} ({out.stat().st_size:,} bytes)")
print(f"BANC v888 subnetwork: {n} neurons = {n_s} sensory + {n_i} interneurons + {n_m} motor")
print(f"Measured directed connections retained: {int((sub['count'] > 0).sum()):,}")
print("Dataset: Harvard Dataverse DOI 10.7910/DVN/7WTH1N (BANC v888)")
print("Dataset licence: CC BY 4.0; neural dynamics are a calibrated LIF approximation.")
