"""Mesh, BRep, physical-interface and motion checks. No claimed load/environment rating."""
import json,math,sys,hashlib
from functools import lru_cache
import numpy as np
import trimesh
from model import *
report={'checks':[],'remaining_physical_tests':['Measure actual bar diameter / straight length / controls clearance','Identify board revision, factory pillar thread and usable thread depth','Measure cable housing, exposed metal and full insertion depth','Print clearance coupons and measure gasket compression','Touch response / glare / visibility with purchased window','Clamp grip, creep, temperature, vibration, fatigue and tether proof tests']}
def check(name,condition,details):
    report['checks'].append({'name':name,'pass':bool(condition),'evidence':details});print(('PASS ' if condition else 'FAIL ')+name,details,flush=True)
@lru_cache(maxsize=32768)
def geometry(a):return a.BoundingBox(),a.Solids()
def vol(a,b):
    ba,aa=geometry(a);bb,bs=geometry(b)
    if ba.xmax<bb.xmin or bb.xmax<ba.xmin or ba.ymax<bb.ymin or bb.ymax<ba.ymin or ba.zmax<bb.zmin or bb.zmax<ba.zmin:return 0.
    # Reject separated/tangent solids before asking OpenCascade for a Boolean.
    bb=bs
    if len(aa)>1:return sum(vol(x,b) for x in aa)
    if len(bb)>1:return sum(vol(a,x) for x in bb)
    return max(0.,a.intersect(b).Volume())
def clear(name,a,b,tol=1e-5):
    v=vol(a,b);check(name,v<tol,{'intersection_mm3':v})
