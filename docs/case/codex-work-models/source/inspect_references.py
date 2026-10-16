"""Read-only measurement of upstream meshes and manufacturer STEP; mm."""
from pathlib import Path
import json, hashlib
import numpy as np
import trimesh
import cadquery as cq
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
ROOT=Path(__file__).resolve().parents[1]
report={}
fig,axes=plt.subplots(2,4,figsize=(18,9))
for row,name in enumerate(['MiniNav-Frame-with-button.stl','MiniNav-back-panel-with-strap-slits.stl']):
 p=ROOT.parent/name
 m=trimesh.load_mesh(p)
 comps=m.split(only_watertight=False)
 d={'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bounds_mm':m.bounds.tolist(),'extents_mm':m.extents.tolist(),'watertight':m.is_watertight,'volume_mm3':float(m.volume),'components':[{'faces':len(c.faces),'bounds':c.bounds.tolist(),'volume':float(c.volume)} for c in comps]}
 d['dominant_planes']={}
 for i,axis in enumerate('xyz'):
  mask=np.abs(m.face_normals[:,i])>.99999
  val=np.round(m.triangles_center[mask,i],3)
  vs=np.unique(val)
  areas=[(float(v),float(m.area_faces[mask][val==v].sum())) for v in vs]
  d['dominant_planes'][axis]=sorted(areas,key=lambda x:-x[1])[:35]
 zs=[.6,2,5,13] if row==0 else [.15,.5,.75,1.4]
 for ax,z in zip(axes[row],zs):
  sec=m.section(plane_origin=[0,0,z],plane_normal=[0,0,1])
  if sec:
   for points in sec.discrete: ax.plot(points[:,0],points[:,1],lw=.6)
  ax.set_title(f'{name.split("-")[1]} z={z:.2f} mm'); ax.set_aspect('equal'); ax.grid(alpha=.2)
 if row==1:
  sec=m.section(plane_origin=[0,0,.75],plane_normal=[0,0,1]);holes=[]
  for points in sec.discrete:
   mn=points.min(0);mx=points.max(0);c=(mn+mx)/2
   if abs(c[0])>15 and abs(c[1])>25 and max(mx-mn)<10:
    holes.append({'centre_mm':c.tolist(),'diameters_mm':(mx-mn).tolist()})
  d['rear_holes_at_z_0p75']=holes
 report[name]=d
fig.tight_layout();fig.savefig(ROOT/'reference/original-sections.png',dpi=160);plt.close(fig)
s=cq.importers.importStep(str(ROOT/'reference/waveshare-2.8-2024.stp')).val()
solids=[]
for i,so in enumerate(s.Solids()):
 b=so.BoundingBox()
 solids.append({'index':i,'volume':so.Volume(),'bounds':[b.xmin,b.ymin,b.zmin,b.xmax,b.ymax,b.zmax]})
report['manufacturer_step']={'bounds':str(s.BoundingBox()),'solids':solids}
(ROOT/'reports/reference-measurements.json').write_text(json.dumps(report,indent=2))
for k,d in report.items():
 if 'bounds_mm' in d:
  print(k,json.dumps(d,indent=2))
print('STEP solids',len(solids))
for d in sorted(solids,key=lambda x:-x['volume'])[:45]:print(d)
