"""Purchased sheet cut patterns in native CAD coordinates, at 1:1 mm scale."""
from model import ROOT, P
out=ROOT/'templates';out.mkdir(exist_ok=True)
svg=['<svg xmlns="http://www.w3.org/2000/svg" width="210mm" height="297mm" viewBox="0 0 210 297">',
'<rect width="210" height="297" fill="white"/>',
'<g fill="none" stroke="black" stroke-width="0.2">']
def rect(x,y,w,h,r):svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}"/>')
rect(10,23,69,85,6.5);rect(12.4,25.4,64.2,80.2,4.1)
rect(100,23,53,69,.8)
rect(100,119,53,69,.8);rect(102,121,49,65,.5)
svg+=['<path d="M10 225h50m-50 -2v4m50 -4v4"/>','</g>','<g font-family="sans-serif" font-size="3.3" fill="black">']
for x,y,t in [(10,10,'Independent MiniNav / purchased materials / mm'),(10,18,'Seam silicone: 1.0 thick'),(100,18,'Clear sheet: 0.5 thick'),(100,114,'Adhesive ring: 0.2 thick'),(10,215,'50 mm calibration: print at 100%, no fit-to-page'),(10,239,'Clear sheet / adhesive centre: native X=0, Y=3'),(10,247,'Seam centre: native X=0, Y=0'),(10,255,'Native Y is long screen axis; installed view rotates -90 degrees.'),(10,263,'Cut uninterrupted rings; physical fit and sealing unverified.')]:
 svg.append(f'<text x="{x}" y="{y}">{t}</text>')
svg+=['</g>','</svg>'];(out/'purchased-materials-1to1.svg').write_text('\n'.join(svg)+'\n')
print('Wrote purchased-material patterns')