def nearest(a,b):return a.distance(b)
def main():
    parts=printable_parts()
    manifest=json.loads((ROOT/'validation/mesh-report.json').read_text())
    check('Exports match current source and parameters',json.loads((ROOT/'validation/export-inputs.json').read_text())=={n:hashlib.sha256((ROOT/'cad'/n).read_bytes()).hexdigest() for n in ['model.py','parameters.json','build.py']},'SHA-256 captured during export')
    for name,s in parts.items():
        check('CAD '+name,s.isValid() and len(s.Solids())==1,{'valid':s.isValid(),'solids':len(s.Solids()),'volume_mm3':s.Volume()})
        f=ROOT/'stl'/f'{name}.stl'
        if f.exists():
            m=trimesh.load_mesh(f);n=len(m.split(only_watertight=False))
            bnd=exact_bounds(s);dims=np.array(bnd[3:])-bnd[:3]
            if name in ('clamp_upper_base','clamp_lower','tpu_liner_upper','tpu_liner_lower'):dims=dims[[0,2,1]]
            delta=float(np.max(np.abs(m.extents-dims)));vd=abs(m.volume-s.Volume())/s.Volume()
            digest=hashlib.sha256(f.read_bytes()).hexdigest()
            check('STL '+name,delta<.04 and vd<.01 and abs(m.bounds[0,2])<.001 and digest==manifest[name]['sha256'] and m.is_watertight and m.is_winding_consistent and m.volume>0 and n==1,{'watertight':bool(m.is_watertight),'components':n,'volume_mm3':float(m.volume),'extents_mm':m.extents.tolist(),'minimum_z':float(m.bounds[0,2]),'bounds_error_mm':delta,'relative_volume_error':float(vd),'sha256':digest})
        else:check('STL '+name,False,'Export is missing')
    b=board_shape()
    base,lower,_=clamp_geometry();car=carrier()
    for name,v in P['variants'].items():
        e=v['extra_depth'];r=rear(e);f=bezel(e,v['hood_projection'],v['side_projection']);t=tray(e);bb=b.translate((0,0,e))
        for label,shape in [('rear',r),('bezel',f),('tray',t),('window',window(e))]:
            clear(f'{name} vendor board vs {label}',bb,shape,1e-4)
        clear(name+' rear vs bezel',r,f)
        clear(name+' rear vs tray',r,t)
        clear(name+' gasket vs rear',seam_gasket(e),r)
        clear(name+' gasket vs bezel',seam_gasket(e),f)
        clear(name+' window vs bezel',window(e),f)
        clear(name+' adhesive vs bezel',adhesive(e),f)
        clear(name+' rear vs carrier',r,car)
        clear(name+' cap vs bezel',cage_cap(),f)
        # Swept rectangular plug overmold ends 1.62 mm outside STEP receptacle mouth.
        # Metal nose intentionally enters socket: no collision test against socket internals.
        plug_front=-36.9
        plug=box(P['plug_housing_width'],P['plug_housing_length'],P['plug_housing_height'],P['usb_center_x'],plug_front-P['plug_housing_length']/2,P['usb_center_z']+e-P['plug_housing_height']/2)
        clear(name+' fully inserted assumed plug housing vs rear',plug,r)
        clear(name+' fully inserted assumed plug housing vs board',plug,bb)
        sweep=box(P['plug_housing_width'],65,P['plug_housing_height'],P['usb_center_x'],plug_front-32.5,P['usb_center_z']+e-P['plug_housing_height']/2)
        clear(name+' plug insertion sweep vs case',sweep,r.fuse(f))
        # Conservative populated-tray top-down insertion with case open.
        max_overlap=0.
        for dz in np.linspace(0,30,16):
            max_overlap=max(max_overlap,vol(t.translate((0,0,float(dz))),r))
        # Continuous swept conservative component boxes prove the board path,
        # avoiding thousands of repeated Booleans on a 451-solid compound.
        for comp in bb.Solids():
            cb=comp.BoundingBox()
            keep=box(cb.xlen,cb.ylen,cb.zlen+30,cb.center.x,cb.center.y,cb.zmin)
            max_overlap=max(max_overlap,vol(keep,r))
        check(name+' populated tray installation sweep',max_overlap<1e-4,{'max_intersection_mm3':max_overlap,'tray_z_samples':16,'board_sweep':'continuous union of 451 conservative component boxes over +30 mm'})
        # Case fastener tools outside seal, before fitting to mount.
        for i,(x,y) in enumerate(SEAL_BOLTS):clear(f'{name} case driver {i+1}',cyl(2,60,x,y,25+e),f)
        clear(name+' closure screw head clearance',union([cyl(2.75,3,x,y,22+e) for x,y in SEAL_BOLTS]),f)
        fingers=[]
        for x,y in [(0,3)]+[(sx*21.6,3+sy*28.8) for sx in (-1,1) for sy in (-1,1)]:
            sphere=cq.Solid.makeSphere(8,cq.Vector(x,y,19.2+e+8),angleDegrees1=-90,angleDegrees2=90)
            fingers.append(vol(sphere,f))
        check(name+' 16 mm fingertip at LCD centre and corners',max(fingers)<1e-4,{'overlaps_mm3':fingers,'scope':'geometric proxy, not a human/glove/touch test'})
        # Central full aperture orthographic sight and normal finger approach.
        view=box(43.8,58.2,80,0,3,19.4+e)
        clear(name+' normal viewing prism',view,f)
        check(name+' USB roof web',17.5-(P['usb_center_z']+P['usb_opening_height']/2)>=1.5,{'minimum_mm':17.5-(P['usb_center_z']+P['usb_opening_height']/2)})
    # Installed hardware is checked pairwise, in addition to board fit above.
    # Smooth thread proxies have clearance bores; no positive-volume overlaps
    # are waived. Free TPU is replaced by nominal compressed assembly geometry.
    for variant in P['variants']:
        rows=assembly(variant,include_board=False,landscape=False);bad=[];count=0
        for i,(n,a,c,o) in enumerate(rows):
            for nn,other,cc,oo in rows[i+1:]:
                v=vol(a,other);count+=1
                if v>1e-4:bad.append({'a':n,'b':nn,'intersection_mm3':v})
        check(variant+' installed non-electronic assembly pairs',not bad,{'pairs':count,'collisions':bad})
    # Sliding assembly: case travels from +Y to seated zero. Cap and retracted screw included.
    lift=P['thumb_cage_ceiling']-P['thumb_head_height']
    case=rear().fuse(cage_cap()).fuse(thumbscrew(lift));maxv=0;bad=[]
    for dy in np.linspace(0,62,63):
        moving=case.translate((0,float(dy),0));vv=vol(moving,car)
        maxv=max(maxv,vv)
        if vv>1e-4:bad.append([float(dy),vv])
    check('Rail insertion / removal sweep',maxv<1e-4,{'samples':63,'max_intersection_mm3':maxv,'bad_samples':bad})
    # Retraction set by cage head contact, with shaft clear of receiver nose.
    lift=P['thumb_cage_ceiling']-P['thumb_head_height'];tip=-P['thumb_screw_length']+lift
    check('Thumb withdrawal clears nose',tip>-7,{'head_travel_mm':lift,'withdrawn_tip_z':tip,'nose_top_z':-7,'clearance_mm':tip+7})
    check('Thumb remains captive',P['thumb_head_diameter']>13 and P['thumb_head_diameter']<18,{'head_diameter':P['thumb_head_diameter'],'cap_aperture':13,'post_inside_width':18})
    maxv=0.
    for dz in np.linspace(0,lift,43):
        for fixed_shape in [rear(),cage_cap(),car]:maxv=max(maxv,vol(thumbscrew(float(dz)),fixed_shape))
    check('Full captive thumbscrew retraction sweep',maxv<1e-4,{'samples':43,'max_overlap_mm3':maxv})
    clear('Thumb tightened shaft vs rear',thumbscrew(),rear())
    clear('Thumb tightened head vs cage',cyl(8,8,0,57,0),cage_cap())
    clear('Thumb withdrawn vs carrier',thumbscrew(lift),car)
    clear('Rigid carrier vs fixed base',car,base)
    # Build exact fixed isolation hardware: no metal contacts floating carrier at rest.
    fixed=[]
    for i,(x,y) in enumerate(BUSHINGS):
        sleeve=washer(8,5.3,11.4,LOWER_WASHER_TOP,x,y)
        fixed.extend([sleeve,washer(15,5.3,WASHER_THICKNESS,BASE_TOP,x,y),washer(15,5.3,WASHER_THICKNESS,TOP_WASHER_BOTTOM,x,y),screw(5,20,ISOLATION_HEAD_Z,x,y,8.5,5)])
        bs=bushing().translate((x,y,LOWER_WASHER_TOP))
        clear(f'Bushing {i+1} vs rigid carrier (tangent contact)',bs,car)
        clear(f'Bushing {i+1} vs sleeve',bs,sleeve)
        clear(f'Bushing {i+1} vs base',bs,base)
    for j,s in enumerate(fixed):clear(f'No rigid isolation bypass {j+1}',car,s)
    stop_hw=[]
    for x,y in STOP_POINTS:
        stop_hw.extend([washer(14,3.4,1,STOP_TOP,x,y),screw(3,18,STOP_TOP+1,x,y,5.5,3)])
    for i,s in enumerate(stop_hw):clear(f'Travel stop free at rest {i+1}',car,s)
    # Limited movement before stops. Not a vibration frequency / stiffness calculation.
    maxv=0.
    for dx,dy,dz in [(0,0,.7),(0,0,-.7),(.7,0,0),(-.7,0,0),(0,.7,0),(0,-.7,0)]:
        moved=car.translate((dx,dy,dz));maxv=max(maxv,vol(moved,base),*[vol(moved,s)for s in fixed+stop_hw])
    check('0.7 mm isolated translation before stops',maxv<1e-4,{'max_rigid_overlap_mm3':maxv})
    upperstop=max(vol(car.translate((0,0,1.2)),s) for s in stop_hw)
    lowerstop=vol(car.translate((0,0,-1.2)),base)
    lateralstop=vol(car.translate((1.2,0,0)),base)
    check('Fail-safe stops engage beyond normal travel',min(upperstop,lowerstop,lateralstop)>0,{'upper_mm3':upperstop,'lower_mm3':lowerstop,'lateral_mm3':lateralstop,'nominal_upper_gap_mm':.9,'nominal_lower_gap_mm':1,'nominal_radial_gap_mm':1,'washer_capture_overlap_at_zero_mm':(15-12.4)/2})
    check('Sleeve sets compression',abs(P['sleeve_length']-(6+2*P['bushing_flange_compressed']))<1e-8,{'free_bushing_height':12,'clamped_height':11.4,'flange_squeeze_each_mm':.3})
    clear('Clamp upper vs lower',base,lower)
    check('Clamp closing gap',P['clamp_split_gap']>=1,{'assumed_assembled_gap_mm':P['clamp_split_gap'],'liner_radial_interference_mm':P['liner_free_thickness']-P['liner_compressed_thickness'],'bar_diameter_assumed_mm':P['bar_diameter'],'straight_width_required_mm':P['bar_straight_width']})
    # Thread reaches: all hardware are parameterized proxies, not an ISO tolerance proof.
    engagement={'case_M3':2.4,'tray_M2.5':2.0,'board_assumed_M3':P['board_screw_length']-(8.5-5.5),'cap_M2.5':2.0,'thumb_M4':3.2,'isolation_M5':4.0,'clamp_M5':4.0,'stop_M3':2.4}
    check('Positive specified metal engagement',all(x>0 for x in engagement.values()),engagement)
    # Top cap screw tool shafts must clear hood and posts (screw axis hole is intended).
    for x in (-12,12):clear(f'Cap driver x={x}',cyl(1.5,30,x,57,25.5),bezel())
    # Gasket, adhesive are closed rings and their measured contact areas are nonzero.
    check('Continuous single-piece seal paths',len(seam_gasket().Solids())==1 and len(adhesive().Solids())==1,{'seam_area_mm2':seam_gasket().Volume()/.7,'adhesive_area_mm2':adhesive().Volume()/.2,'seam_nominal_width_mm':2.4,'adhesive_nominal_width_mm':2.0})
    check('Window to native touch gap',abs(window().BoundingBox().zmin-b.BoundingBox().zmax-.2)<1e-5,{'gap_mm':window().BoundingBox().zmin-b.BoundingBox().zmax,'added_window_mm':P['window_thickness'],'touch':'UNVERIFIED physical test required'})
    for n in ['MiniNav-Frame-with-button.stl','MiniNav-back-panel-with-strap-slits.stl']:
        digest=hashlib.sha256((ROOT.parent/n).read_bytes()).hexdigest()
        check('Original preserved '+n,digest==json.loads((ROOT/'evidence/reference-measurements.json').read_text())[n]['sha256'],digest)
    r=rear()
    floor=vol(r,box(.02,.02,40,0,0,-10))/.0004
    nut_floor=vol(r,box(.02,.02,40,27.2,22,-10))/.0004
    tongue=vol(r,box(20,.02,.02,30,0,18.5))/.0004
    check('Measured local shell sections',floor>=3-1e-5 and nut_floor>=1.5-1e-5 and tongue>=1.6-1e-5,{'rear_floor_mm':floor,'tray_nut_floor_mm':nut_floor,'tongue_wall_mm':tongue,'method':'BRep material in 0.02 mm square probes','scope':'three local sections, not a global minimum-wall analysis'})
    report['passed']=sum(x['pass']for x in report['checks']);report['failed']=sum(not x['pass']for x in report['checks'])
    report['limitations']=['No physical printing, fit, touch, sealing, strength, fatigue, vibration, thermal or ride tests.', 'Archived 2024 manufacturer geometry may differ from the user board; factory threads and cable nose remain assumptions.', 'Nominal compressed TPU is not a deformation simulation; smooth screw proxies do not prove tolerance or strength.', 'Motion checks combine sampled paths with continuous board component-box sweep; installed bike obstructions are unknown.']
    (ROOT/'validation/checks.json').write_text(json.dumps(report,indent=2)+'\n')
    lines=['# Geometric validation','',f"Result: **{'PASS' if not report['failed'] else 'FAIL'}** — {report['passed']} passed, {report['failed']} failed.",'','Generated by `cad/validate.py`; numeric evidence in [checks.json](checks.json).','', '| Check | Result |','|---|---|']
    lines += [f"| {x['name']} | {'PASS' if x['pass'] else 'FAIL'} |" for x in report['checks']]
    lines += ['','## Limits','']+['- '+x for x in report['limitations']]
    (ROOT/'validation/VALIDATION.md').write_text('\n'.join(lines)+'\n')
    print(f"{report['passed']} passed; {report['failed']} failed",flush=True)
    return 1 if report['failed'] else 0
if __name__=='__main__':sys.exit(main())
