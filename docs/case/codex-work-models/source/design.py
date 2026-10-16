"""BikeNav enclosure, mm. CadQuery 2.6.1; see README before printing.

Case: +X USB side, +Y top, +Z toward viewer; glass front z=-0.5.
Isolation: same XY, wedge top z=0. Case translates +34 Z here.
Bike: bar axis X, centre z=0. Wedge bottom at clamp_top_z.
STLs are reoriented onto Z=0. STEP parts retain their design coordinates.
"""
from pathlib import Path
import argparse, json, math, hashlib
from OCP.BRepBndLib import BRepBndLib
from OCP.Bnd import Bnd_Box
import cadquery as cq

ROOT=Path(__file__).resolve().parents[1]
P=json.loads((ROOT/'source/parameters.json').read_text())
EPS=0.02
BUSH=[(x,y) for x in (-28,28) for y in (-18,18)]
SCREWS=[(x,y) for x in (-36.5,0,36.5) for y in (-33.5,33.5)]
BOARD_POSTS=[(x,y) for x in (-30.1,29.9) for y in (-20.5,20.5)]
PARTS={}

def exact_bounds(shape):
 # OCCT's default triangulation-backed box overestimates the flared loft after
 # STL export (compact Z max 8.988 instead of 6). Measure the analytic BRep.
 b=Bnd_Box()
 BRepBndLib.AddOptimal_s(shape.wrapped,b,False,False)
 return list(b.Get())

def box(x,y,z,at=(0,0,0)):
 return cq.Workplane('XY').box(x,y,z,centered=(True,True,False)).translate(at).val()
def rr(x,y,r,z,h,at=(0,0)):
 w=cq.Workplane('XY').rect(x,y).extrude(h)
 if r: w=w.edges('|Z').fillet(r)
 return w.val().translate((at[0],at[1],z))
def cyl(r,h,at=(0,0,0),axis=(0,0,1)):
 return cq.Solid.makeCylinder(r,h,cq.Vector(*at),cq.Vector(*axis))
def hexhole(af,h,at=(0,0,0)):
 return cq.Workplane('XY').polygon(6,af/math.cos(math.pi/6)).extrude(h).val().translate(at)
def ring(outer,inner,r,z,h,at=(0,0)):
 return rr(*outer,r,z,h,at).cut(rr(*inner,max(.1,r-(outer[0]-inner[0])/2),z-EPS,h+2*EPS,at))
def union(*ss):
 s=ss[0]
 for q in ss[1:]:s=s.fuse(q)
 return s.clean()
def nut(af,height,bore,at):
 return hexhole(af,height,at).cut(cyl(bore/2,height+.2,(at[0],at[1],at[2]-.1)))
def washer(od,id,h,at):
 return cyl(od/2,h,at).cut(cyl(id/2,h+.2,(at[0],at[1],at[2]-.1)))
def on_y(s):
 return s.rotate((0,0,0),(1,0,0),-90)
def cyl_y(r,y0,y1,x=0,z=0):
 return cyl(r,y1-y0,(x,y0,z),(0,1,0))
def profile_xz(points,y0,length):
 # XZ workplane normal is -Y.
 return cq.Workplane('XZ').polyline(points).close().extrude(length).val().translate((0,y0+length,0))
def add(name,s,material='rigid',qty=1,rotation=None,note=''):
 s=s.clean()
 assert s.isValid(),name+' invalid BRep'
 assert len(s.Solids())==1,(name,'solids',len(s.Solids()))
 PARTS[name]={'shape':s,'material':material,'quantity':qty,'rotation':rotation,'note':note}
 return s

