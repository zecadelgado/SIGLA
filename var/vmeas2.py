import fitz
V=r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf'
doc=fitz.open(V); page=doc[0]; Hf="helv"; B="hebo"
def t(x,topo,size,txt):
    if txt: page.insert_text((x,topo+size),txt,fontsize=size,fontname=Hf,color=(0,0,0))
def Xc(cx,cy,size=7):
    w=fitz.get_text_length("X",fontname=B,fontsize=size); page.insert_text((cx-w/2,cy+size*0.35),"X",fontsize=size,fontname=B,color=(0,0,0))
# desrat (to verify) + desinset (to fix)
Xc(25,128); Xc(25,138)
for cx in [78,143,208]: Xc(cx,144)
for cx in [78,143,208]: Xc(cx,155)
for cx in [78,150,210]: Xc(cx,166)
Xc(25,187); Xc(25,197)
for cx in [78,143,208]: Xc(cx,205)
for cx in [78,143,208]: Xc(cx,216)
for cx in [76,114,150,188,222]: Xc(cx,227)
for cx in [76,143,205]: Xc(cx,238)
W,Hh=page.rect.width,page.rect.height
for x in range(0,int(W)+1,5):
    col=(1,0,0) if x%20==0 else (0.72,0.85,1); page.draw_line((x,0),(x,Hh),color=col,width=0.13)
for y in range(0,int(Hh)+1,5):
    col=(0,0.6,0) if y%20==0 else (0.82,0.93,0.82); page.draw_line((0,y),(W,y),color=col,width=0.1)
for x in range(0,int(W)+1,20): page.insert_text((x+0.3,123),str(x),fontsize=2.6,color=(0,0,0.8))
for yy in range(0,int(Hh)+1,5): page.insert_text((0.3,yy-0.3),str(yy),fontsize=2.3,color=(0,0.45,0))
page.get_pixmap(matrix=fitz.Matrix(8,8),clip=fitz.Rect(0,122,264.7,245)).save(r'var/vm_desrat_desinset.png')
print("ok"); doc.close()
