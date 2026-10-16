"""Independent MiniNav enclosure. CadQuery 2.6.1; all dimensions millimetres.
Native CAD: board portrait X/Y, +Z toward rider, rear outside Z=0.
Mount bar axis native Y; display top native -X. Assembly previews rotate -90 Z
to landscape (+X right, +Y top), matching the firmware landscape-only display.
Dimensions with no vendor evidence are explicit design choices/assumptions; see EVIDENCE.md.
"""
from pathlib import Path
import json, math
from functools import lru_cache
from OCP.BRepBndLib import BRepBndLib
from OCP.Bnd import Bnd_Box
import cadquery as cq
ROOT=Path(__file__).resolve().parents[1]
P=json.loads((ROOT/'cad/parameters.json').read_text())
# Review correction: move seats outside the continuous gasket at all four corners.
SEAL_BOLTS=[(x,y) for x in (-34.5,34.5) for y in (-43.5,43.5)]
BOARD_HOLES=[(x,y) for x in (-20.5,20.5) for y in (-29.9,30.1)]
TRAY_BOLTS=[(x,y) for x in (-27.2,27.2) for y in (-22,22)]
BUSHINGS=[(x,y) for x in (-31,31) for y in (-23,23)]
STOP_POINTS=[(0,-26),(0,26)]
BASE_TOP=-23.0
WASHER_THICKNESS=P['washer_thickness']
LOWER_WASHER_TOP=BASE_TOP+WASHER_THICKNESS
CARRIER_BOTTOM=LOWER_WASHER_TOP+P['bushing_flange_compressed']
CARRIER_TOP=CARRIER_BOTTOM+P['carrier_thickness']
TOP_WASHER_BOTTOM=CARRIER_TOP+P['bushing_flange_compressed']
ISOLATION_HEAD_Z=TOP_WASHER_BOTTOM+WASHER_THICKNESS
STOP_BOTTOM=CARRIER_BOTTOM-1.0
STOP_TOP=CARRIER_TOP+.9

def exact_bounds(shape):
    b=Bnd_Box();BRepBndLib.AddOptimal_s(shape.wrapped,b,False,False)
    return list(b.Get())

def box(w,h,d,x=0,y=0,z=0):
    return cq.Workplane('XY').box(w,h,d,centered=(True,True,False)).translate((x,y,z)).val()
def rr(w,h,r,d,z=0,x=0,y=0):
    return cq.Workplane('XY').rect(w,h).extrude(d).edges('|Z').fillet(r).translate((x,y,z)).val()
def cyl(r,h,x=0,y=0,z=0):
    return cq.Solid.makeCylinder(r,h,cq.Vector(x,y,z))
def ring(w,h,r,iw,ih,ir,d,z=0,x=0,y=0):
    return rr(w,h,r,d,z,x,y).cut(rr(iw,ih,ir,d+2,z-1,x,y))
def hexagon(af,h,x=0,y=0,z=0):
    return cq.Workplane('XY').polygon(6,af/math.cos(math.pi/6)).extrude(h).translate((x,y,z)).val()
def union(shapes):
    a=shapes[0]
    for b in shapes[1:]: a=a.fuse(b)
    return a.clean()
def drill(s,points,diam,z,h):
    for x,y in points:s=s.cut(cyl(diam/2,h,x,y,z))
    return s.clean()
def washer(od,id,t,z,x=0,y=0):return cyl(od/2,t,x,y,z).cut(cyl(id/2,t+2,x,y,z-1))
def nut(af,h,z,x=0,y=0,diam=3):return hexagon(af,h,x,y,z).cut(cyl(diam/2,h+2,x,y,z-1))
def screw(d,l,z,x=0,y=0,head_d=None,head_h=None):
    # Z is underside of head; screw points toward -Z. Threads simplified to cylinders.
    return cyl(d/2,l,x,y,z-l).fuse(cyl((head_d or 1.8*d)/2,head_h or d,x,y,z))
def thumbscrew(lift=0):
    # DIN 464 M4: 8 mm shoulder, 16 mm knurl, 9.5 mm total head, 3.5 mm knurl.
    return union([cyl(2,16,0,57,-16+lift),cyl(4,6,0,57,lift),cyl(P['thumb_head_diameter']/2,3.5,0,57,6+lift)])

@lru_cache(maxsize=4)
def board_shape(extra=0):
    return cq.importers.importStep(str(ROOT/'evidence/esp32-s3-touch-lcd-2_8.stp')).val().translate((0,P['board_y_shift'],P['board_face_z']-.7+extra))