def case_front(style):
 W,H=P['case_width'],P['case_height']; cw=P['glass_width']+2*P['electronics_xy_clearance']; ch=P['glass_height']+2*P['electronics_xy_clearance']
 a=rr(W,H,4.5,P['seam_z'],2.5-P['seam_z'])
 a=a.cut(rr(cw,ch,2.0,-1.52,1.52))
 a=a.cut(rr(P['window_aperture_width'],P['window_aperture_height'],1,-2,5,(P['window_center_x'],0)))
 # A broad recessed aperture lets a fingertip reach the active-area edges.
 # The film remains bonded to an uninterrupted flat underside land.
 lower=cq.Workplane(obj=rr(P['window_aperture_width'],P['window_aperture_height'],1,-.1,.1,(P['window_center_x'],0))).faces('>Z').wires().val()
 upper=cq.Workplane(obj=rr(P['window_aperture_width']+16,P['window_aperture_height']+16,3,2.5,.01,(P['window_center_x'],0))).faces('>Z').wires().val()
 a=a.cut(cq.Solid.makeLoft([lower,upper],True))
 a=a.cut(rr(P['window_film_width']+.2,P['window_film_height']+.2,1,-.4,.4,(P['window_center_x'],0)))
 a=a.fuse(ring((77.1,54.5),(cw,ch),3.3,-3.5,2.02))
 for x,y in SCREWS:
  a=a.cut(hexhole(5.8,2.72,(x,y,-1.52)))
  a=a.cut(cyl(1.7,2.82,(x,y,-1.52)))
 proj=P['hood_projection'][style]; side=P['hood_side_projection'][style]
 if proj:
  a=a.fuse(box(W,2.4,proj+2.5,(0,H/2-1.2,0)))
  # Sloped side wing: y=-3 at bezel; rises gently toward the top.
  wing=cq.Workplane('YZ').polyline([(-3,0),(H/2,0),(H/2,2.5+side),(12,2.5+side),(-3,2.5)]).close().extrude(2.4).val()
  a=a.fuse(wing.translate((W/2-2.4,0,0))).fuse(wing.translate((-W/2,0,0)))
 # Recut after adding hood roots, which overlap the upper nut positions.
 for x,y in SCREWS:
  a=a.cut(hexhole(5.8,2.72,(x,y,-1.52)))
  a=a.cut(cyl(1.7,2.82,(x,y,-1.52)))
 return a.clean()

def male_rail(clearance=0):
 # Wide foot below narrow neck. Insert from +Y (slide toward -Y); withdraw +Y.
 s=profile_xz([(-15,8.5),(15,8.5),(11,16.5),(-11,16.5)],-16,32)
 return s.fuse(box(22,32,2.5,(0,0,16.5))).clean()

def case_rear():
 cw=P['glass_width']+2*P['electronics_xy_clearance'];ch=P['glass_height']+2*P['electronics_xy_clearance']
 s=rr(P['case_width'],P['case_height'],4.5,-15,13.5)
 s=s.cut(rr(cw,ch,2.0,-12.5,11.1))
 s=s.cut(rr(77.7,55.1,3.6,-3.8,2.4))
 s=s.cut(ring((82.6,60.6),(79,57),5.3,-2.4,1))
 # Opening accepts housing, including enough recess for its nose to reach USB.
 port=rr(P['usb_opening_width'],P['usb_opening_height'],1,-5,20)
 # temporary local XY profile becomes YZ, cylinder/extrusion along +X
 port=port.rotate((0,0,0),(0,1,0),90).rotate((0,0,0),(1,0,0),90)
 # after transforms: local x -> global y; local y -> global z
 port=port.translate((37,P['usb_center_y'],P['usb_center_z']))
 s=s.cut(port)
 for x,y in SCREWS:s=s.cut(cyl(1.7,14,(x,y,-15.1)))
 for x,y in BOARD_POSTS:s=s.fuse(cyl(3,1.5,(x,y,-12.5)))
 eye=cyl(5,5,(-48,-20,-14)).cut(cyl(2.3,6,(-48,-20,-14.5)))
 s=s.fuse(eye)
 rail=male_rail().translate((0,0,-34))
 # M4 heat-set insert in EXTERNAL rail; blind end remains outside cavity.
 rail=rail.cut(cyl_y(P['insert_diameter']/2,-16.05,-16+P['insert_length']+1.0,z=-21.5))
 s=s.fuse(rail)
 return s.clean()

