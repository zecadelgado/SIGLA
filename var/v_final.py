import fitz
V=r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf'
doc=fitz.open(V); page=doc[0]
Hf="helv"; B="hebo"
def t(x,topo,size,txt):
    if txt: page.insert_text((x,topo+size),txt,fontsize=size,fontname=Hf,color=(0,0,0))
def Xc(cx,cy,size=7):
    w=fitz.get_text_length("X",fontname=B,fontsize=size); page.insert_text((cx-w/2,cy+size*0.35),"X",fontsize=size,fontname=B,color=(0,0,0))
# top
t(185,10,7,"22/06/2026"); t(250,10,7,"14h"); t(200,27,7,"Joao Silva"); t(200,39,7,"Maria Souza")
t(182,55,7,"08:00"); t(236,55,7,"11:30")
# client
t(38,51,8,"Empresa Exemplo Ltda"); t(175,51,8,"12.345.678/0001-90")
t(48,60,8,"Rua das Flores, 100"); t(172,60,8,"54 99999-0000")
t(53,69,8,"Sananduva"); t(150,69,8,"RS"); t(214,69,8,"Carlos Lima")
# tipo
for cx in [24.5,69.6,122.2,168.6,210.2]: Xc(cx,101)
# desrat
Xc(25,128); Xc(25,138); t(62,124,7.5,"Cozinha, deposito")
for cx in [78,143,208]: Xc(cx,144)
for cx in [78,143,208]: Xc(cx,155)
for cx in [78,150,210]: Xc(cx,166)
# desinset
Xc(25,177); Xc(25,188); t(62,173,7.5,"Salao, banheiros")
for cx in [78,143,208]: Xc(cx,196)
for cx in [78,143,208]: Xc(cx,207)
for cx in [76,114,150,188,222]: Xc(cx,217)
for cx in [76,143,205]: Xc(cx,228)
# componente
for cy in [250,258,266,274,282,290]: Xc(16,cy)
for cy in [250,258,266,274,282,290]: Xc(81,cy)
for cy in [250,258,266,274,282,290,298]: Xc(143,cy)
Xc(205,250)
# signatures
t(33,326,7,"Carlos Lima"); t(205,326,7,"Joao Silva")
page.get_pixmap(matrix=fitz.Matrix(6,6)).save(r'var/v_final.png'); print("ok")
doc.close()
