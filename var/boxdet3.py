import fitz, numpy as np, sys
src=sys.argv[1]; S=8.0
doc=fitz.open(src); page=doc[0]; W,Hh=page.rect.width,page.rect.height
pix=page.get_pixmap(matrix=fitz.Matrix(S,S),colorspace=fitz.csGRAY)
img=np.frombuffer(pix.samples,np.uint8).reshape(pix.height,pix.width)
dark=(img<200).astype(np.int32); H,Wd=dark.shape
I=np.zeros((H+1,Wd+1),np.int32); I[1:,1:]=dark.cumsum(0).cumsum(1)
def rect(r,c,h,w): return I[r+h,c+w]-I[r,c+w]-I[r+h,c]+I[r,c]
t=2; cand=[]
for ptk in (5,5.5,6,6.5):
    k=int(round(ptk*S))
    rr=np.arange(0,H-k); cc=np.arange(0,Wd-k); R,C=np.meshgrid(rr,cc,indexing='ij')
    outer=rect(R,C,k,k); inner=rect(R+t,C+t,k-2*t,k-2*t)
    peri=outer-inner; pc=k*k-(k-2*t)**2; ic=(k-2*t)**2
    good=(peri>=0.78*pc)&(inner<=0.06*ic)
    ys,xs=np.where(good); sc=peri/pc-inner/ic
    for y,x in zip(ys,xs): cand.append(((x+k/2)/S,(y+k/2)/S,sc[y,x]))
cand.sort(key=lambda z:-z[2]); m=[]
for cx,cy,s in cand:
    if 95<cy<302 and 8<cx<235:
        if all(not(abs(a-cx)<4 and abs(b-cy)<4) for a,b in m): m.append((round(cx,1),round(cy,1)))
rows=[]
for cx,cy in m:
    p=False
    for r in rows:
        if abs(r['y']-cy)<3.5: r['xs'].append(cx); p=True; break
    if not p: rows.append({'y':cy,'xs':[cx]})
rows.sort(key=lambda r:r['y'])
for r in rows: print("y=%5.1f n=%d : %s"%(r['y'],len(r['xs']),sorted(round(x,1) for x in r['xs'])))
doc.close()