def carrier():
 s=rr(78,56,4,2.5,3)
 for x,y in BUSH:s=s.cut(cyl(4.5,3.2,(x,y,2.4)))
 # Closed near end (y=-16), open far end (+Y). This keeps the removal path clear of the knob cage.
 track=box(39,40,11.35,(0,.35,5.5))
 c=P['rail_clearance']
 channel=profile_xz([(-15-c,8.5-c),(15+c,8.5-c),(11+c,16.5+c),(-11-c,16.5+c)],-16,38)
 channel=channel.fuse(box(22+2*c,38,8,(0,3,16.5)))
 track=track.cut(channel)
 s=s.fuse(track).cut(cyl_y(2.3,-21,-15.9,z=12.5))
 # Captive knob cage stands forward of the case, with no rigid base contact.
 s=s.fuse(box(34,17,3,(0,-32.5,2.5)))
 s=s.fuse(box(34,3,23,(0,-39.5,2.5)))
 s=s.cut(cyl_y(2.3,-41.1,-37.8,z=12.5))
 # Two side walls capture a 22 mm knob flange, open at top/bottom for grip.
 for x in (-14.5,14.5):
  s=s.fuse(box(5,11,23,(x,-46.5,2.5)))
  for z in (6,19):
   s=s.cut(cyl_y(1.25,-52.1,-42,x,z))
 return s.clean()

def cage_cap():
 s=box(34,3,23,(0,-53.5,2.5))
 s=s.cut(cyl_y(9.3,-55.2,-51.8,z=12.5))
 for x in (-14.5,14.5):
  for z in (6,19):s=s.cut(cyl_y(1.6,-55.2,-51.8,x,z))
 return s

def thumbwheel():
 # locked: body -52..-41, flange -43..-41, head seat y=-45.
 s=cyl_y(9,-52,-41,z=12.5).fuse(cyl_y(11,-43,-41,z=12.5))
 for i in range(12):
  a=i*math.pi/6
  s=s.cut(cyl_y(1.1,-52.1,-43,x=9*math.cos(a),z=12.5+9*math.sin(a)))
 s=s.cut(cyl_y(2.2,-52.1,-40.9,z=12.5))
 h=on_y(hexhole(7.2,7.1)).translate((0,-52.1,12.5))
 s=s.cut(h)
 # Positive bolthead cover, secured with two M2 screws in blind pilots.
 for x in (-6.3,6.3):s=s.cut(cyl_y(.8,-52.1,-45.8,x,12.5))
 return s.clean()

def thumb_cap():
 s=cyl_y(9,-54,-52,z=12.5)
 for x in (-6.3,6.3):s=s.cut(cyl_y(1.1,-54.1,-51.9,x,12.5))
 return s

def bushing(free=True):
 f=P['bushing_free_flange'] if free else P['bushing_installed_flange']
 return union(cyl(7,f),cyl(4.4,1.5,(0,0,f))).cut(cyl(P['bushing_bore']/2,f+1.6,(0,0,-.05)))

