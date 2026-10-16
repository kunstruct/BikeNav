"""Read only the two original meshes; retain sections, hashes, measured extents."""
from pathlib import Path
import json,hashlib
import numpy as np
import trimesh
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
ROOT=Path(__file__).resolve().parents[1]
fig,axs=plt.subplots(2,4,figsize=(16,10))
result={}
for row,name in enumerate(['MiniNav-Frame-with-button.stl','MiniNav-back-panel-with-strap-slits.stl']):
 p=ROOT.parent/name; m=trimesh.load_mesh(p)
 d={'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bounds_mm':m.bounds.tolist(),'faces':len(m.faces),'volume_mm3':float(m.volume),'watertight':bool(m.is_watertight),'connected_components':len(m.split(only_watertight=False)),'sections':{}}
 heights=[.75,2.0,8.0,12.8] if row==0 else [.25,.75,1.25,1.45]
 for ax,z in zip(axs[row],heights):
  section=m.section([0,0,1],[0,0,z]);loops=[]
  if section:
   for loop in section.discrete:
    ax.plot(loop[:,0],loop[:,1],lw=.7)
    loops.append({'bounds_xy':np.array([loop[:,:2].min(0),loop[:,:2].max(0)]).round(4).tolist(),'length_mm':round(float(np.linalg.norm(np.diff(loop,axis=0),axis=1).sum()),4)})
  d['sections'][str(z)]=loops;ax.set_aspect('equal');ax.grid(alpha=.2);ax.set_title(f'{"Frame" if row==0 else "Back"}: z={z:g} mm');ax.set_xlabel('Original X / mm');ax.set_ylabel('Original Y / mm')
 result[name]=d
(ROOT/'evidence/reference-measurements.json').write_text(json.dumps(result,indent=2)+'\n')
fig.suptitle('Actual original STL intersections — original coordinates, not new design',fontsize=16)
fig.tight_layout();fig.savefig(ROOT/'evidence/reference-sections.png',dpi=150)
print(json.dumps(result,indent=2))
