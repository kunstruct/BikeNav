#!/usr/bin/env python3
"""Render the MiniNav concept film with Pillow, NumPy and GStreamer (system Python)."""
from pathlib import Path
import math, wave
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import gi
gi.require_version('Gst', '1.0')
from gi.repository import Gst

ROOT = Path(__file__).resolve().parent
W, H, FPS, DURATION = 1280, 720, 24, 49
BG = '#111f24'; WHITE = '#f5f0df'; MUTED = '#9bb0ad'; MINT = '#bde9b2'; ORANGE = '#f4a56d'
def font(size, bold=False):
    return ImageFont.truetype('/usr/share/fonts/truetype/lato/Lato-'+('Bold' if bold else 'Regular')+'.ttf', size)
F = {s: font(s, s >= 40) for s in [16,18,20,24,28,32,42,54,62,76]}
SCENES = [
 ('MEET YOUR EVERYDAY CO-PILOT', ['Small screen.', 'Little soul.'], 'A familiar face. A reason to head out.'),
 ('01 / WAKE', ['Hello,', 'little explorer.'], 'Meet Pip. Your day starts together.'),
 ('02 / RIDE', ['Find your way.', 'Stay in the moment.'], 'Clear directions take the front seat.'),
 ('03 / ARRIVE', ['Every journey', 'leaves a memory.'], 'A tiny celebration, once you have parked.'),
 ('04 / FOCUS', ['Park the bike.', 'Find your flow.'], 'Your riding buddy becomes your desk buddy.'),
 ('05 / CONNECT', ['Plug in.', 'Open possibilities.'], 'Local connection. Cloud possibilities.'),
 ('MININAV / COMPANION CONCEPT', ['Go places.', 'Grow together.'], 'Your route. Your rhythm. Your little companion.'),
]

