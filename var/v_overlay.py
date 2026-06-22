import fitz
V=r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf'
doc=fitz.open(V); page=doc[0]; W,Hh=page.rect.width,page.rect.height
Hf="helv"; B="hebo"
def t(x,topo,size,txt):
    if txt: page.insert_text((x,topo+size),txt,fontsize=size,fontname=Hf,color=(0,0,1))
def Xc(cx,cy,size=7):
    w=fitz.get_text_length("X",fontname=B,fontsize=size); page.insert_text((cx-w/2,cy+size*0.35),"X",fontsize=size,fontname=B,color=(1,0,0))
# ---- marks (current best guess) ----
t(199,9,7,"22/06/2026"); t(246,9,7,"14h"); t(199,25,7,"Joao Silva"); t(199,38,7,"Maria Souza")
t(182,54,7,"08:00"); t(236,54,7,"11:30")
t(38,51,8,"Empresa X"); t(175,51,8,"12.345/0001-90"); t(48,60,8,"Rua das Flores"); t(172,60,8,"54 9999"); t(53,69,8,"Sananduva"); t(150,69,8,"RS"); t(214,69,8,"Carlos")
for cx in [25,81,121,163,206]: Xc(cx,102)
for cy in [133,145]: Xc(25,cy)
for cx in [78,140,207]: Xc(cx,152)
for cx in [78,140,207]: Xc(cx,163)
for cx in [61,152,212]: Xc(cx,178)
for cy in [214,226]: Xc(25,cy)
for cx in [78,140,207]: Xc(cx,233)
for cx in [78,140,207]: Xc(cx,245)
for cx in [45,95,140,180,212]: Xc(cx,258)
for cx in [45,110,170]: Xc(cx,270)
for cy in [250,258,266,274,282,290]: Xc(25,cy)
for cy in [250,258,266,274,282,290]: Xc(88,cy)
for cy in [250,258,266,274,282,290,298]: Xc(150,cy)
Xc(205,250)
t(35,335,7,"Carlos"); t(200,335,7,"Joao")
# ---- grid on top (faint) ----
for x in range(0,int(W)+1,10):
    col=(1,0,0) if x%50==0 else (0.7,0.85,1); page.draw_line((x,0),(x,Hh),color=col,width=0.2)
for y in range(0,int(Hh)+1,5):
    col=(0,0.6,0) if y%50==0 else (0.8,0.92,0.8); page.draw_line((0,y),(W,y),color=col,width=0.15)
for x in range(0,int(W)+1,20): page.insert_text((x+0.4,6),str(x),fontsize=3,color=(0,0,0.8))
for y in range(0,int(Hh)+1,10): page.insert_text((0.4,y-0.4),str(y),fontsize=2.6,color=(0,0.5,0))
for out,clip in [('var/vt_top.png',(140,2,264.7,75)),('var/vt_mid.png',(0,95,264.7,235)),('var/vt_bot.png',(0,232,264.7,342.2))]:
    page.get_pixmap(matrix=fitz.Matrix(9,9),clip=fitz.Rect(*clip)).save(out)
print("ok")
doc.close()