def clamp():
 R=P['bar_diameter']/2+P['liner_installed_thickness'];O=R+P['clamp_wall'];L=P['clamp_width'];g=P['clamp_split_gap']
 whole=cyl(O,L,(-L/2,0,0),(1,0,0)).cut(cyl(R,L+1,(-L/2-.5,0,0),(1,0,0)))
 up=whole.intersect(box(L+1,2*O+1,O+1,(0,0,g/2)))
 down=whole.intersect(box(L+1,2*O+1,O+1,(0,0,-O-1-g/2)))
 bolt_y=R+6
 for x in (-10,10):
  for y in (-bolt_y,bolt_y):
   up=up.fuse(cyl(5,6,(x,y,g/2))).fuse(box(10,7,6,(x,y-math.copysign(3,y),g/2)))
   down=down.fuse(cyl(5,6,(x,y,-g/2-6))).fuse(box(10,7,6,(x,y-math.copysign(3,y),-g/2-6)))
 # Re-cut bore after adding ears; avoids any material in liner space.
 bore=cyl(R,L+1,(-L/2-.5,0,0),(1,0,0))
 up=up.fuse(box(L,18,O+2-7,(0,0,7))).cut(bore)
 down=down.cut(bore)
 top=O+2
 for x in (-10,10):
  for y in (-bolt_y,bolt_y):
   up=up.cut(cyl(2.2,30,(x,y,0)))
   down=down.cut(cyl(2.2,30,(x,y,-30)))
   up=up.cut(cyl(4.7,40,(x,y,g/2+6)))
   down=down.cut(cyl(4.7,40,(x,y,-40-g/2-6)))
  up=up.cut(cyl(2.2,7,(x,0,top-6.5)))
  # Side-loading M4 nut trap, roof 2.1 mm, independent of bar bore.
  up=up.cut(hexhole(7.2,3.4,(x,0,top-5.5)))
  up=up.cut(box(L/2-9,7.2,3.4,(math.copysign(10+(L/2-9)/2,x),0,top-5.5)))
 return up.clean(),down.clean(),top

def liner(free=True):
 R=P['bar_diameter']/2; t=P['liner_free_thickness'] if free else P['liner_installed_thickness'];L=P['clamp_width'];g=P['clamp_split_gap']
 a=cyl(R+t,L,(-L/2,0,0),(1,0,0))
 for x in (-L/2-1.5,L/2):a=a.fuse(cyl(R+t+1,1.5,(x,0,0),(1,0,0)))
 a=a.cut(cyl(R,L+4,(-L/2-2,0,0),(1,0,0)))
 return a.intersect(box(L+4,2*(R+t+2),R+t+2,(0,0,g/2))).clean()

def wedge(angle):
 # Top is a rigid transform of isolation XY; lowest top height >=8 mm.
 rad=math.radians(angle); zc=8+28*math.sin(rad)
 # In bike-local coordinates, bottom=0, top z=zc+y*tan(angle).
 t=math.tan(rad)
 s=cq.Workplane('YZ').polyline([(-28,0),(28,0),(28,zc+28*t),(-28,zc-28*t)]).close().extrude(78).val().translate((-39,0,0))
 # Open underside pockets reduce mass; retain 4 mm perimeter, 5 mm sloping
 # roof, centre clamp-bolt block and four 13 mm isolation fastener columns.
 void=cq.Workplane('YZ').polyline([(-24,-.1),(24,-.1),(24,zc+24*t-5/math.cos(rad)),(-24,zc-24*t-5/math.cos(rad))]).close().extrude(70).val().translate((-35,0,0))
 void=void.cut(box(36,18,60,(0,0,-.2)))
 for x,y in BUSH:
  column=cyl(6.5,70,(x,y,-60)).rotate((0,0,0),(1,0,0),angle).translate((0,0,zc))
  void=void.cut(column)
 s=s.cut(void)
 # Clamp bolts vertical with common head seat 4 mm above wedge underside.
 for x in (-10,10):
  s=s.cut(cyl(2.2,50,(x,0,-.1)))
  s=s.cut(cyl(4,50,(x,0,4)))
 def tf(sh):return sh.rotate((0,0,0),(1,0,0),angle).translate((0,0,zc))
 for x,y in BUSH:
  s=s.cut(tf(cyl(1.7,70,(x,y,-60))))
  # Nyloc nut/washer access recess: leaves 4 mm of rigid material below top.
  s=s.cut(tf(cyl(5,56,(x,y,-60))))
 # Downward travel stops below carrier; no static contact.
 for y in (-16,16):s=s.fuse(tf(cyl(3,1,(0,y,0))))
 return s.clean(),zc

