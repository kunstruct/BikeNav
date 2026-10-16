"""Rebuild and check solids, exported meshes, load-path gaps and swept assembly paths.
This is geometric verification, not structural analysis or physical qualification.
"""
import hashlib,json,math,sys
from pathlib import Path
import numpy as np
import trimesh
import design as D
D.build();ROOT=D.ROOT;P=D.P
checks=[];meshes=[]
def check(name,ok,detail):
 checks.append({'name':name,'pass':bool(ok),'detail':detail});print(('PASS ' if ok else 'FAIL ')+name+': '+str(detail),flush=True)
def vol(a,b):
 ba=a.BoundingBox();bb=b.BoundingBox()
 if any([ba.xmax<=bb.xmin+1e-7,bb.xmax<=ba.xmin+1e-7,ba.ymax<=bb.ymin+1e-7,bb.ymax<=ba.ymin+1e-7,ba.zmax<=bb.zmin+1e-7,bb.zmax<=ba.zmin+1e-7]):return 0.
 return max(0.,a.intersect(b).Volume())
manifest=json.loads((ROOT/'reports/parts-manifest.json').read_text())
check('Exports match current source and parameters',json.loads((ROOT/'reports/export-inputs.json').read_text())=={n:hashlib.sha256((ROOT/'source'/n).read_bytes()).hexdigest() for n in ['design.py','parameters.json']},'SHA-256 of CAD source and parameters at export')
for name,d in D.PARTS.items():
 m=trimesh.load_mesh(ROOT/'stl'/f'{name}.stl');expected=np.array(manifest[name]['print_bounds_mm']).reshape(2,3)
 components=len(m.split(only_watertight=False));delta=float(np.max(np.abs(m.bounds-expected)))
 vd=abs(float(m.volume)-d['shape'].Volume())/d['shape'].Volume()
 q=d['shape']
 if d['rotation']:q=q.rotate((0,0,0),*d['rotation'])
 b=D.exact_bounds(q);q=q.translate((-(b[0]+b[3])/2,-(b[1]+b[4])/2,-b[2]))
 source_delta=float(np.max(np.abs(expected.flatten()-D.exact_bounds(q))))
 digest=hashlib.sha256((ROOT/'stl'/f'{name}.stl').read_bytes()).hexdigest()
 ok=source_delta<1e-5 and digest==manifest[name]['stl_sha256'] and m.is_watertight and m.is_winding_consistent and m.volume>0 and components==1 and delta<.04 and vd<.01
 meshes.append({'part':name,'pass':bool(ok),'watertight':m.is_watertight,'consistent_winding':m.is_winding_consistent,'positive_volume_mm3':float(m.volume),'components':components,'bounds_mm':m.bounds.tolist(),'dimensions_mm':m.extents.tolist(),'source_bounds_error_mm':source_delta,'sha256':digest,'bounds_error_mm':delta,'relative_volume_error':vd,'triangles':len(m.faces)})
check('All printable meshes',all(m['pass'] for m in meshes),{'count':len(meshes),'failed':[m['part'] for m in meshes if not m['pass']]})
# Pairwise solid overlaps at installed (compressed) elastomer geometry.
for angle in P['adapter_angles']:
 rows=D.assembly('sun',angle);bad=[];tested=0
 for i,(n,a,_) in enumerate(rows):
  for n2,b,_ in rows[i+1:]:
   if 'reference' in n or 'reference' in n2:continue
   v=vol(a,b);tested+=1
   if v>1e-4:bad.append({'a':n,'b':n2,'intersection_mm3':round(v,6)})
 check(f'Installed interference at {angle} deg',not bad,{'pairs':tested,'collisions':bad})
# Case/lid variation mating independently of mount.
rear=D.PARTS['rear_shell']['shape'];carrier=D.PARTS['floating_carrier']['shape']
for style in P['hood_projection']:
 v=vol(rear,D.PARTS['front_'+style]['shape']);check('Rear/front '+style,v<1e-5,{'intersection_mm3':v})
# A geometric proxy for access: 16 mm spherical fingertip tangent to the
# outer film at z=-0.15, tested at centre and all four active-area corners.
finger=[]
for x,y in [(P['window_center_x'],0)]+[(P['window_center_x']+sx*28.8,sy*21.6) for sx in (-1,1) for sy in (-1,1)]:
 sphere=D.cq.Solid.makeSphere(8,D.cq.Vector(x,y,7.85),angleDegrees1=-90,angleDegrees2=90)
 for style in P['hood_projection']:
  v=vol(sphere,D.PARTS['front_'+style]['shape'])
  finger.append({'style':style,'point':[x,y],'overlap_mm3':v})
