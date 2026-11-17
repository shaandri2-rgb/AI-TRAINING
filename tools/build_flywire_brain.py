#!/usr/bin/env python3
"""Pack the complete FlyWire FAFB v783 Shiu-model network into a compact CSR asset.

Input files are the published Connectivity_783.parquet and Completeness_783.csv
from philshiu/Drosophila_brain_model. This preserves every neuron and every
connection in that model; the Android runtime uses a simplified fixed-step LIF
approximation and is not a bit-for-bit Brian2 reproduction.
"""
import argparse, struct
from pathlib import Path
import numpy as np
import pandas as pd

p=argparse.ArgumentParser()
p.add_argument("--completeness",required=True)
p.add_argument("--connectivity",required=True)
p.add_argument("--out",required=True)
a=p.parse_args()

comp=pd.read_csv(a.completeness,index_col=0)
ids=pd.to_numeric(pd.Index(comp.index),errors="raise").to_numpy(dtype=np.int64)
if len(ids)<100000 or len(ids)>200000:
    raise SystemExit(f"Unexpected FAFB v783 neuron count: {len(ids)}")
con=pd.read_parquet(a.connectivity,columns=["Presynaptic_Index","Postsynaptic_Index","Excitatory x Connectivity"])
pre=pd.to_numeric(con["Presynaptic_Index"],errors="raise").to_numpy(dtype=np.int64)
post=pd.to_numeric(con["Postsynaptic_Index"],errors="raise").to_numpy(dtype=np.int64)
weight=pd.to_numeric(con["Excitatory x Connectivity"],errors="raise").to_numpy(dtype=np.float32)
n=len(ids)
if len(pre)<1000000 or len(pre)>30000000:
    raise SystemExit(f"Unexpected FAFB v783 connection count: {len(pre)}")
if pre.min()<0 or post.min()<0 or pre.max()>=n or post.max()>=n:
    raise SystemExit("Connectivity indices do not match Completeness_783.csv ordering")
order=np.argsort(pre,kind="stable")
pre=pre[order]; post=post[order]; weight=weight[order]
counts=np.bincount(pre,minlength=n).astype(np.int64)
offsets=np.empty(n+1,dtype=np.int64); offsets[0]=0; np.cumsum(counts,out=offsets[1:])
if offsets[-1]!=len(pre): raise SystemExit("CSR index construction failed")
out=Path(a.out); out.parent.mkdir(parents=True,exist_ok=True)
with out.open("wb") as f:
    f.write(b"FLYW")
    f.write(struct.pack("<III",1,n,len(pre)))
    f.write(ids.astype("<i8",copy=False).tobytes())
    f.write(offsets.astype("<i4",copy=False).tobytes())
    f.write(post.astype("<i4",copy=False).tobytes())
    f.write(weight.astype("<f4",copy=False).tobytes())
print(f"Packed full FAFB v783 network: {n:,} neurons, {len(pre):,} directed connections")
print(f"Asset: {out} ({out.stat().st_size:,} bytes)")
print("Weights retain the published Shiu model's signed Excitatory x Connectivity values.")
