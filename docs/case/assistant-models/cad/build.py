"""Export printable meshes, neutral CAD and reproducible geometry previews."""
import argparse,json,hashlib
from pathlib import Path
import cadquery as cq
import numpy as np
import trimesh
from model import *

def mesh(shape):
    vertices,triangles=shape.tessellate(.08,.18)
    return trimesh.Trimesh(vertices=np.array([v.toTuple() for v in vertices]),faces=np.array(triangles),process=True)

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--skip-preview',action='store_true');args=ap.parse_args()
    (ROOT/'cad/neutral').mkdir(exist_ok=True)
    manifest={}
    for name,s in printable_parts().items():
        if not s.isValid() or len(s.Solids())!=1:raise RuntimeError(f'{name}: invalid or disconnected CAD ({len(s.Solids())} solids)')
        # Clamp and liner end-on prints keep the circular profile in the XY plane.
        rotate=name in ('clamp_upper_base','clamp_lower','tpu_liner_upper','tpu_liner_lower')
        print_s=s.rotate((0,0,0),(1,0,0),90) if rotate else s
        b=exact_bounds(print_s);shift=(-b[0],-b[1],-b[2]);print_s=print_s.translate(shift)
        path=ROOT/'stl'/f'{name}.stl';cq.exporters.export(print_s,str(path),tolerance=.04,angularTolerance=.12)
        cq.exporters.export(s,str(ROOT/'cad/neutral'/f'{name}.step'))
        m=trimesh.load_mesh(path)
        manifest[name]={'file':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'material':'TPU' if name.startswith('tpu_') else 'PLA prototype; PETG/ASA after validation','print_rotation_x_degrees':90 if rotate else 0,'print_translation_mm':list(shift),'assembly_bounds_mm':[exact_bounds(s)[:3],exact_bounds(s)[3:]],'print_bounds_mm':[exact_bounds(print_s)[:3],exact_bounds(print_s)[3:]],'stl_extents_mm':m.extents.tolist(),'volume_mm3':float(m.volume),'faces':len(m.faces),'watertight':bool(m.is_watertight),'winding_consistent':bool(m.is_winding_consistent),'components':len(m.split(only_watertight=False))}
        print(name,manifest[name]['watertight'],manifest[name]['components'],flush=True)
    (ROOT/'validation/mesh-report.json').write_text(json.dumps(manifest,indent=2)+'\n')
    (ROOT/'validation/export-inputs.json').write_text(json.dumps({n:hashlib.sha256((ROOT/'cad'/n).read_bytes()).hexdigest() for n in ['model.py','parameters.json','build.py']},indent=2)+'\n')
    for variant in P['variants']:
        a=cq.Assembly(name='Independent_'+variant)
        # Keep vendor detail in its original reference file; assembled STEP uses
        # labelled glass/PCB proxies, avoiding 170 MB of duplicate BRep per view.
        rows=assembly(variant,include_board=False)
        e=P['variants'][variant]['extra_depth']
        for n,s in [('REFERENCE_glass',rr(50.54,73.06,2,.7,17.8+e)),('REFERENCE_PCB',box(49.9,69,1.6,0,-.1,12.5+e))]:
            rows.append((n,s.rotate((0,0,0),(0,0,1),-90),'#296a50',(0,0,0)))
        for i,(n,s,c,o) in enumerate(rows):
            a.add(s,name=f'{i:03d}_'+n.replace(' ','_').replace('/','_'),color=cq.Color(*[int(c[j:j+2],16)/255 for j in (1,3,5)]))
        a.save(str(ROOT/'cad/neutral'/f'ASSEMBLY_{variant}.step'))
    # Purchased cut-sheet templates, deliberately outside the printable STL directory.
    for name,s in [('window',window()),('window_adhesive',adhesive()),('seam_gasket',seam_gasket(free=True))]:
        cq.exporters.export(s,str(ROOT/'cad/neutral'/f'PURCHASED_{name}.step'))
    if not args.skip_preview:
        from render import render_all
        render_all()
if __name__=='__main__':main()