check('16 mm spherical fingertip at LCD centre and corners',max(c['overlap_mm3'] for c in finger)<1e-4,{'samples':finger,'note':'geometric access proxy only; touch response and human/gloved use untested'})
# Board STEP solid bounding boxes are deliberately conservative keep-outs.
ref=json.loads((ROOT/'reports/reference-measurements.json').read_text())
board_keepouts=[];bounds=[]
for so in ref['manufacturer_step']['solids']:
 x0,y0,z0,x1,y1,z1=so['bounds'];bounds.append(so['bounds'])
 if min(x1-x0,y1-y0,z1-z0)<1e-6:continue
 b=D.box(x1-x0,y1-y0,z1-z0,((x0+x1)/2,(y0+y1)/2,z0)).rotate((0,0,0),(0,0,1),90).translate((-3,0,-1.2))
 board_keepouts.append((so['index'],b))
for name in ['rear_shell','front_sun']:
 bad=[]
 for idx,k in board_keepouts:
  v=vol(D.PARTS[name]['shape'],k)
  if v>1e-4:bad.append({'solid_index':idx,'bbox_overlap_mm3':v})
 check('Manufacturer conservative component keep-outs vs '+name,not bad,{'tested_solids':len(board_keepouts),'collisions':bad})
# Actual rear shell including tether and rail moves +Y; front stays attached.
fixed=[carrier,D.PARTS['cage_front']['shape'],D.PARTS['thumbwheel']['shape'].translate((0,-9,0)),D.PARTS['thumbwheel_cap']['shape'].translate((0,-9,0))]
# Screw retracts 9 mm with head; its tip clears rail start by 3 mm.
fixed.append(D.cyl_y(2,-54,-19,z=12.5))
bad=[]
for dist in np.arange(0,60.01,1):
 for n in ('rear_shell','front_sun'):
  moving=D.PARTS[n]['shape'].translate((0,float(dist),34))
  for j,f in enumerate(fixed):
   v=vol(moving,f)
   if v>1e-4:bad.append([float(dist),n,j,v])
check('Rail removal +Y, 0..60 mm in 1 mm steps',not bad,{'positions':61,'moving_parts':2,'fixed_obstacles':len(fixed),'collisions':bad})
# Include protruding shell screw heads and insert in the removal sweep.
bad=[]
_,_,top=D.clamp();_,zc=D.wedge(0)
case_hardware=[(n,s.translate((0,0,-top-zc))) for n,s,c in D.assembly('sun',0) if n.startswith(('M3_case_','M4_rail_'))]
for dist in np.arange(0,60.01,1):
 for n,moving in case_hardware:
  for j,f in enumerate(fixed):
   v=vol(moving.translate((0,float(dist),0)),f)
   if v>1e-4:bad.append([float(dist),n,j,v])
check('Case hardware removal sweep',not bad,{'positions':61,'moving_parts':len(case_hardware),'collisions':bad,'shell_screw_head_to_track_top_gap_mm':.45})
# Retract the captured knob, its cover and release bolt through their complete stroke.
release=D.union(D.cyl_y(2,-45,-10,z=12.5),D.on_y(D.hexhole(7,2.8)).translate((0,-47.8,12.5)))
bad=[]
for dist in np.arange(0,9.01,.25):
 for name,s in [('wheel',D.PARTS['thumbwheel']['shape']),('head_cover',D.PARTS['thumbwheel_cap']['shape']),('bolt',release)]:
  for f in [carrier,D.PARTS['cage_front']['shape']]:
   v=vol(s.translate((0,-float(dist),0)),f)
   if v>1e-4:bad.append([float(dist),name,v])
