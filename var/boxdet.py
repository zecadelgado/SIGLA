import fitz, numpy as np, sys
def detect(src, S=8.0, pt_sizes=(4,4.5,5,5.5,6,6.5,7), thr=150):
    doc=fitz.open(src); page=doc[0]; W,Hh=page.rect.width,page.rect.height
    pix=page.get_pixmap(matrix=fitz.Matrix(S,S),colorspace=fitz.csGRAY)
    img=np.frombuffer(pix.samples,np.uint8).reshape(pix.height,pix.width)
    dark=(img<thr).astype(np.int32); H,Wd=dark.shape
    I=np.zeros((H+1,Wd+1),np.int32); I[1:,1:]=dark.cumsum(0).cumsum(1)
    def rect(r,c,h,w): return I[r+h,c+w]-I[r,c+w]-I[r+h,c]+I[r,c]
    t=max(2,int(round(0.6*S)))  # ring thickness px
    cand=[]
    for ptk in pt_sizes:
        k=int(round(ptk*S))
        if k<6 or k-2*t<3: continue
        rr=np.arange(0,H-k); cc=np.arange(0,Wd-k); R,C=np.meshgrid(rr,cc,indexing='ij')
        outer=rect(R,C,k,k); inner=rect(R+t,C+t,k-2*t,k-2*t)
        peri=outer-inner; peri_cells=k*k-(k-2*t)*(k-2*t); in_cells=(k-2*t)*(k-2*t)
        good=(peri>=0.55*peri_cells)&(inner<=0.18*in_cells)
        ys,xs=np.where(good)
        sc=(peri/peri_cells)-(inner/np.maximum(in_cells,1))
        for y,x in zip(ys,xs): cand.append(((x+k/2)/S,(y+k/2)/S,sc[y,x]))
    cand.sort(key=lambda z:-z[2]); merged=[]
    for cx,cy,s in cand:
        if all(not(abs(m[0]-cx)<3.5 and abs(m[1]-cy)<3.5) for m in merged): merged.append((round(cx,1),round(cy,1)))
    doc.close(); return merged,W,Hh
src=sys.argv[1]; m,W,H=detect(src)
print("page %.1f x %.1f boxes=%d"%(W,H,len(m)))
rows=[]
for cx,cy in m:
    p=False
    for r in rows:
        if abs(r['y']-cy)<3.5: r['xs'].append(cx); p=True; break
    if not p: rows.append({'y':cy,'xs':[cx]})
rows.sort(key=lambda r:r['y'])
for r in rows: print("y=%5.1f n=%d : %s"%(r['y'],len(r['xs']),sorted(round(x,1) for x in r['xs'])))