def text(d, xy, value, size, color=WHITE): d.text(xy, value, font=F[size], fill=color)
def rr(d, box, radius, color, outline=None, width=1): d.rounded_rectangle(box, radius, fill=color, outline=outline, width=width)
def pet(d, cx, cy, scale, t, happy=False):
    # Original 12-column pixel sprite; the ears suggest a small trail creature.
    rows=['001100001100','001110011100','001111111100','011111111110','111111111111','111111111111','111111111111','011111111110','001111111100','001100001100']
    y = cy - 5*scale + int(math.sin(t*2.5)*4)
    for j,row in enumerate(rows):
        for i,v in enumerate(row):
            if v=='1': d.rectangle((cx+(i-6)*scale,y+j*scale,cx+(i-5)*scale-1,y+(j+1)*scale-1), fill=MINT)
    blink = t%4.6 > 4.38
    for x in [-2.5,2.5]:
        xx=cx+x*scale
        rr(d,(xx-scale/2,y+4*scale,xx+scale/2,y+(4.25 if blink or happy else 5.4)*scale),2,BG)
    d.line((cx-scale,y+7*scale,cx,y+7.5*scale,cx+scale,y+7*scale),fill=BG,width=max(2,scale//3))

def screen(d, box, scene, t):
    x,y,x2,y2=box
    rr(d,box,13,'#223638')
    text(d,(x+18,y+12),'MININAV',16,MINT)
    text(d,(x2-58,y+12),'09:41',16,MUTED)
    if scene==2:
        for i in range(5):
            yy=y+80+i*44+(t*15)%44
            d.line((x+10,yy,x2-10,yy-28),fill='#3c5150',width=6)
        d.line((x+110,y2-20,x+110,y+135,x+205,y+135,x+205,y+85),fill=MINT,width=13)
        rr(d,(x+12,y+43,x2-12,y+102),9,MINT)
        d.line((x+29,y+86,x+29,y+61,x+54,y+61),fill=BG,width=4)
        d.polygon([(x+54,y+54),(x+64,y+61),(x+54,y+68)],fill=BG)
        text(d,(x+78,y+52),'200 m',28,BG)
        d.polygon([(x+110,y2-55),(x+98,y2-30),(x+122,y2-30)],fill=WHITE)
    elif scene==4:
        text(d,(x+24,y+53),'FOCUS TIME',18,MUTED)
        text(d,(x+23,y+82),'24:59',54)
        d.arc((x+205,y+65,x+285,y+145),-90,230,fill=MINT,width=5)
        pet(d,x+245,y+198,5,t)
        text(d,(x+24,y+189),'One thing',20)
        text(d,(x+24,y+217),'at a time.',20,MUTED)
    elif scene==5:
        text(d,(x+22,y+49),'USB / COMPANION LINK',18,MINT)
        rr(d,(x+20,y+84,x2-20,y+128),8,'#324647')
        text(d,(x+32,y+96),'PC   •   Focus / notifications',18)
        rr(d,(x+20,y+139,x2-20,y+183),8,'#324647')
        text(d,(x+32,y+151),'PHONE   •   Status / stats',18)
        rr(d,(x+20,y+194,x2-20,y+242),8,'#55443b')
        text(d,(x+32,y+207),'●  Notes   /   Tap to record',18,ORANGE)
    else:
        pet(d,(x+x2)//2,y+122,10,t,scene==3)
        label={0:'Ready when you are.',1:'Hey, let’s go outside.',3:'New memory: lakeside.',6:'See you out there.'}[scene]
        text(d,(x+22,y+214),label,20)
        if scene==3:
            for i in range(8):
                xx=x+22+i*40; yy=y+55+int(math.sin(t*2+i)*16)
                d.rectangle((xx,yy,xx+4,yy+4),fill=ORANGE)

def frame(t):
    scene=min(6,int(t//7)); local=t%7
    im=Image.new('RGB',(W,H),BG); d=ImageDraw.Draw(im)
    for r in range(290,20,-20):
        shade=tuple(int(v) for v in (19+(290-r)*.025,38+(290-r)*.025,41+(290-r)*.025))
        d.ellipse((966-r,345-r,966+r,345+r),fill=shade)
    text(d,(64,37),'mini',28); text(d,(117,37),'nav',28,MINT)
    text(d,(1005,43),'A LITTLE MORE ALIVE',16,MUTED)
    label,lines,sub=SCENES[scene]
    offset=int(20*(1-min(1,local/.7))**3)
    text(d,(65,181+offset),label,18,ORANGE)
    for i,line in enumerate(lines): text(d,(60,225+i*76+offset),line,62)
    # Hand-wrapped copy keeps each frame comfortably readable.
    words=sub.split(); current=''; yy=414
    for word in words:
        nxt=(current+' '+word).strip()
        if d.textlength(nxt,font=F[24])>540:
            text(d,(65,yy),current,24,MUTED); yy+=34; current=word
        else: current=nxt
    text(d,(65,yy),current,24,MUTED)
    if scene==6:
        rr(d,(65,520,333,569),24,MINT); text(d,(89,532),'LET’S MAKE IT REAL.',20,BG)
    # Industrial-design sketch: landscape 4:3 display, protective lip, USB power.
    bob=int(math.sin(t*1.1)*5)
    x,y=755,188+bob
    d.ellipse((749,566,1194,602),fill='#0b161a')
    if scene==2:
        d.line((690,538,1240,538),fill='#3d5052',width=24)
        rr(d,(917,463,1014,552),15,'#506062')
    elif scene in (4,5):
        d.polygon([(890,470),(1020,470),(1080,574),(839,574)],fill='#405251')
    d.line((1155,y+295,1215,y+295,1215,641),fill='#647471',width=7)
    rr(d,(x+8,y+11,x+434,y+331),32,'#091519')
    rr(d,(x,y,x+426,y+320),30,'#586965')
    rr(d,(x+7,y+7,x+419,y+308),25,'#364948')
    rr(d,(x+14,y+10,x+412,y+40),12,'#72827a')
    screen(d,(x+45,y+39,x+381,y+291),scene,t)
    d.ellipse((x+400,y+147,x+411,y+158),fill=ORANGE)
    text(d,(802,618),'2.8-INCH WORLD. EVERYDAY COMPANION.',16,MUTED)
    text(d,(65,657),'PROJECT VISION  /  CONCEPT UI • PROPOSED FEATURES',16,MUTED)
    for i in range(7):
        rr(d,(1004+i*28,663,1022+i*28,667),2,MINT if i<=scene else '#3a4b4d')
    # Fade through the background at scene boundaries.
    alpha=min(1,local/.38,(7-local)/.38)
    if t>DURATION-.8: alpha=min(alpha,(DURATION-t)/.8)
    return Image.blend(Image.new('RGB',(W,H),BG),im,max(0,alpha))

def soundtrack():
    sr=48000; a=np.zeros(sr*DURATION,dtype=np.float32)
    # Original soft pentatonic score, composed specifically for this film.
    for beat in range(98):
        start=beat*.5
        note=[60,67,69,64,67,72,69,67][beat%8]
        f=440*2**((note-69)/12); tt=np.arange(int(sr*1.5))/sr
        sig=(np.sin(2*np.pi*f*tt)+.2*np.sin(4*np.pi*f*tt))*np.exp(-tt*3)*np.minimum(1,tt/.018)*.085
        p=int(start*sr); n=min(len(sig),len(a)-p); a[p:p+n]+=sig[:n]
    for bar in range(7):
        for note in ([48,55,60],[45,52,57],[41,48,53],[43,50,55])[bar%4]:
            tt=np.arange(sr*7)/sr; f=440*2**((note-69)/12)
            env=np.minimum(1,tt/1.2)*np.minimum(1,(7-tt)/1.4)
            a[bar*sr*7:(bar+1)*sr*7]+=.025*np.sin(2*np.pi*f*tt)*env
    a*=np.minimum(1,np.arange(len(a))/(sr*1.5))*np.minimum(1,(len(a)-np.arange(len(a)))/(sr*2))
    with wave.open(str(ROOT/'soundtrack.wav'),'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr); w.writeframes((a*32767).astype('<i2').tobytes())

def render():
    soundtrack(); Gst.init(None)
    pipeline=Gst.parse_launch(f'appsrc name=frames format=time is-live=false block=true caps="video/x-raw,format=RGB,width={W},height={H},framerate={FPS}/1" ! queue ! videoconvert ! vp8enc deadline=10000 cpu-used=4 target-bitrate=2500000 threads=4 ! queue ! mux. filesrc location="{ROOT}/soundtrack.wav" ! wavparse ! audioconvert ! opusenc ! queue ! mux. webmmux name=mux ! filesink location="{ROOT}/mininav-intro.webm"')
    src=pipeline.get_by_name('frames'); pipeline.set_state(Gst.State.PLAYING)
    for i in range(FPS*DURATION):
        im=frame(i/FPS)
        if i in [FPS*3,FPS*17,FPS*31,FPS*45]: im.save(ROOT/f'preview-{i//FPS:02d}.png')
        raw=im.tobytes(); buf=Gst.Buffer.new_allocate(None,len(raw),None); buf.fill(0,raw)
        buf.pts=i*Gst.SECOND//FPS; buf.duration=Gst.SECOND//FPS
        if src.emit('push-buffer',buf)!=Gst.FlowReturn.OK: raise RuntimeError('Video encoder rejected frame')
        if i%(FPS*7)==0: print(f'Rendered scene {i//(FPS*7)+1}/7',flush=True)
    src.emit('end-of-stream')
    msg=pipeline.get_bus().timed_pop_filtered(Gst.CLOCK_TIME_NONE,Gst.MessageType.ERROR|Gst.MessageType.EOS)
    if msg.type==Gst.MessageType.ERROR: raise RuntimeError(msg.parse_error())
    pipeline.set_state(Gst.State.NULL)
    print(ROOT/'mininav-intro.webm',flush=True)

if __name__=='__main__': render()
