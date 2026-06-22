import fitz, numpy as np
def load(src):
    doc=fitz.open(src); page=doc[0]; W,H=page.rect.width,page.rect.height
    S=4.0
    pix=page.get_pixmap(matrix=fitz.Matrix(S,S), colorspace=fitz.csGRAY)
    img=np.frombuffer(pix.samples,np.uint8).reshape(pix.height,pix.width)
    return (img<128).astype(np.int32), S, W, H
def peaks(arr, th, S):
    out=[]; i=0; n=len(arr)
    while i<n:
        if arr[i]>=th:
            j=i
            while j<n and arr[j]>=th: j+=1
            out.append(round(((i+j-1)/2)/S,1)); i=j
        else: i+=1
    return out
def vbands(dark,S,bands,frac=0.7):
    for (y0,y1,label) in bands:
        sub=dark[int(y0*S):int(y1*S),:]
        cs=sub.sum(axis=0); th=frac*sub.shape[0]
        print('  Vlines [%s] y%g-%g:'%(label,y0,y1), peaks(cs,th,S))
def hbands(dark,S,bands,frac=0.6):
    for (x0,x1,label) in bands:
        sub=dark[:,int(x0*S):int(x1*S)]
        rs=sub.sum(axis=1); th=frac*sub.shape[1]
        print('  Hlines [%s] x%g-%g:'%(label,x0,x1), peaks(rs,th,S))

print('==== OS ====')
dark,S,W,H=load(r'sigla-relatorios/src/main/resources/formularios/ordem-servico-modelo.pdf')
vbands(dark,S,[(62,108,'fields'),(127,200,'table')])
print('==== VISITA ====')
dark,S,W,H=load(r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf')
vbands(dark,S,[(120,128,'tipoVisitaBar'),(212,226,'desratBar'),(232,300,'componentRows'),(300,338,'signatures')])
