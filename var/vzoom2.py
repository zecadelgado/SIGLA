import fitz
V=r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf'
doc=fitz.open(V); page=doc[0]; B="hebo"
# fine grid x every 5
W,Hh=page.rect.width,page.rect.height
for x in range(20,265,5):
    col=(1,0,0) if x%20==0 else (0.6,0.8,1); page.draw_line((x,198),(x,232),color=col,width=0.15)
for x in range(20,265,20): page.insert_text((x+0.3,201),str(x),fontsize=2.4,color=(0,0,0.8))
page.get_pixmap(matrix=fitz.Matrix(7,7),clip=fitz.Rect(20,201,264.7,232)).save(r'var/vzoom_full.png')
print("ok"); doc.close()
