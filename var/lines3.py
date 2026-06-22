import fitz, numpy as np
doc=fitz.open(r'sigla-relatorios/src/main/resources/formularios/ordem-servico-modelo.pdf')
page=doc[0]; S=4.0
pix=page.get_pixmap(matrix=fitz.Matrix(S,S),colorspace=fitz.csGRAY)
img=np.frombuffer(pix.samples,np.uint8).reshape(pix.height,pix.width)
dark=(img<128).astype(np.int32)
def peaks(arr,th):
    out=[];i=0;n=len(arr)
    while i<n:
        if arr[i]>=th:
            j=i
            while j<n and arr[j]>=th:j+=1
            out.append(round(((i+j-1)/2)/S,1));i=j
        else:i+=1
    return out
# row1 only (y 61-72): vertical sub-dividers
sub=dark[int(61.5*S):int(72*S),:]; cs=sub.sum(axis=0)
print('row1 V dividers:', peaks(cs,0.7*sub.shape[0]))
# table bottom: H lines within PRODUTO col (x 250-300)
sub2=dark[:,int(250*S):int(300*S)]; rs=sub2.sum(axis=1)
print('PRODUTO col H lines:', peaks(rs,0.6*sub2.shape[1]))
# footer band verticals (y 202-260): the ETAPA|DE divider
sub3=dark[int(203*S):int(213*S),:]; cs3=sub3.sum(axis=0)
print('ETAPA row V divider:', peaks(cs3,0.6*sub3.shape[0]))
doc.close()