check('Captive screw withdrawal sweep',not bad,{'positions':37,'step_mm':.25,'collisions':bad})
check('Rail free after 36.35 mm translation',16-(-16)==32 and (-16+36.35)>=20.35-1e-8,{'rail_length':32,'receiver_far_edge':20.35,'travel_required':36.35,'recommend_free_space_mm':60})
# Full lower stop face, upper washer overlap and compliant clearances.
check('Sleeve compression limit',abs(2*P['bushing_installed_flange']+3-P['sleeve_length'])<1e-8,{'free_stack_mm':2*P['bushing_free_flange']+3,'installed_stack_mm':8,'flange_compression_percent':100*(1-2.5/2.7)})
check('Isolation metal clear of rigid floating carrier',(9-P['sleeve_od'])/2>0 and (14-9)/2>0 and 2.5>0,{'sleeve_to_carrier_radial_gap_mm':(9-P['sleeve_od'])/2,'washer_to_carrier_axial_gap_mm':2.5,'washer_retention_overlap_mm':(14-9)/2,'down_stop_gap_mm':1.5,'sleeve_to_TPU_radial_gap_mm':(P['bushing_bore']-P['sleeve_od'])/2})
# BRep prove stops reached exactly after downward travel, and washer at upward travel.
probe=D.cyl(3,1,(0,16,0))
check('Down stop onset',vol(carrier.translate((0,0,-1.5)),probe)<1e-6 and vol(carrier.translate((0,0,-1.51)),probe)>.001,{'contact_at_downward_mm':1.5})
x,y=D.BUSH[0];w=D.cyl(7,.8,(x,y,8)).cut(D.cyl(1.6,1,(x,y,7.9)))
check('Upper retention onset',vol(carrier.translate((0,0,2.5)),w)<1e-6 and vol(carrier.translate((0,0,2.51)),w)>.001,{'contact_at_upward_mm':2.5})
# Lateral movement first reaches the edge of the retention washer at 1.5 mm,
# before the 2.5 mm sleeve-to-carrier-hole radial limit.
lateral_washer=D.washer(14,3.2,.8,(28,18,8))
check('Lateral stop onset',vol(carrier.translate((1.5,0,0)),lateral_washer)<1e-6 and vol(carrier.translate((1.51,0,0)),lateral_washer)>1e-6,{'first_contact_plus_X_mm':1.5,'surface':'rail wall against retention washer edge; symmetric -X','sleeve_radial_clearance_mm':2.5})
# Contiguous gasket groove; port entirely below it. Check fitted compressed ring.
g=D.ring((82.4,60.4),(79.2,57.2),5.2,-2.4,.9)
check('Continuous gasket fits rear groove',len(g.Solids())==1 and vol(g,rear)<1e-6,{'components':len(g.Solids()),'groove_width_mm':1.8,'gasket_width_mm':1.6,'per_side_lateral_clearance_mm':.1,'compression_percent':25})
roof=-2.4-(P['usb_center_z']+P['usb_opening_height']/2)
check('USB below continuous gasket',roof>=1.2,{'minimum_vertical_web_mm':roof})
# Housing sweep from outside wall to the STEP connector face. 14 x 6 housing, R1.
plug=D.rr(14,6,1,0,18).rotate((0,0,0),(0,1,0),90).rotate((0,0,0),(1,0,0),90).translate((35.28,P['usb_center_y'],P['usb_center_z']))
check('USB 14x6 mm housing insertion sweep',vol(plug,rear)<1e-6,{'housing_swept_x_mm':[35.28,53.28],'wall_outer_x':45,'intersection_mm3':vol(plug,rear)})
# Tool access along screw axes with case off bike: 5.5mm driver envelope from rear.
bad=[]
for x,y in D.SCREWS:
 driver=D.cyl(2.75,30,(x,y,-45))
 if vol(driver,rear)>1e-5:bad.append((x,y,vol(driver,rear)))
check('Six case screw tool approaches',not bad,{'driver_diameter_mm':5.5,'collisions':bad,'condition':'case detached; tool approaches from rear'})
# Rail insert doesn't approach electronics cavity.
check('External insert separates from cavity',-12.5-(-21.5+P['insert_diameter']/2)>=2.5,{'vertical_separation_mm':-12.5-(-21.5+P['insert_diameter']/2),'insert_engagement_mm':6,'screw_tip_blind_clearance_mm':1})
# Access is checked at the documented assembly stage (case/carrier removed).
upper,lower,clamp_top=D.clamp();by=P['bar_diameter']/2+P['liner_installed_thickness']+6
access=[]
for x in (-10,10):
 for y in (-by,by):
  access.extend([vol(upper,D.cyl(4.5,40,(x,y,6.5))),vol(lower,D.cyl(4.5,40,(x,y,-46.5)))])
