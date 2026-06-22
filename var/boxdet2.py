import fitz, numpy as np, sys
src=sys.argv[1]; S=8.0
doc=fitz.open(src); page=doc[0]; W,Hh=page.rect.width,page.rect.height
pix=page.get_pixmap(matrix=fitz.Matrix(S,S),colorspace=fitz.csGRAY)
img=np.frombuffer(pix.samples,np.uint8).reshape(pix.height,pix.width)
dark=(img<150).astype(np.int32); H,Wd=dark.shape
I=np.zeros((H+1,Wd+1),np.int32); I[1:,1:]=dark.cumsum(0).cumsum(1)
def rect(r,c,h,w): return I[r+h,c+w]-I[r,c+w]-I[r+h,c]+I[r,c]
def find(y0,y1,x0,x1,sizes=(5,5.5,6,6.5)):
    t=3; cand=[]
    for ptk in sizes:
        k=int(round(ptk*S))
        if k-2*t<3: continue
        for y in range(int(y0*S),int(y1*S)-k):
            for x in range(int(x0*S),int(x1*S)-k):
                outer=rect(np.array([y]),np.array([x]),k,k)[0]
                inner=rect(np.array([y+t]),np.array([x+t]),k-2*t,k-2*t)[0]
                pc=k*k-(k-2*t)**2; ic=(k-2*t)**2
                if outer-inner>=0.5*pc and inner<=0.22*ic:
                    cand.append(((x+k/2)/S,(y+k/2)/S,(outer-inner)/pc-inner/ic))
    cand.sort(key=lambda z:-z[2]); m=[]
    for cx,cy,s in cand:
        if all(not(abs(a-cx)<4 and abs(b-cy)<4) for a,b in m): m.append((round(cx,1),round(cy,1)))
    return sorted(m,key=lambda z:z[0])
bands=[("TIPO",99,104,15,260),("DESRAT_ext",128,150,15,40),("DESRAT_t1",148,156,40,260),
("DESRAT_t2",159,168,40,260),("DESRAT_praga",172,182,40,260),
("DESINSET_ext",210,230,15,40),("DESINSET_t1",228,238,40,260),("DESINSET_t2",240,250,40,260),
("DESINSET_p1",253,263,30,260),("DESINSET_p2",265,275,30,260),
("COMP",246,302,15,230)]
for name,y0,y1,x0,x1 in bands:
    print("%-13s y%g-%g:"%(name,y0,y1), [ (a,b) for a,b in find(y0,y1,x0,x1)])
doc.close()
