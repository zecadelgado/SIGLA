import fitz
V=r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf'
doc=fitz.open(V); page=doc[0]; Hf="helv"; B="hebo"
def t(x,topo,size,txt):
    if txt: page.insert_text((x,topo+size),txt,fontsize=size,fontname=Hf,color=(0,0,0))
def Xc(cx,cy,size=7):
    w=fitz.get_text_length("X",fontname=B,fontsize=size); page.insert_text((cx-w/2,cy+size*0.35),"X",fontsize=size,fontname=B,color=(0,0,0))
t(185,10,7,"22/06/2026"); t(250,10,7,"14h"); t(200,27,7,"Joao Silva"); t(200,39,7,"Maria Souza")
t(182,55,7,"08:00"); t(236,55,7,"11:30")
Xc(25,177); Xc(25,188); t(62,173,7.5,"Salao, banheiros")
for cx in [78,143,208]: Xc(cx,196)
for cx in [78,143,208]: Xc(cx,207)
for cx in [76,114,150,188,222]: Xc(cx,217)
for cx in [76,143,205]: Xc(cx,228)
for cy in [250,258,266,274,282,290]: Xc(16,cy)
for cy in [250,258,266,274,282,290]: Xc(81,cy)
for cy in [250,258,266,274,282,290,298]: Xc(143,cy)
Xc(205,250)
W,Hh=page.rect.width,page.rect.height
for x in range(0,int(W)+1,5):
    col=(1,0,0) if x%20==0 else (0.72,0.85,1); page.draw_line((x,0),(x,Hh),color=col,width=0.15)
for y in range(0,int(Hh)+1,5):
    col=(0,0.6,0) if y%20==0 else (0.82,0.93,0.82); page.draw_line((0,y),(W,y),color=col,width=0.12)
for x in range(0,int(W)+1,20): page.insert_text((x+0.3,4),str(x),fontsize=2.6,color=(0,0,0.8))
for yy in range(0,int(Hh)+1,10): page.insert_text((0.3,yy-0.3),str(yy),fontsize=2.6,color=(0,0.45,0))
page.get_pixmap(matrix=fitz.Matrix(10,10),clip=fitz.Rect(140,2,264.7,72)).save(r'var/vf_top.png')
page.get_pixmap(matrix=fitz.Matrix(9,9),clip=fitz.Rect(0,168,264.7,300)).save(r'var/vf_low.png')
print("ok"); doc.close()