def rail_coupon(clearance):
 outer=box(39,12,11.35,(0,0,5.5))
 c=clearance
 cut=profile_xz([(-15-c,8.5-c),(15+c,8.5-c),(11+c,16.5+c),(-11-c,16.5+c)],-7,14)
 cut=cut.fuse(box(22+2*c,14,4,(0,0,16.5)))
 return outer.cut(cut)

def build():
 PARTS.clear()
 for style in P['hood_projection']:add('front_'+style,case_front(style),note='Print rear rim down; support under bezel ledge and blind nut roofs.')
 add('rear_shell',case_rear(),note='Rail foot down; support exterior floor around rail. Keep cavity facing up.')
 add('seam_gasket',ring((82.4,60.4),(79.2,57.2),5.2,0,1.2),'TPU',note='1.2 mm free height; installed height 0.9 mm. Silicone sheet preferred for sealing.')
 add('floating_carrier',carrier(),note='Plate down; supports beneath cage and rail overhangs. Clear channel carefully.')
 add('cage_front',cage_cap(),rotation=((1,0,0),90),note='Rear face down.')
 add('thumbwheel',thumbwheel(),rotation=((1,0,0),90),note='Flange down; small support beneath flange transition as needed.')
 add('thumbwheel_cap',thumb_cap(),rotation=((1,0,0),90))
 add('bushing_half',bushing(),'TPU',8,note='Four pairs. Flange down. Free stack 8.4, sleeve limits installed stack to 8.0 mm.')
 u,d,top=clamp()
 add('clamp_upper',u,rotation=((0,1,0),90),note='Bar axis vertical for circular bore; use supports for nut slots.')
 add('clamp_lower',d,rotation=((0,1,0),90),note='Bar axis vertical; flat axial face on bed.')
 add('liner_half',liner(),'TPU',2,rotation=((0,1,0),90),note='Bar axis vertical. Two identical halves, one rotated 180 degrees in assembly.')
 for a in P['adapter_angles']:add(f'angle_adapter_{a:02d}',wedge(a)[0],note='Flat underside down; supports in four nut access recesses.')
 add('coupon_rail_male',male_rail().intersect(box(40,12,30,(0,0,0))),note='Tests full cross-section, shortened length.')
 for c in (.2,.35,.5):add('coupon_rail_female_'+str(c).replace('.','p'),rail_coupon(c))
 cup=box(24,3,16,(0,0,0))
 port=rr(P['usb_opening_width'],P['usb_opening_height'],1,0,5)
 port=port.rotate((0,0,0),(1,0,0),90).translate((0,2.5,8))
 add('coupon_usb_plug',cup.cut(port),rotation=((1,0,0),90))
 fit=box(38,18,5)
 for x,dia in [(-12,3.4),(0,P['insert_diameter']),(12,9.0)]:fit=fit.cut(cyl(dia/2,6,(x,0,-.5)))
 add('coupon_holes_insert',fit,note='Left to right: 3.4 bolt, 5.6 insert, 9.0 grommet hole. Verify insert supplier.')
 return PARTS

def purchased_geometry():
 # Never exported as printable STL.
 film=rr(P['window_film_width'],P['window_film_height'],1,-.35,.2,(P['window_center_x'],0))
 adhesive=ring((P['window_film_width'],P['window_film_height']),(P['window_aperture_width'],P['window_aperture_height']),1,-.15,.15,(P['window_center_x'],0))
 return {'transparent_PET_film':film,'window_adhesive':adhesive}

