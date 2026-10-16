"""Assemble actual CAD renders into a review contact sheet (no generated artwork)."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont,ImageOps
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'previews';out.mkdir(exist_ok=True)
font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',25)
rows=[('Assembled','assembled_sun.png','assembly-front.png'),('Exploded','exploded_sun.png','exploded.png'),('Rear / retention','rear_mount.png','assembly-rear.png'),('Isolation','isolation_mount.png','bushing-detail.png')]
im=Image.new('RGB',(1800,2720),'white');d=ImageDraw.Draw(im)
for j,label in enumerate(['codex-work-models / original, reviewed','assistant-models / independent, reviewed']):d.text((j*900+25,15),label,font=font,fill='#19394b')
for i,(label,left,right) in enumerate(rows):
 for j,(folder,name) in enumerate([('codex-work-models',left),('assistant-models',right)]):
  pic=Image.open(ROOT/folder/'previews'/name).convert('RGB');pic=ImageOps.contain(pic,(890,620))
  im.paste(pic,(j*900+(900-pic.width)//2,75+i*660+(620-pic.height)//2))
  d.text((j*900+25,50+i*660),label,font=font,fill='#19394b')
im.save(out/'comparison.png')
# Additional sheet for all variant and section views, so inspection is recorded.
more=[('codex-work-models',x) for x in ['assembled_compact.png','assembled_touring.png','section.png']]+[('assistant-models',x) for x in ['variants.png','isolation-section.png','electronics.png']]
sheet=Image.new('RGB',(1800,1800),'white');d=ImageDraw.Draw(sheet)
for i,(folder,name) in enumerate(more):
 x=(i//3)*900;y=(i%3)*600
 pic=ImageOps.contain(Image.open(ROOT/folder/'previews'/name).convert('RGB'),(880,550))
 sheet.paste(pic,(x+(900-pic.width)//2,y+40+(550-pic.height)//2))
 d.text((x+15,y+10),folder+' / '+name,font=font,fill='#19394b')
sheet.save(out/'variants-and-sections.png')
print('Wrote two contact sheets')
