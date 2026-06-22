import fitz
V=r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf'
doc=fitz.open(V); page=doc[0]; Hf="helv"; B="hebo"
def t(x,topo,size,txt):
    if txt: page.insert_text((x,topo+size),txt,fontsize=size,fontname=Hf,color=(0,0,0))
def Xc(cx,cy,size=7):
    w=fitz.get_text_length("X",fontname=B,fontsize=size); page.insert_text((cx-w/2,cy+size*0.35),"X",fontsize=size,fontname=B,color=(0,0,1))
TEC=[55,110,172]
# desrat
Xc(25,128); Xc(25,138)
for cx in TEC: Xc(cx,144)
for cx in TEC: Xc(cx,155)
for cx in [58,150,235]: Xc(cx,166)
# desinset
Xc(25,187); Xc(25,197)
for cx in TEC: Xc(cx,205)
for cx in TEC: Xc(cx,216)
for cx in [55,105,147,185,219,249]: Xc(cx,227)
for cx in [55,105,168]: Xc(cx,238)
W,Hh=page.rect.width,page.rect.height
for x in range(20,265,5):
    col=(1,0,0) if x%20==0 else (0.6,0.8,1); page.draw_line((x,124),(x,242),color=col,width=0.12)
for x in range(20,265,20): page.insert_text((x+0.3,126),str(x),fontsize=2.3,color=(0,0,0.8))
for yy in range(120,243,5): page.insert_text((20.3,yy-0.3),str(yy),fontsize=2.2,color=(0,0.45,0)); page.draw_line((20,yy),(264.7,yy),color=(0.85,0.93,0.85),width=0.08)
page.get_pixmap(matrix=fitz.Matrix(8,8),clip=fitz.Rect(20,123,264.7,243)).save(r'var/vm3.png')
print("ok"); doc.close()
