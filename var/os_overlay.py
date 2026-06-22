import fitz
M=r'sigla-relatorios/src/main/resources/formularios/ordem-servico-modelo.pdf'
doc=fitz.open(M); page=doc[0]
HELV="helv"; HELVB="hebo"
def t(x,topo,size,txt,bold=False):
    if not txt: return
    page.insert_text((x,topo+size),txt,fontsize=size,fontname=(HELVB if bold else HELV),color=(0,0,0))
def tfit(x,topo,wmax,size,txt):
    if not txt: return
    s=size
    while s>5 and fitz.get_text_length(txt,fontsize=s,fontname=HELV)>wmax: s-=0.5
    page.insert_text((x,topo+s),txt,fontsize=s,fontname=HELV,color=(0,0,0))
def X(cx,topo,size=8):
    w=fitz.get_text_length("X",fontsize=size,fontname=HELVB)
    page.insert_text((cx-w/2,topo+size),"X",fontsize=size,fontname=HELVB,color=(0,0,0))
# fields
tfit(41,63,31,8,"22/06/2026"); t(264,63,8,"Joao Silva")
t(52,75,8,"Empresa Exemplo Ltda"); t(246,75,8,"08:00")
t(45,87,8,"12.345.678/0001-90"); t(258,87,8,"11:30")
t(46,99,8,"contato@exemplo.com"); t(262,99,8,"54 99999-0000")
X(122,63)  # MANHA after label
for topo in [130,144.3,158.6,172.9,187.2]: X(14,topo)        # col1
for topo in [132,144,158,172]: X(173.5,topo)                 # col3
c4=[125,134.4,143.8,153.2,162.6,172,181.4,190.8]
for topo in c4: X(252.5,topo)                                # col4
for topo in c4:
    w=fitz.get_text_length("5",fontsize=8,fontname=HELV); page.insert_text((318-w/2,topo+8),"5",fontsize=8,fontname=HELV)
t(145,126.5,7.5,"500ml"); t(112,138.5,7.5,"10L"); t(145,170.5,7.5,"250g"); t(112,182.5,7.5,"5L")
t(46,203,8,"1"); t(188,203,8,"3"); t(40,216,8,"Aplicacao conforme contrato.")
page.get_pixmap(matrix=fitz.Matrix(6,6)).save(r'var/os_test.png'); print("ok")
doc.close()