def tray(extra=0):
    # Mount factory threaded pillars from below BEFORE lowering the populated tray.
    t=ring(58,72,2,34,48,2,2,5.5)
    for x,y in BOARD_HOLES:t=t.fuse(cyl(3.8,3,x,y,5.5))
    for x,y in TRAY_BOLTS:t=t.fuse(cyl(2.6,2,x,y,5.5))
    t=drill(t,BOARD_HOLES,P['board_thread_diameter']+.4,5,4)
    t=drill(t,TRAY_BOLTS,2.8,5,4)
    return t.translate((0,0,extra)).clean()

def rear(extra=0):
    a=rr(78,96,6,17.5+extra)
    a=a.cut(rr(60,76,2,30+extra,3))
    a=a.fuse(ring(63.2,79.2,3.6,60,76,2,2,17.5+extra))
    # Blind inner tray fasteners: nut loaded horizontally from the cavity.
    for x,y in TRAY_BOLTS:
        a=a.fuse(cyl(4.2,2.5+extra,x,y,3))
        a=a.cut(hexagon(5.3,2.2,x,y,1.5+extra))
        # Side-loading nut slot faces inward, entirely above the sealed floor.
        a=a.cut(box(7,5.3,2.2,x-math.copysign(3,x),y,1.5+extra))
        a=a.cut(cyl(1.4,5,x,y,3.6+extra))
    for x,y in SEAL_BOLTS:
        a=a.cut(cyl(1.7,28+extra,x,y,-1))
        a=a.cut(hexagon(5.8,2.7,x,y,-.1))
    # Only open wall penetration: plug-sized bottom tunnel, wholly below gasket.
    a=a.cut(box(P['usb_opening_width'],20,P['usb_opening_height'],P['usb_center_x'],-43,P['usb_center_z']+extra-P['usb_opening_height']/2))
    # Male T rails and dry captive-thumbscrew ear.
    for x in (-15,15):
        a=a.fuse(box(4,52,3,x,0,-3)).fuse(box(8,52,3,x,0,-6))
    a=a.fuse(rr(34,23,3,5,-5,0,54.5))
    a=a.cut(cyl(2.3,8,0,57,-6))
    for x in (-12,12):
        a=a.fuse(box(6,8,20,x,57,0))
        a=a.cut(cyl(1.4,7,x,57,14))
        a=a.cut(hexagon(5.3,2.2,x,57,15))
        a=a.cut(box(6,5.3,2.2,x+math.copysign(2,x),57,15))
    # External tether eye: hole does not reach cavity or seam.
    a=a.fuse(rr(12,14,3,5,0,42,-30))
    a=a.cut(cyl(2.5,7,43,-30,-1))
    return a.clean()

def cage_cap():
    s=rr(32,18,2,3,20,0,57).cut(cyl(6.5,5,0,57,19))
    return drill(s,[(-12,57),(12,57)],2.8,19,5)

def bezel(extra=0,hood=18,side=10):
    a=rr(78,96,6,3.8,18.2+extra)
    # Separate narrow tongue groove leaves the internal window-bonding shelf intact.
    a=a.cut(ring(63.8,79.8,3.9,59.6,75.6,1.8,1.6,18.2+extra))
    a=a.cut(rr(60,76,2,.5,18.2+extra))
    a=a.cut(rr(53.4,69.4,1,1.2,18.2+extra,0,3))
    a=a.cut(rr(48.2,62.6,.8,7,17+extra,0,3))
    # Review: original straight aperture blocked a 16 mm fingertip at all LCD
    # corners. Keep the flat bonding shelf, flare only above adhesive Z=19.4.
    lower=cq.Workplane(obj=rr(48.2,62.6,.8,.1,19.3+extra,0,3)).faces('>Z').wires().val()
    upper=cq.Workplane(obj=rr(60.2,74.6,2,.01,22+extra,0,3)).faces('>Z').wires().val()
    a=a.cut(cq.Solid.makeLoft([lower,upper],True))
    a=drill(a,SEAL_BOLTS,3.4,17+extra,7)
    # Hard seats fix gasket squeeze at 0.7 mm compressed thickness.
    for x,y in SEAL_BOLTS:
        seat=cyl(3.4,.7,x,y,17.5+extra).cut(cyl(1.7,2,x,y,17+extra))
        a=a.fuse(seat)
    if hood:
        # Hood follows the landscape screen top (native -X).
        a=a.fuse(box(2.8,96,hood,-37.6,0,22+extra))
        for y in (-46.6,46.6):
            w=cq.Workplane('XZ').polyline([(0,22+extra),(-36.2,22+extra),(-36.2,22+extra+side)]).close().extrude(2.8).val().translate((0,y+1.4,0))
            a=a.fuse(w)
        # Access reliefs preserve a 1.3 mm outer hood wall beside closure heads.
        for x,y in SEAL_BOLTS:a=a.cut(cyl(3.2,hood+1,x,y,22+extra))
    return a.clean()