def assembly(style='sun',angle=20,exploded=False,hardware=True):
 # Return named actual shapes in bike coordinates with display colours.
 _,_,top=clamp(); _,zc=wedge(angle)
 def tilt(s):return s.rotate((0,0,0),(1,0,0),angle).translate((0,0,top+zc))
 rows=[]
 def put(n,s,col,shift=0,space='iso'):
  if space=='case':s=s.translate((0,0,34+shift));s=tilt(s)
  elif space=='iso':s=tilt(s.translate((0,0,shift)))
  else:s=s.translate((0,0,shift))
  rows.append((n,s,col))
 put('clamp_upper',PARTS['clamp_upper']['shape'],(.22,.28,.33),space='bike')
 put('clamp_lower',PARTS['clamp_lower']['shape'],(.22,.28,.33),-16 if exploded else 0,'bike')
 put('liner_upper',liner(False),(.18,.50,.44),space='bike')
 put('liner_lower',liner(False).rotate((0,0,0),(1,0,0),180),(.18,.50,.44),-16 if exploded else 0,'bike')
 put('angle_adapter',PARTS[f'angle_adapter_{angle:02d}']['shape'].translate((0,0,top)),(.34,.40,.45),space='bike')
 if hardware:
  by=P['bar_diameter']/2+P['liner_installed_thickness']+6
  for x in (-10,10):
   put(f'M4_adapter_bolt_{x}',union(cyl(2,10,(x,0,top-6)),cyl(3.5,4,(x,0,top+4))),(.65,.68,.7),space='bike')
   put(f'M4_adapter_nut_{x}',nut(7,3.2,4.1,(x,0,top-5.3)),(.65,.68,.7),space='bike')
   for y in (-by,by):
    put(f'M4_clamp_bolt_{x}_{y}',union(cyl(2,25,(x,y,-17.7)),cyl(3.5,4,(x,y,7.3))),(.65,.68,.7),space='bike')
    put(f'M4_clamp_top_washer_{x}_{y}',washer(9,4.3,.8,(x,y,6.5)),(.65,.68,.7),space='bike')
    put(f'M4_clamp_bottom_washer_{x}_{y}',washer(9,4.3,.8,(x,y,-7.3)),(.65,.68,.7),space='bike')
    put(f'M4_clamp_nut_{x}_{y}',nut(7,5,4.1,(x,y,-12.3)),(.65,.68,.7),space='bike')
 put('floating_carrier',PARTS['floating_carrier']['shape'],(.22,.30,.38),10 if exploded else 0)
 for x,y in BUSH:
  lo=bushing(False).translate((x,y,0))
  hi=bushing(False).rotate((0,0,0),(1,0,0),180).translate((x,y,8))
  put(f'bushing_lower_{x}_{y}',lo,(.15,.65,.53),5 if exploded else 0)
  put(f'bushing_upper_{x}_{y}',hi,(.15,.65,.53),14 if exploded else 0)
  if hardware:
   sleeve=cyl(2,8,(x,y,0)).cut(cyl(1.6,8.2,(x,y,-.1)))
   top_washer=cyl(7,.8,(x,y,8)).cut(cyl(1.6,1,(x,y,7.9)))
   put(f'sleeve_{x}_{y}',sleeve,(.7,.72,.75))
   put(f'retaining_washer_{x}_{y}',top_washer,(.7,.72,.75),16 if exploded else 0)
   put(f'M3_isolator_bolt_{x}_{y}',union(cyl(1.5,20,(x,y,-11.2)),cyl(2.75,3,(x,y,8.8))),(.6,.62,.65))
   put(f'M3_isolator_bottom_washer_{x}_{y}',washer(7,3.2,.5,(x,y,-4.5)),(.7,.72,.75))
   put(f'M3_isolator_nut_{x}_{y}',nut(5.5,4,3.1,(x,y,-8.5)),(.7,.72,.75))
 put('rear_shell',PARTS['rear_shell']['shape'],(.18,.24,.30),30 if exploded else 0,'case')
 if hardware:
  for x,y in SCREWS:
   bolt=union(cyl(1.5,16,(x,y,-15)),cyl(2.75,1.7,(x,y,-16.7)))
   put(f'M3_case_bolt_{x}_{y}',bolt,(.65,.68,.7),30 if exploded else 0,'case')
   put(f'M3_case_nut_{x}_{y}',nut(5.5,2.4,3.1,(x,y,-1.2)),(.65,.68,.7),62 if exploded else 0,'case')
  insert=cyl_y(P['insert_diameter']/2,-16,-10,z=-21.5).cut(cyl_y(2.05,-16.1,-9.9,z=-21.5))
  put('M4_rail_insert',insert,(.7,.53,.2),30 if exploded else 0,'case')
 gasket=ring((82.4,60.4),(79.2,57.2),5.2,-2.4,.9)
 put('seam_gasket_compressed',gasket,(.1,.65,.5),41 if exploded else 0,'case')
 put('front_'+style,PARTS['front_'+style]['shape'],(.94,.47,.16),62 if exploded else 0,'case')
 for n,s in purchased_geometry().items():put(n,s,(.38,.75,.89),56 if exploded else 0,'case')
 # Display-only conservative electronics envelopes, based on drawing/STEP.
 glass=rr(73.06,50.54,2,-1.2,.7)
 pcb=box(69,49.9,1.6,(-.1,0,-6.5))
 active=box(57.6,43.2,.03,(-2.7,0,-.49))
 put('glass_reference',glass,(.08,.11,.14),48 if exploded else 0,'case')
 put('pcb_reference',pcb,(.1,.42,.28),45 if exploded else 0,'case')
 put('active_screen_reference',active,(.12,.48,.68),48 if exploded else 0,'case')
 put('cage_front',PARTS['cage_front']['shape'].translate((0,-12 if exploded else 0,0)),(.38,.44,.5),10 if exploded else 0)
 put('thumbwheel',PARTS['thumbwheel']['shape'],(.94,.47,.16),10 if exploded else 0)
 put('thumbwheel_cap',PARTS['thumbwheel_cap']['shape'],(.94,.47,.16),10 if exploded else 0)
 if hardware:
  screw=union(cyl_y(2,-45,-10,z=12.5),on_y(hexhole(7,2.8)).translate((0,-47.8,12.5)))
  put('M4x35_release_bolt',screw,(.7,.72,.75),10 if exploded else 0)
 return rows

