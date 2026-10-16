"""1:1 SVG cut patterns for purchased materials, and a coordinate sheet."""
import json
from pathlib import Path
import design as D
R=D.ROOT

def rect(x,y,w,h,r=1,fill='none',stroke='#000'):
 return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" stroke-width="0.15"/>'
def txt(x,y,t,size=3):return f'<text x="{x}" y="{y}" font-family="sans-serif" font-size="{size}">{t}</text>'
svg='<svg xmlns="http://www.w3.org/2000/svg" width="210mm" height="297mm" viewBox="0 0 210 297">'
svg+=txt(10,12,'BikeNav / purchased material cut templates',5)+txt(10,19,'Print at 100% / actual size. Verify the 50 mm scale before cutting.')
svg+=txt(10,29,'PET transparent film: 68 x 52.4 x 0.20 mm, R1')+rect(10,34,68,52.4)
svg+=txt(100,29,'Window adhesive: continuous ring')+rect(100,34,68,52.4)+rect(103.3,36.7,61.4,47)
svg+=txt(100,90,'0.15 mm adhesive; 3.3 mm / 2.7 mm lands')
svg+=txt(10,105,'Seam gasket: 82.4 x 60.4 / 79.2 x 57.2, R5.2 / R3.6')
svg+=rect(10,110,82.4,60.4,5.2)+rect(11.6,111.6,79.2,57.2,3.6)
svg+=txt(10,177,'Silicone sheet 1.2 mm (or printed TPU trial). No joint in ring.')
svg+=txt(10,190,'Board pads (purchased closed-cell silicone, shim after trial fit):')
svg+=txt(10,197,'4 rear pads: diameter 6 mm; 0.8 mm free, target 0.5 installed.')
for x in (12,22,32,42):svg+=f'<circle cx="{x}" cy="207" r="3" fill="none" stroke="black" stroke-width="0.15"/>'
svg+=txt(10,219,'4 front spacer strips: 4 x 0.7 mm; target 0.15 mm installed.')
for x in (10,20,30,40):svg+=rect(x,224,4,.7,0)
svg+=txt(10,239,'Front pads sit on film bond land at x=-29 / +24, y=+-24.6.')
svg+=txt(10,246,'Check touch and glass pressure before sealing. Do not print an opaque window.')
svg+='<path d="M10 264 H60 M10 261 V267 M60 261 V267" fill="none" stroke="black" stroke-width="0.3"/>'+txt(23,273,'50 mm')+'</svg>'
(R/'templates/purchased_materials_1to1.svg').write_text(svg)
# Coordinates in the case frame, y positive up; screen glass centred at origin.
s='<svg xmlns="http://www.w3.org/2000/svg" width="900" height="760" viewBox="-60 -52 120 104">'
s+='<rect x="-60" y="-52" width="120" height="104" fill="#f5f7fa"/>'
s+=txt(-55,-46,'Case coordinates / front view / mm',3.5)
s+=rect(-45,-37,90,74,4.5,fill='#d9e0e7',stroke='#34465b')
s+=rect(-36.53,-25.27,73.06,50.54,2,fill='#26394a')
s+=rect(-2.7-28.8,-21.6,57.6,43.2,.2,fill='#4c99bf')
for x,y in D.BOARD_POSTS:s+=f'<circle cx="{x}" cy="{-y}" r="1.6" fill="#ffd166"/>'
s+='<path d="M0 0 H53 M0 0 V-41" stroke="#be2a35" stroke-width="0.4"/>'+txt(52,4,'+X',3)+txt(2,-40,'+Y',3)
s+=txt(-28,2,'LCD AA 57.6 x 43.2',2.7)+txt(-52,43,'USB +X, y=-0.06, z=-7.6695; screen front z=-0.5',2.6)
s+=txt(-52,48,'Yellow: factory support centres. Buttons are enclosed at +Y.',2.6)
s+='</svg>'
(R/'reference/coordinate-map.svg').write_text(s)
print('Wrote purchased material template and coordinate map')