def seam_gasket(extra=0,free=False):
    return ring(69,85,6.5,64.2,80.2,4.1,1 if free else .7,17.5+extra)
def window(extra=0):return rr(53,69,.8,P['window_thickness'],18.7+extra,0,3)
def adhesive(extra=0):return ring(53,69,.8,49,65,.5,P['window_adhesive_thickness'],19.2+extra,0,3)

def carrier(clearance=None):
    c=P['rail_clearance'] if clearance is None else clearance
    a=rr(80,66,5,6,CARRIER_BOTTOM)
    a=a.fuse(rr(48,66,3,-.5-CARRIER_TOP,CARRIER_TOP))
    a=a.fuse(rr(36,36,3,6,-13,0,48))  # captive nut nose, outside case cavity
    # Bracing joins the nose into main plate, still removable from below.
    a=a.fuse(box(36,7,6.2,0,31,CARRIER_BOTTOM))
    for x in (-15,15):
        a=a.cut(box(8+2*c,66,3+2*c,x,6.7,-6-c))
        a=a.cut(box(4+2*c,66,3+c,x,6.7,-3))
    a=drill(a,BUSHINGS,12.4,-21,25)
    a=drill(a,BUSHINGS,18.6,CARRIER_TOP,20)
    for x,y in STOP_POINTS:
        a=a.cut(cyl(4,7,x,y,CARRIER_BOTTOM-.5))
        a=a.cut(cyl(9,20,x,y,CARRIER_TOP))
    # M4 nut slides from right; closed roof carries screw tension.
    a=a.cut(cyl(2.25,8,0,57,-14))
    a=a.cut(hexagon(7.3,3.4,0,57,-11.9))
    a=a.cut(box(20,7.3,3.4,9,57,-11.9))
    for y in (46,63):
        a=a.cut(cq.Solid.makeCylinder(1.05,7,cq.Vector(11.5,y,-10),cq.Vector(1,0,0)))
    return a.clean()

def nut_keeper():
    a=box(3,23,6,19.5,54.5,-13)
    for y in (46,63):
        a=a.cut(cq.Solid.makeCylinder(1.4,5,cq.Vector(17,y,-10),cq.Vector(1,0,0)))
    return a.clean()

def bushing(free=False):
    flange=P['bushing_flange_free'] if free else P['bushing_flange_compressed']
    b=union([cyl(P['bushing_flange_od']/2,flange),cyl(P['bushing_barrel_od']/2,6,0,0,flange),cyl(P['bushing_flange_od']/2,flange,0,0,flange+6)])
    return b.cut(cyl(P['bushing_bore']/2,2*flange+8,0,0,-1)).clean()

def clamp_geometry():
    R=P['bar_diameter']/2+P['liner_compressed_thickness']; ro=R+5
    axis=-49.0; w=P['bar_straight_width']; gap=P['clamp_split_gap']
    outer=cq.Solid.makeCylinder(ro,w,cq.Vector(-w/2,0,axis),cq.Vector(1,0,0))
    bore=cq.Solid.makeCylinder(R,w+2,cq.Vector(-w/2-1,0,axis),cq.Vector(1,0,0))
    ann=outer.cut(bore)
    upper=ann.intersect(box(w+2,2*ro+2,40,0,0,axis+gap/2))
    lower=ann.intersect(box(w+2,2*ro+2,40,0,0,axis-gap/2-40))
    bolt_y=R+8.6; bolt_x=min(12,w/2-7)
    for y in (-bolt_y,bolt_y):
        upper=upper.fuse(box(w,14,9,0,y,axis+gap/2))
        lower=lower.fuse(box(w,14,9,0,y,axis-gap/2-9))
    bolts=[(x,y) for x in (-bolt_x,bolt_x) for y in (-bolt_y,bolt_y)]
    upper=upper.cut(bore);lower=lower.cut(bore)
    upper=drill(upper,bolts,5.5,axis-1,30);lower=drill(lower,bolts,5.5,axis-20,30)
    for x,y in bolts:upper=upper.cut(hexagon(8.3,4.4,x,y,axis+gap/2+9-4.3))
    # Crown ties directly to short base, no tall mast.
    upper=upper.fuse(box(w,20,9,0,0,-36))
    # Review: orient the clamp along the long screen axis for landscape firmware.
    upper=upper.rotate((0,0,0),(0,0,1),90)
    lower=lower.rotate((0,0,0),(0,0,1),90)
    bore=bore.rotate((0,0,0),(0,0,1),90)
    bolts=[(-y,x) for x,y in bolts]
    for x,y in bolts:
        lower=lower.cut(cyl(7.7,20,x,y,axis-gap/2-29))
    upper=upper.fuse(rr(80,66,5,5,-28))
    for x,y in BUSHINGS:
        upper=upper.cut(cyl(2.75,7,x,y,-29))
        upper=upper.cut(hexagon(8.3,4.3,x,y,-28.1))
    # Rigid travel stops with 1 mm nominal axial and radial gaps.
    for x,y in STOP_POINTS:
        upper=upper.fuse(cyl(6,STOP_BOTTOM-BASE_TOP,x,y,BASE_TOP)).fuse(cyl(3,STOP_TOP-STOP_BOTTOM,x,y,STOP_BOTTOM))
        upper=upper.cut(cyl(1.7,20,x,y,-29))
        upper=upper.cut(hexagon(5.8,2.6,x,y,-28.1))
    return upper.cut(bore).clean(),lower.clean(),bolts