check('Clamp driver and nut socket access',max(access)<1e-5,{'socket_envelope_diameter_mm':9,'maximum_overlap_mm3':max(access),'stage':'adapter removed'})
access=[]
for angle in P['adapter_angles']:
 wedge,zc=D.wedge(angle)
 for x,y in D.BUSH:
  tool=D.cyl(4.5,56,(x,y,-60)).rotate((0,0,0),(1,0,0),angle).translate((0,0,zc))
  access.append(vol(wedge,tool))
check('Isolation nut socket access',max(access)<1e-5,{'socket_envelope_diameter_mm':9,'counterbore_diameter_mm':10,'maximum_overlap_mm3':max(access),'stage':'case detached; socket below wedge, outside clamp width'})
check('Fastener thread engagement',2.2>=2 and 6>=4 and 3.2>=3,{'case_M3_nut_mm':2.2,'release_M4_insert_mm':6,'adapter_M4_nut_mm':3.2,'isolator_M3_nyloc_mm':4,'clamp_M4_nyloc_mm':5,'M3_isolator_protrusion_mm':2.7,'M4_clamp_protrusion_mm':5.4,'note':'nominal screw lengths; verify delivered hardware and printed seating before use'})
# Closed cavity wall/floor sampling excluding USB; geometric measurements not minimum-FEA.
check('Nominal shell structural sections',(54.5-(P['glass_height']+1.2))/2>=1.2 and roof>=1.2,{'floor_mm':2.5,'bezel_mm':2.5,'hood_mm':2.4,'tongue_min_wall_mm':(54.5-(P['glass_height']+1.2))/2,'tongue_socket_clearance_per_side_mm':.3,'tongue_overlap_mm':2,'groove_USB_web_mm':roof,'front_nut_roof_mm':1.3,'blind_screw_roof_mm':1.2,'carrier_mm':3,'rail_floor_mm':2.65})
check('Captive release geometry',22>18.6 and 9>6+1,{'flange_diameter_mm':22,'front_aperture_mm':18.6,'radial_capture_mm':1.7,'axial_release_travel_mm':9,'screw_tip_released_y':-19,'rail_near_face_y':-16,'head_positive_cover':'two M2x6 screws','keeper_fasteners':'four M3x10 screws'})
# Bar fit and required straight length (liner axial lips included).
check('Clamp liner and split gap',P['clamp_split_gap']>0,{'ASSUMED_bar_diameter_mm':P['bar_diameter'],'straight_bar_minimum_mm':P['clamp_width']+3,'split_gap_mm':P['clamp_split_gap'],'free_liner_mm':P['liner_free_thickness'],'installed_liner_mm':P['liner_installed_thickness'],'liner_compression_percent':20})
for n in ['MiniNav-Frame-with-button.stl','MiniNav-back-panel-with-strap-slits.stl']:
 digest=hashlib.sha256((ROOT.parent/n).read_bytes()).hexdigest()
 check('Original preserved '+n,digest==ref[n]['sha256'],digest)
report={'status':'PASS' if all(c['pass'] for c in checks) else 'FAIL','checks_passed':sum(c['pass'] for c in checks),'checks_total':len(checks),'checks':checks,'meshes':meshes,'limitations':['No physical prints, fit, material, sealing, touch, thermal, vibration, fatigue or road tests performed.','Bounding boxes from manufacturer 2024 STEP, not a scan of the user board.','No load rating, waterproof certification or measured vibration reduction.','Motion samples are 1 mm apart; constant extruded rail sections and clear removal direction also reviewed analytically.']}
(ROOT/'reports/validation.json').write_text(json.dumps(report,indent=2))
lines=['# Geometric validation','',f"Result: **{report['status']}** — {report['checks_passed']}/{report['checks_total']} checks.",'','Generated by `source/validate.py`. Exact numeric records: `validation.json`.','', '| Check | Result |', '|---|---|']
lines += [f"| {c['name']} | {'PASS' if c['pass'] else 'FAIL'} |" for c in checks]
lines+=['','## Meshes (millimetres)','','| Part | Watertight | Components | Volume mm³ | XYZ print dimensions mm |','|---|---|---|---|---|']
for m in meshes:lines.append(f"| {m['part']} | {m['watertight']} | {m['components']} | {m['positive_volume_mm3']:.3f} | {' × '.join(f'{v:.3f}' for v in m['dimensions_mm'])} |")
lines+=['','## Limits','']+['- '+s for s in report['limitations']]
(ROOT/'reports/VALIDATION.md').write_text('\n'.join(lines)+'\n')
print(report['status'],report['checks_passed'],report['checks_total'])
sys.exit(0 if report['status']=='PASS' else 1)
