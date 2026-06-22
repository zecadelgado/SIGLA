import fitz
V=r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf'
doc=fitz.open(V); page=doc[0]; B="hebo"
def Xc(cx,cy,size=7,col=(1,0,0)):
    w=fitz.get_text_length("X",fontname=B,fontsize=size); page.insert_text((cx-w/2,cy+size*0.35),"X",fontsize=size,fontname=B,color=col)
for cx in [78,143,208]: Xc(cx,205)        # red = my guess
page.get_pixmap(matrix=fitz.Matrix(9,9),clip=fitz.Rect(60,198,264.7,214)).save(r'var/vzoom_tec.png')
print("ok"); doc.close()
