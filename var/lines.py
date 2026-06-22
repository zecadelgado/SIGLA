import fitz, numpy as np
def detect(src, name):
    doc=fitz.open(src); page=doc[0]; W,H=page.rect.width,page.rect.height
    S=4.0
    pix=page.get_pixmap(matrix=fitz.Matrix(S,S), colorspace=fitz.csGRAY)
    img=np.frombuffer(pix.samples, np.uint8).reshape(pix.height,pix.width)
    dark=(img<128).astype(np.int32)
    colsum=dark.sum(axis=0); rowsum=dark.sum(axis=1)
    # vertical lines: columns where dark covers > 45% of height
    vth=0.45*pix.height; hth=0.45*pix.width
    def peaks(arr, th):
        out=[]; i=0; n=len(arr)
        while i<n:
            if arr[i]>=th:
                j=i
                while j<n and arr[j]>=th: j+=1
                out.append(round(((i+j-1)/2)/S,1)); i=j
            else: i+=1
        return out
    print('====',name,'page pt %.1f x %.1f'%(W,H))
    print(' V lines pt:', peaks(colsum,vth))
    print(' H lines pt:', peaks(rowsum,hth))
    doc.close()
detect(r'sigla-relatorios/src/main/resources/formularios/ordem-servico-modelo.pdf','OS')
detect(r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf','VISITA')
