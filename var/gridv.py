import fitz
V=r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf'
def grid_clip(out, clip, scale):
    doc=fitz.open(V); page=doc[0]; W,H=page.rect.width,page.rect.height
    for x in range(0,int(W)+1,10):
        col=(1,0,0) if x%50==0 else (0.75,0.85,1); page.draw_line((x,0),(x,H),color=col,width=0.25)
    for y in range(0,int(H)+1,10):
        col=(1,0,0) if y%50==0 else (0.75,0.85,1); page.draw_line((0,y),(W,y),color=col,width=0.25)
    for x in range(0,int(W)+1,20): page.insert_text((x+0.5,clip[1]+5),str(x),fontsize=3.5,color=(0,0,0.8))
    for y in range(0,int(H)+1,10): page.insert_text((clip[0]+0.5,y-0.6),str(y),fontsize=3.0,color=(0,0.5,0))
    pix=page.get_pixmap(matrix=fitz.Matrix(scale,scale),clip=fitz.Rect(*clip)); pix.save(out); print(out,pix.width,'x',pix.height); doc.close()
grid_clip(r'var/gv_top.png',(0,14,264.7,100),8)
grid_clip(r'var/gv_mid.png',(0,98,264.7,235),8)
grid_clip(r'var/gv_bot.png',(0,232,264.7,342.2),8)
