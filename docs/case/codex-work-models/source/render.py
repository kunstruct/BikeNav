"""Raster previews from tessellated CAD using VTK; run under xvfb-run."""
import math
from pathlib import Path
import numpy as np
import vtk
from vtk.util.numpy_support import numpy_to_vtk,numpy_to_vtkIdTypeArray
import design as D
D.build()
ROOT=D.ROOT

def actor(shape,col,alpha=1):
 vs,fs=shape.tessellate(.08,.16)
 vertices=np.array([v.toTuple() for v in vs]);faces=np.array(fs,dtype=np.int64)
 points=vtk.vtkPoints();points.SetData(numpy_to_vtk(vertices,deep=True))
 cells=vtk.vtkCellArray();cells.SetCells(len(faces),numpy_to_vtkIdTypeArray(np.column_stack((np.full(len(faces),3),faces)).ravel(),deep=True))
 poly=vtk.vtkPolyData();poly.SetPoints(points);poly.SetPolys(cells)
 normals=vtk.vtkPolyDataNormals();normals.SetInputData(poly);normals.ComputePointNormalsOn();normals.SetFeatureAngle(35)
 mapper=vtk.vtkPolyDataMapper();mapper.SetInputConnection(normals.GetOutputPort())
 a=vtk.vtkActor();a.SetMapper(mapper);a.GetProperty().SetColor(*col);a.GetProperty().SetOpacity(alpha);a.GetProperty().SetSpecular(.22);a.GetProperty().SetSpecularPower(35)
 return a

def render(rows,path,title,subtitle,view=(1,-1.4,1.5),size=(1600,1200),transparent=True):
 ren=vtk.vtkRenderer();ren.SetBackground(.93,.95,.97);ren.SetBackground2(.77,.82,.86);ren.GradientBackgroundOn()
 for n,s,c in rows:
  alpha=.26 if n=='transparent_PET_film' and transparent else 1
  ren.AddActor(actor(s,c,alpha))
 win=vtk.vtkRenderWindow();win.SetOffScreenRendering(1);win.SetSize(*size);win.AddRenderer(ren);win.SetMultiSamples(4)
 bounds=ren.ComputeVisiblePropBounds();center=np.array([(bounds[0]+bounds[1])/2,(bounds[2]+bounds[3])/2,(bounds[4]+bounds[5])/2]);diam=max(bounds[1]-bounds[0],bounds[3]-bounds[2],bounds[5]-bounds[4])
 camera=ren.GetActiveCamera();camera.SetFocalPoint(*center);camera.SetPosition(*(center+np.array(view)*diam*1.8));camera.SetViewUp(0,0,1);camera.ParallelProjectionOn();ren.ResetCamera();camera.Zoom(1.08)
 for text,y,font in [(title,size[1]-65,29),(subtitle,size[1]-100,17),('BikeNav | parametric CAD preview | mm | untested prototype',25,16)]:
  t=vtk.vtkTextActor();t.SetInput(text);t.SetPosition(35,y);t.GetTextProperty().SetFontSize(font);t.GetTextProperty().SetColor(.13,.18,.23);ren.AddActor2D(t)
 win.Render();w=vtk.vtkWindowToImageFilter();w.SetInput(win);w.Update();out=vtk.vtkPNGWriter();out.SetFileName(str(path));out.SetInputConnection(w.GetOutputPort());out.Write();win.Finalize()

for style in ('compact','sun','touring'):
 rows=D.assembly(style,20)
 render(rows,ROOT/'previews'/f'assembled_{style}.png',f'BikeNav / {style}',f'20 degree adapter | hood projection {D.P["hood_projection"][style]} mm | transparent film shown blue')
render(D.assembly('sun',20),ROOT/'previews/rear_mount.png','Rail, isolation and clamp','Captive release screw projects beyond the lower case edge',view=(-1,-1,-.7))
render(D.assembly('sun',20,True),ROOT/'previews/exploded_sun.png','Assembly / exploded','Orange: front + release knob | teal: TPU | blue: purchased film',view=(1,-1.6,1.0),size=(1600,1600))
rows=D.assembly('sun',0)
cut=D.box(200,200,300,(100,0,-100))
section=[]
for n,s,c in rows:
 q=s.cut(cut)
 if q.Volume()>1e-5:section.append((n,q,c))
render(section,ROOT/'previews/section.png','Cutaway / x = 0 mm','Actual solid geometry, 0 degree adapter; section exposes the enclosure stack',view=(1,-1.8,.7))
# Clear isolation detail without enclosure.
rows=[r for r in D.assembly('sun',0) if not any(k in r[0] for k in ['shell','front_sun','gasket','film','adhesive','glass','pcb','active_screen','M3_case_','M4_rail_'])]
render(rows,ROOT/'previews/isolation_mount.png','Mount / case removed','Four sleeves + eight TPU halves | 56 x 36 mm bushing pitch',view=(1,-1.5,1.6))
print('Rendered 7 geometry previews')
