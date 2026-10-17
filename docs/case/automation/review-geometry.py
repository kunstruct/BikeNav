"""Reproduce review diagnoses and common metrics; no physical-performance inference.
Run with either design's recorded CadQuery environment from any directory.
"""
from pathlib import Path
import sys,json,tempfile,math,hashlib
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'codex-work-models/source'))
sys.path.insert(0,str(ROOT/'assistant-models/cad'))
import design as O
import model as I
import trimesh
O.build()
s=O.case_front('compact').translate((0,0,3.5))
exact_before=O.exact_bounds(s)
with tempfile.TemporaryDirectory(prefix='bikenav-review-') as td:
 p=Path(td)/'front.stl';O.cq.exporters.export(s,str(p),tolerance=.035,angularTolerance=.12)
 mesh=trimesh.load_mesh(p);b=s.BoundingBox()
 compact={'exact_before':exact_before,'old_default_after_export':[b.xmin,b.ymin,b.zmin,b.xmax,b.ymax,b.zmax],'exact_after':O.exact_bounds(s),'mesh_bounds':mesh.bounds.tolist(),'mesh_watertight':bool(mesh.is_watertight),'mesh_components':len(mesh.split(only_watertight=False)),'cad_volume_mm3':s.Volume(),'mesh_volume_mm3':float(mesh.volume)}
g=I.seam_gasket();before=[]
for x in (-34,34):
 for y in (-43,43):before.append(I.cyl(3.4,.7,x,y,17.5).intersect(g).Volume())
after=I.bezel().intersect(g).Volume()
# Actual original straight-aperture obstacle is independently reconstructed from
# the archived pre-review dimensions. Restrict it to the window vicinity.
old_lip=I.box(60,76,2.6,0,3,19.4).cut(I.rr(45,59.4,.8,4,19,0,3))
finger_before=[];finger_after=[]
for x,y in [(sx*21.6,3+sy*28.8) for sx in (-1,1) for sy in (-1,1)]:
 f=I.cq.Solid.makeSphere(8,I.cq.Vector(x,y,27.2),angleDegrees1=-90,angleDegrees2=90)
 finger_before.append(old_lip.intersect(f).Volume());finger_after.append(I.bezel().intersect(f).Volume())
orig_names=['front_sun','rear_shell','floating_carrier','cage_front','thumbwheel','thumbwheel_cap','clamp_upper','clamp_lower','angle_adapter_20','bushing_half','liner_half','seam_gasket']
ip=I.printable_parts();ind_names={'rear_sun':1,'bezel_sun':1,'board_tray':1,'clamp_upper_base':1,'clamp_lower':1,'carrier':1,'captive_cap':1,'nut_keeper':1,'tpu_bushing':4,'tpu_liner_upper':1,'tpu_liner_lower':1}
orig_vol={mat:sum(O.PARTS[n]['shape'].Volume()*O.PARTS[n]['quantity'] for n in orig_names if O.PARTS[n]['material']==mat) for mat in ['rigid','TPU']}
ind_vol={mat:sum(ip[n].Volume()*q for n,q in ind_names.items() if ('TPU' if n.startswith('tpu_') else 'rigid')==mat) for mat in ['rigid','TPU']}
_,_,top=O.clamp();_,zc=O.wedge(20)
metrics={'codex-work-models':{'printable_stl_files':len(O.PARTS),'sun_printed_pieces':sum(O.PARTS[n]['quantity'] for n in orig_names),'sun_printed_volume_mm3':orig_vol,'case_face_mm':[90,74],'assumed_straight_bar_mm':35,'glass_face_centre_above_bar_axis_mm':top+zc+33.5*math.cos(math.radians(20)),'removal_travel_to_clear_mm':36.35},'assistant-models':{'printable_stl_files':len(ip),'sun_printed_pieces':sum(ind_names.values()),'sun_printed_volume_mm3':ind_vol,'case_face_mm':[96,78],'assumed_straight_bar_mm':40,'glass_face_centre_above_bar_axis_mm':18.5+49,'removal_travel_to_clear_mm':59}}
report={'original_compact_bounds_diagnosis':compact,'independent_gasket':{'old_seat_intersections_mm3':before,'old_total_mm3':sum(before),'corrected_bezel_intersection_mm3':after},'independent_finger':{'old_straight_lip_corner_overlaps_mm3':finger_before,'reviewed_bezel_corner_overlaps_mm3':finger_after},'common_metrics':metrics,'limits':['Nominal CAD dimensions/volumes, not measured prints or strength evidence.','Rigid volume excludes hardware/board/seals; original TPU volume includes optional printed gasket, independent uses purchased silicone.','Metrics use sun variants, original 20-degree adapter; sightline is not a measured bike fit.']}
(ROOT/'automation/review-evidence.json').write_text(json.dumps(report,indent=2)+'\n')
assert compact['mesh_watertight'] and max(abs(a-b) for a,b in zip(compact['exact_before'],compact['exact_after']))<1e-5
assert sum(before)>4 and after<1e-5 and max(finger_after)<1e-4
print(json.dumps(report,indent=2))