def export():
 build();manifest={}
 for name,d in PARTS.items():
  s=d['shape'];p=s
  if d['rotation']:
   axis,angle=d['rotation'];p=p.rotate((0,0,0),axis,angle)
  b=exact_bounds(p);translation=(-(b[0]+b[3])/2,-(b[1]+b[4])/2,-b[2]);p=p.translate(translation)
  cq.exporters.export(p,str(ROOT/'stl'/f'{name}.stl'),tolerance=.035,angularTolerance=.12)
  cq.exporters.export(s,str(ROOT/'step'/f'{name}.step'))
  manifest[name]={k:v for k,v in d.items() if k!='shape'}
  manifest[name]['print_translation']=translation
  manifest[name]['print_bounds_mm']=exact_bounds(p)
  manifest[name]['stl_sha256']=hashlib.sha256((ROOT/'stl'/f'{name}.stl').read_bytes()).hexdigest()
  manifest[name]['design_volume_mm3']=s.Volume()
 for n,s in purchased_geometry().items():cq.exporters.export(s,str(ROOT/'step'/f'PURCHASED_{n}.step'))
 for style in P['hood_projection']:
  assy=cq.Assembly(name='BikeNav_'+style)
  for n,s,col in assembly(style,P['default_angle_degrees']):assy.add(s,name=n,color=cq.Color(*col,.4 if n=='transparent_PET_film' else 1))
  assy.save(str(ROOT/'step'/f'ASSEMBLY_{style}.step'))
 (ROOT/'reports/parts-manifest.json').write_text(json.dumps(manifest,indent=2))
 (ROOT/'reports/export-inputs.json').write_text(json.dumps({n:hashlib.sha256((ROOT/'source'/n).read_bytes()).hexdigest() for n in ['design.py','parameters.json']},indent=2)+'\n')
 print('Exported',len(PARTS),'printable parts and 3 STEP assemblies',flush=True)

if __name__=='__main__':export()