def liner(top=True,installed=False):
    r=P['bar_diameter']/2; w=P['bar_straight_width'];axis=-49
    a=cq.Solid.makeCylinder(r+(P['liner_compressed_thickness'] if installed else P['liner_free_thickness']),w,cq.Vector(-w/2,0,axis),cq.Vector(1,0,0))
    a=a.cut(cq.Solid.makeCylinder(r,w+2,cq.Vector(-w/2-1,0,axis),cq.Vector(1,0,0)))
    return a.intersect(box(w+2,2*r+4,30,0,0,axis+P['clamp_split_gap']/2 if top else axis-30-P['clamp_split_gap']/2)).rotate((0,0,0),(0,0,1),90).clean()

def coupon_rail(c):
    # A complete female 18 mm segment, separate male coupon also exported.
    a=box(16,18,9,0,0,0)
    a=a.cut(box(8+2*c,20,3+2*c,0,0,3-c))
    a=a.cut(box(4+2*c,20,4,0,0,6))
    return a.clean()
def coupon_male():return union([box(8,22,3),box(4,22,3,0,0,3),box(16,22,2,0,0,6)])
def coupon_holes():
    a=box(60,20,6)
    for x,d in zip((-24,-12,0,12,24),(2.4,2.8,3.4,4.6,5.5)):a=a.cut(cyl(d/2,8,x,0,-1))
    return a

def printable_parts():
    upper,lower,_=clamp_geometry()
    parts={'clamp_upper_base':upper,'clamp_lower':lower,'carrier':carrier(),'captive_cap':cage_cap(),'nut_keeper':nut_keeper(),'board_tray':tray(),'tpu_bushing':bushing(True),'tpu_liner_upper':liner(True),'tpu_liner_lower':liner(False),'coupon_rail_male':coupon_male(),'coupon_holes':coupon_holes(),'coupon_usb_plug':box(24,3,17).cut(box(16,5,9,0,0,4)),'coupon_isolator':box(28,28,6).cut(cyl(6.2,8,0,0,-1))}
    for c in (.2,.3,.4):parts[f'coupon_rail_{c:.1f}']=coupon_rail(c)
    for name,v in P['variants'].items():
        e=v['extra_depth'];parts[f'rear_{name}']=rear(e);parts[f'bezel_{name}']=bezel(e,v['hood_projection'],v['side_projection'])
        if e:parts[f'board_tray_{name}']=tray(e)
    return parts

