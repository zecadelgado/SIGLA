import fitz

def grid(src, out, scale):
    doc = fitz.open(src)
    page = doc[0]
    W, H = page.rect.width, page.rect.height
    # minor lines every 10pt (light grey), major every 50pt (red) with labels
    for x in range(0, int(W)+1, 10):
        col = (1,0,0) if x % 50 == 0 else (0.7,0.85,1)
        page.draw_line((x,0),(x,H), color=col, width=0.3)
    for y in range(0, int(H)+1, 10):
        col = (1,0,0) if y % 50 == 0 else (0.7,0.85,1)
        page.draw_line((0,y),(W,y), color=col, width=0.3)
    for x in range(0, int(W)+1, 50):
        page.insert_text((x+1, 7), str(x), fontsize=5, color=(0,0,0.8))
    for y in range(0, int(H)+1, 50):
        page.insert_text((1, y-1), str(y), fontsize=5, color=(0,0,0.8))
    pix = page.get_pixmap(matrix=fitz.Matrix(scale,scale))
    pix.save(out)
    print(out, pix.width,'x',pix.height, '| page pt', round(W,1),'x',round(H,1))
    doc.close()

grid(r'sigla-relatorios/src/main/resources/formularios/ordem-servico-modelo.pdf', r'var/grid_os.png', 6)
grid(r'sigla-relatorios/src/main/resources/formularios/relatorio-visita-modelo.pdf', r'var/grid_visita.png', 6)
