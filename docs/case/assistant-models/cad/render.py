"""Depth-buffered VTK previews made exclusively from CAD tessellations.
Xvfb is started on a free display if necessary; no AI-generated illustrations.
"""
import os,subprocess,contextlib,math
import numpy as np
from PIL import Image,ImageDraw,ImageFont
from model import *
from build import mesh
CACHE={}
@contextlib.contextmanager
def display():
    if os.environ.get('DISPLAY'):
        yield;return
    readfd,writefd=os.pipe()
    p=subprocess.Popen(['Xvfb','-displayfd',str(writefd),'-screen','0','1800x1800x24','-nolisten','tcp'],pass_fds=(writefd,),stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    os.close(writefd)
    try:
        with os.fdopen(readfd) as f:n=f.readline().strip()
        if not n:raise RuntimeError('Xvfb did not provide a display')
        os.environ['DISPLAY']=':'+n
        yield
    finally:
        p.terminate();p.wait();os.environ.pop('DISPLAY',None)

def font(size,bold=False):
    path='/usr/share/fonts/truetype/dejavu/DejaVuSans'+('-Bold' if bold else '')+'.ttf'
    return ImageFont.truetype(path,size)

def draw(items,path,title,elev=25,azim=-65,exploded=False,size=(1500,1450)):
    import vtk
    from vtk.util.numpy_support import numpy_to_vtk,numpy_to_vtkIdTypeArray
    ren=vtk.vtkRenderer();ren.SetBackground(.945,.96,.975)
    win=vtk.vtkRenderWindow();win.SetOffScreenRendering(1);win.AddRenderer(ren);win.SetSize(size[0],size[1]-200);win.SetMultiSamples(4)
    allv=[]
    for name,s,col,offset in items:
        key=hash(s)
        if key not in CACHE:CACHE[key]=mesh(s)
        m=CACHE[key];v=m.vertices.copy()
        if exploded:v+=np.array(offset)
        allv.append(v)
        pts=vtk.vtkPoints();pts.SetData(numpy_to_vtk(v,deep=True))
        faces=np.hstack((np.full((len(m.faces),1),3,dtype=np.int64),m.faces)).ravel()
        cells=vtk.vtkCellArray();cells.SetCells(len(m.faces),numpy_to_vtkIdTypeArray(faces,deep=True))
        pd=vtk.vtkPolyData();pd.SetPoints(pts);pd.SetPolys(cells)
        normals=vtk.vtkPolyDataNormals();normals.SetInputData(pd);normals.ConsistencyOn();normals.AutoOrientNormalsOn();normals.SetFeatureAngle(35);normals.Update()
        mapper=vtk.vtkPolyDataMapper();mapper.SetInputConnection(normals.GetOutputPort())
        actor=vtk.vtkActor();actor.SetMapper(mapper)
        rgb=tuple(int(col[i:i+2],16)/255 for i in (1,3,5));prop=actor.GetProperty();prop.SetColor(*rgb);prop.SetAmbient(.3);prop.SetDiffuse(.7);prop.SetSpecular(.1);prop.SetSpecularPower(20)
        # Window rendered frosted blue for unambiguous depth; it is purchased clear sheet.
        ren.AddActor(actor)
    vv=np.vstack(allv);mid=(vv.min(0)+vv.max(0))/2;dist=float(np.linalg.norm(vv.max(0)-vv.min(0)))*2
    az,el=map(math.radians,(azim,elev));direction=np.array([math.cos(el)*math.cos(az),math.cos(el)*math.sin(az),math.sin(el)])
    cam=ren.GetActiveCamera();cam.SetFocalPoint(*mid);cam.SetPosition(*(mid+dist*direction));cam.SetViewUp(0,0,1);cam.ParallelProjectionOn();ren.ResetCamera();right=np.cross(direction,[0,0,1]);right/=np.linalg.norm(right);up=np.cross(right,direction)
    aspect=size[0]/(size[1]-200)
    cam.SetParallelScale(max(np.ptp(vv@up)/2,np.ptp(vv@right)/(2*aspect))*1.10);ren.ResetCameraClippingRange()
    win.Render();capture=vtk.vtkWindowToImageFilter();capture.SetInput(win);capture.ReadFrontBufferOff();capture.Update()
    writer=vtk.vtkPNGWriter();tmp=ROOT/'previews'/('_'+path);writer.SetFileName(str(tmp));writer.SetInputConnection(capture.GetOutputPort());writer.Write();win.Finalize()
    im=Image.new('RGB',size,(241,245,249));im.paste(Image.open(tmp),(0,100));tmp.unlink();d=ImageDraw.Draw(im)
    d.text((size[0]//2,35),title,font=font(29,True),fill='#19394b',anchor='mm')
    legend=[('#315d73','Rigid case'),('#e29649','Bezel / hood'),('#61bba2','TPU / seals'),('#a6afb6','Metal hardware'),('#8cc9d4','Clear sheet (purchased)')]
    x=45;y=size[1]-80
    for col,label in legend:
        d.rectangle((x,y,x+22,y+19),fill=col);d.text((x+30,y-2),label,font=font(18),fill='#334155');x+=int(d.textlength(label,font=font(18)))+65
    d.text((size[0]//2,size[1]-30),'Geometry preview • mm • physical fit, touch response and performance unverified',font=font(18),fill='#52606d',anchor='mm')
    im.save(ROOT/'previews'/path)

def render_all():
  with display():
    a=assembly(include_board=True)
    draw(a,'assembly-front.png','SUN / independent MiniNav enclosure',38,-65)
    draw(a,'assembly-rear.png','Rear rails, floating carrier and split clamp',-26,115)
    draw(a,'exploded.png','Assembly / exploded actual geometry',16,-65,True,(1500,1900))
    items=[]
    for i,(name,v) in enumerate(P['variants'].items()):
        for nm,s,c,o in [('rear',rear(v['extra_depth']),'#315d73',(0,0,0)),('bezel',bezel(v['extra_depth'],v['hood_projection'],v['side_projection']),'#e29649',(0,0,0)),('glass',window(v['extra_depth']),'#8cc9d4',(0,0,0))]:items.append((nm,s.rotate((0,0,0),(0,0,1),-90).translate((125*(i-1),0,0)),c,o))
    draw(items,'variants.png','COMPACT 0 mm  /  SUN 18 mm  /  TOURING 22 mm',48,-78,False,(1800,950))
    native=assembly(include_board=False,landscape=False)
    cut=box(150,140,200,0,-47,-100)
    chosen=[]
    for n,s,c,o in native:
        if any(k in n for k in ['carrier','base','bushing','sleeve','isolation','Travel stop']) or n.startswith('M5 washer'):
            sec=s.intersect(cut)
            if sec.Volume()>1e-6:chosen.append((n,sec,c,o))
    draw(chosen,'isolation-section.png','Section Y = 23 / sleeve, TPU and floating carrier',4,88,False,(1600,1000))
    draw([('vendor board',board_shape(),'#296a50',(0,0,0)),('tray',tray(),'#c2cbd1',(0,0,0)),('rear',rear(),'#315d73',(0,0,0))],'electronics.png','Vendor STEP on removable tray / open rear shell',50,-60)
    # Independent isolator cutaway, magnified for inspection.
    cut=box(30,40,40,31,3,-35)
    iso=[]
    for n,s,c,o in native:
        if any(k in n for k in ['carrier','base','bushing','sleeve','isolation','Travel stop']) or n.startswith('M5 washer'):
            sec=s.intersect(cut)
            if sec.Volume()>1e-6:iso.append((n,sec,c,o))
    draw(iso,'bushing-detail.png','Captured TPU / no metal-to-carrier contact at rest',4,88,False,(1500,1000))
if __name__=='__main__':render_all()