def assembly(variant='sun',include_board=True,landscape=True):
    v=P['variants'][variant];e=v['extra_depth'];up,lo,clamp_pts=clamp_geometry()
    # tuple name, shape, color, exploded displacement; purchased parts distinctly labelled.
    a=[]
    def add(name,s,color,offset=(0,0,0)):a.append((name,s,color,offset))
    add('PLA rear shell',rear(e),'#315d73',(0,0,28))
    add('PLA bezel / hood',bezel(e,v['hood_projection'],v['side_projection']),'#e29649',(0,0,88))
    add('PLA populated tray',tray(e),'#abb8c3',(0,0,43))
    if include_board:add('Waveshare vendor STEP',board_shape(e),'#296a50',(0,0,51))
    add('PURCHASED clear window',window(e),'#8cc9d4',(0,0,75))
    add('PURCHASED adhesive ring',adhesive(e),'#c6cbcf',(0,0,78))
    add('PURCHASED silicone seam',seam_gasket(e),'#61bba2',(0,0,66))
    add('PLA captive screw cap',cage_cap(),'#315d73',(0,0,52))
    add('PURCHASED M4 thumbscrew',thumbscrew(),'#a6afb6',(0,0,30))
    add('PURCHASED M4 locking nut',nut(7,3.2,-11.8,0,57,4),'#a6afb6',(0,0,2))
    add('PLA nut keeper',nut_keeper(),'#d7a35a',(10,0,0))
    add('PLA floating carrier',carrier(),'#d7a35a',(0,0,0))
    add('PLA clamp / fixed base',up,'#60778a',(0,0,-38))
    add('PLA lower clamp',lo,'#60778a',(0,0,-52))
    add('TPU upper liner',liner(True,True),'#58a88f',(0,0,-38))
    add('TPU lower liner',liner(False,True),'#58a88f',(0,0,-45))
    for i,(x,y) in enumerate(BUSHINGS):
        add(f'TPU bushing {i+1}',bushing().translate((x,y,LOWER_WASHER_TOP)),'#61bba2',(0,0,-16))
        add(f'METAL sleeve {i+1}',washer(8,5.3,11.4,LOWER_WASHER_TOP,x,y),'#b7bec4',(0,0,-16))
        for suffix,z in [('lower',-23),('upper',TOP_WASHER_BOTTOM)]:add(f'M5 washer {i+1} {suffix}',washer(15,5.3,WASHER_THICKNESS,z,x,y),'#a6afb6',(0,0,-16))
        add(f'M5 isolation screw {i+1}',screw(5,20,ISOLATION_HEAD_Z,x,y,8.5,5),'#a6afb6',(0,0,7))
        add(f'M5 isolation nut {i+1}',nut(8,4,-28,x,y,5),'#a6afb6',(0,0,-38))
    for i,(x,y) in enumerate(STOP_POINTS):
        add(f'Travel stop washer {i+1}',washer(14,3.4,1,STOP_TOP,x,y),'#a6afb6',(0,0,-15))
        add(f'Travel stop M3 screw {i+1}',screw(3,18,STOP_TOP+1,x,y,5.5,3),'#a6afb6',(0,0,6))
        add(f'Travel stop nut {i+1}',nut(5.5,2.4,-27.9,x,y,3),'#a6afb6',(0,0,-38))
    for i,(x,y) in enumerate(SEAL_BOLTS):
        add(f'Case M3 screw {i+1}',screw(3,22+e,22+e,x,y,5.5,3),'#a6afb6',(0,0,93))
        add(f'Case M3 nut {i+1}',nut(5.5,2.4,.1,x,y,3),'#a6afb6',(0,0,28))
    for i,(x,y) in enumerate(clamp_pts):
        # Bottom-entering screws rotated around their underside-head plane.
        u=-49-P['clamp_split_gap']/2-9-WASHER_THICKNESS
        s=screw(5,25,0,0,0,8.5,5).rotate((0,0,0),(1,0,0),180).translate((x,y,u))
        add(f'Clamp M5 screw {i+1}',s,'#a6afb6',(0,0,-58))
        add(f'Clamp M5 washer {i+1}',washer(15,5.3,WASHER_THICKNESS,u,x,y),'#a6afb6',(0,0,-52))
        add(f'Clamp M5 nut {i+1}',nut(8,4,-43.2,x,y,5).rotate((x,y,0),(x,y,1),90),'#a6afb6',(0,0,-38))
    for i,(x,y) in enumerate(TRAY_BOLTS):
        add(f'Tray M2.5 screw {i+1}',screw(2.5,6,7.5+e,x,y,4.5,2.5),'#a6afb6',(0,0,44))
        add(f'Tray M2.5 nut {i+1}',nut(5,2,1.6+e,x,y,2.6),'#a6afb6',(0,0,28))
    for i,(x,y) in enumerate(BOARD_HOLES):
        s=screw(P['board_thread_diameter'],P['board_screw_length'],0,0,0,5.7,1.65).rotate((0,0,0),(1,0,0),180).translate((x,y,5.5+e))
        add(f'Board screw {i+1}',s,'#a6afb6',(0,0,43))
    for x in (-12,12):
        add(f'Cap screw {x}',screw(2.5,8,23,x,57,4.5,2.5),'#a6afb6',(0,0,57))
        add(f'Cap nut {x}',nut(5,2,15.1,x,57,2.6),'#a6afb6',(0,0,52))
    if landscape:
        return [(n,s.rotate((0,0,0),(0,0,1),-90),c,(o[1],-o[0],o[2])) for n,s,c,o in a]
    return a
