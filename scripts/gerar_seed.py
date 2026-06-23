# -*- coding: utf-8 -*-
"""
Gera o SQL de popularização do banco SIGLA (dedetizadora de pequeno porte).
- Lê os 17 potenciais clientes do Excel e emite-os como leads (cliente_indicacoes).
- Converte 4 leads em clientes ativos (cadastro) + contratos/agenda/OS/financeiro.
- Cria 4 clientes fictícios, funcionários, produtos, estoque e financeiro.
UUIDs determinísticos via md5(chave) para permitir referências cruzadas e re-execução idempotente.
"""
import hashlib
import openpyxl
import datetime as dt

XLSX = r"C:\Users\estef\Downloads\potenciais_clientes_passo_fundo_2026-06-23.xlsx"
OUT = r"D:\SIGLA\scripts\seed_dedetizadora.sql"

ADMIN_USER = "a5d19704-d558-4034-91d5-a9b570109306"   # usuarios: Luciano (ADMIN)
FUNC_JOAO = "2a45267e-eca0-431c-ba79-a22cb7ee3afc"     # cadastro FUNCIONARIO existente (Operador)
FUNC_JULIANO = "1841bdf5-dec4-4227-b17a-c41f9198a0c0"  # cadastro FUNCIONARIO existente (Gerente)

def uid(key: str) -> str:
    """UUID determinístico equivalente a md5(key)::uuid no Postgres."""
    h = hashlib.md5(key.encode("utf-8")).hexdigest()
    return str(__import__("uuid").UUID(hex=h))

def q(v):
    """Literal SQL seguro."""
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, (int, float)):
        return str(v)
    s = str(v).replace("'", "''")
    return "'" + s + "'"

# ---------- CPF/CNPJ fictícios porém válidos (dígitos verificadores corretos) ----------
def cpf_valido(seed: int) -> str:
    import random
    r = random.Random(seed)
    n = [r.randint(0, 9) for _ in range(9)]
    for _ in range(2):
        s = sum((len(n) + 1 - i) * v for i, v in enumerate(n))
        d = (s * 10) % 11
        n.append(0 if d == 10 else d)
    return "{}{}{}.{}{}{}.{}{}{}-{}{}".format(*n)

def cnpj_valido(seed: int) -> str:
    import random
    r = random.Random(seed)
    n = [r.randint(0, 9) for _ in range(8)] + [0, 0, 0, 1]
    pesos1 = [5,4,3,2,9,8,7,6,5,4,3,2]
    pesos2 = [6,5,4,3,2,9,8,7,6,5,4,3,2]
    s = sum(p*v for p, v in zip(pesos1, n)); d1 = 0 if (s % 11) < 2 else 11 - (s % 11)
    n.append(d1)
    s = sum(p*v for p, v in zip(pesos2, n)); d2 = 0 if (s % 11) < 2 else 11 - (s % 11)
    n.append(d2)
    return "{}{}.{}{}{}.{}{}{}/{}{}{}{}-{}{}".format(*n)

# ============================ LER EXCEL (17 leads) ============================
wb = openpyxl.load_workbook(XLSX, data_only=True)
ws = wb["Potenciais Clientes"]
rows = list(ws.iter_rows(min_row=5, values_only=True))  # dados começam na linha 5 (1-based)
leads = [r for r in rows if r and r[0] is not None]

# Mapa: ID do Excel -> chave do cliente convertido (cadastro)
CONVERT = {9: "cli-maita", 10: "cli-prix", 12: "cli-itatiaia", 15: "cli-notredame"}

out = []
def emit(s=""):
    out.append(s)

emit("-- ============================================================")
emit("-- SEED DEDETIZADORA SIGLA  (gerado automaticamente)")
emit("-- Banco de produção: insere apenas DADOS. Não altera schema.")
emit("-- Idempotente: ON CONFLICT (id) DO NOTHING.")
emit("-- ============================================================")
emit()

# ---------------------------------------------------------------- BATCH 1: financeiro ref
emit("-- ===== BATCH 01: FINANCEIRO REFERENCIAS (formas + categorias) =====")
formas = [("PIX","fpag-pix"),("DINHEIRO","fpag-dinheiro"),("BOLETO","fpag-boleto"),("CARTAO","fpag-cartao"),("TRANSFERENCIA","fpag-transf")]
for nome, k in formas:
    emit(f"INSERT INTO financeiro_formas_pagamento (id,nome,ativo) VALUES ({q(uid(k))},{q(nome)},true) ON CONFLICT (nome) DO NOTHING;")
cats = [("ENTRY","SERVICOS","fcat-servicos"),("ENTRY","CONTRATOS","fcat-contratos"),
        ("EXPENSE","PRODUTOS","fcat-produtos"),("EXPENSE","COMBUSTIVEL","fcat-combustivel"),
        ("EXPENSE","SALARIOS","fcat-salarios"),("EXPENSE","ALIMENTACAO","fcat-alimentacao"),
        ("EXPENSE","EXTRAS","fcat-extras")]
for tipo, nome, k in cats:
    emit(f"INSERT INTO financeiro_categorias (id,tipo,nome,ativo) VALUES ({q(uid(k))},{q(tipo)},{q(nome)},true) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 2: funcionarios (+3)
emit("-- ===== BATCH 02: FUNCIONARIOS (3 novos, cadastro tipo=FUNCIONARIO) =====")
funcs = [
    ("fn-anderson","Anderson Refatti", cpf_valido(101),"(54) 99645-2210","anderson.refatti.sigla@gmail.com","Técnico aplicador","Boqueirão"),
    ("fn-cristiano","Cristiano Bavaresco", cpf_valido(102),"(54) 99712-8834","cristiano.bavaresco.sigla@gmail.com","Técnico aplicador","Vila Rodrigues"),
    ("fn-patricia","Patrícia Webber", cpf_valido(103),"(54) 99188-4471","patricia.webber.sigla@gmail.com","Auxiliar administrativo","Centro"),
]
for k,nome,cpf,tel,email,cargo,bairro in funcs:
    emit("INSERT INTO cadastro (id,tipo,nome,nome_fantasia,cpf,telefone_principal,email,cep,rua,numero,bairro,cidade,estado,cargo,situacao,ativo) VALUES "
         f"({q(uid(k))},'FUNCIONARIO',{q(nome)},{q(nome)},{q(cpf)},{q(tel)},{q(email)},'99000-000','Rua das Acácias','100',{q(bairro)},'Passo Fundo','RS',{q(cargo)},'ATIVO',true) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 3: clientes
emit("-- ===== BATCH 03: CLIENTES (4 convertidos do Excel + 4 ficticios) =====")
# converted: usa dados reais do Excel; sem inventar CNPJ/razao_social/responsavel real.
lead_by_id = {r[0]: r for r in leads}
conv_meta = {
    "cli-maita":   {"obs":"Convertido de potencial cliente (prospecção 23/06/2026). Hotel com hospedagem, eventos e restaurante; contrato preventivo mensal.","contato":"Gerência / Manutenção"},
    "cli-prix":    {"obs":"Convertido de potencial cliente (prospecção 23/06/2026). Hotel; contrato preventivo mensal de áreas comuns e cozinha.","contato":"Governança / Manutenção"},
    "cli-itatiaia":{"obs":"Convertido de potencial cliente (prospecção 23/06/2026). Hotel; contrato encerrado, em renegociação.","contato":"Gerência"},
    "cli-notredame":{"obs":"Convertido de potencial cliente (prospecção 23/06/2026). Escola com alimentação e eventos; serviços fora do horário de aula.","contato":"Direção administrativa"},
}
def col(r, i):
    v = r[i]
    return v if (v is not None and str(v).strip() != "") else None

for exid, k in CONVERT.items():
    r = lead_by_id[exid]
    nome = col(r,2); email = col(r,9); cep=col(r,10); rua=col(r,11); numero=col(r,12)
    compl=col(r,13); bairro=col(r,14); cidade=col(r,15) or "Passo Fundo"; estado=col(r,16) or "RS"
    tel = col(r,7)
    meta = conv_meta[k]
    emit("INSERT INTO cadastro (id,tipo,nome,nome_fantasia,telefone_principal,email,cep,rua,numero,complemento,bairro,cidade,estado,observacoes,ativo) VALUES "
         f"({q(uid(k))},'CLIENTE',{q(nome)},{q(nome)},{q(tel)},{q(email)},{q(cep)},{q(rua)},{q(numero)},{q(compl)},{q(bairro)},{q(cidade)},{q(estado)},{q(meta['obs'])},true) ON CONFLICT (id) DO NOTHING;")
    # responsavel (contato por papel, sem nome de pessoa real)
    emit("INSERT INTO cliente_responsaveis (id,cliente_id,nome,cargo,telefone,principal,ativo) VALUES "
         f"({q(uid('resp-'+k))},{q(uid(k))},{q(meta['contato'])},{q(meta['contato'])},{q(tel)},true,true) ON CONFLICT (id) DO NOTHING;")

# ficticios
fic = [
    # key, nome, tipo_pf?, doc, tel, email, cep, rua, numero, bairro, obs
    ("cli-maria","Maria Aparecida Gomes",True, cpf_valido(201),"(54) 99634-1188","mariaapgomes.sigla@gmail.com","99025-180","Rua Lava Pés","845","Boqueirão","Cliente residencial. Dedetização preventiva semestral."),
    ("cli-jose","José Carlos Pereira",True, cpf_valido(202),"(54) 99815-7723","josecpereira.sigla@gmail.com","99074-360","Rua Antônio Araújo","260","Vera Cruz","Cliente residencial. Controle de formigas e baratas."),
    ("cli-padaria","Padaria e Confeitaria Pão da Casa Ltda",False, cnpj_valido(301),"(54) 3045-2299","contato.paodacasa.sigla@gmail.com","99010-120","Avenida Sete de Setembro","712","Centro","Pequeno comércio (alimentação). Controle mensal de pragas na produção."),
    ("cli-restaurante","Restaurante Sabor da Serra Ltda",False, cnpj_valido(302),"(54) 3312-7766","saborr.serra.sigla@gmail.com","99050-260","Avenida Brasil Leste","1530","Petrópolis","Restaurante. Controle quinzenal de baratas e roedores; exige certificado sanitário."),
]
for k,nome,is_pf,doc,tel,email,cep,rua,numero,bairro,obs in fic:
    fantasia = nome if not is_pf else None
    cpf = doc if is_pf else None
    cnpj = doc if not is_pf else None
    emit("INSERT INTO cadastro (id,tipo,nome,nome_fantasia,cpf,cnpj,telefone_principal,email,cep,rua,numero,bairro,cidade,estado,observacoes,ativo) VALUES "
         f"({q(uid(k))},'CLIENTE',{q(nome)},{q(fantasia)},{q(cpf)},{q(cnpj)},{q(tel)},{q(email)},{q(cep)},{q(rua)},{q(numero)},{q(bairro)},'Passo Fundo','RS',{q(obs)},true) ON CONFLICT (id) DO NOTHING;")
    if not is_pf:
        emit("INSERT INTO cliente_responsaveis (id,cliente_id,nome,cargo,telefone,principal,ativo) VALUES "
             f"({q(uid('resp-'+k))},{q(uid(k))},{q('Responsável')},{q('Proprietário')},{q(tel)},true,true) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 4: leads (17)
emit("-- ===== BATCH 04: POTENCIAIS CLIENTES / LEADS (cliente_indicacoes) =====")
for r in leads:
    exid = r[0]
    nome = col(r,2)
    tel = col(r,7) or col(r,8) or ""
    seg = col(r,25); prio = col(r,24); origem = col(r,21); prox = col(r,32); obse = col(r,27)
    partes = []
    if seg: partes.append(f"Segmento: {seg}")
    if prio: partes.append(f"Prioridade: {prio}")
    if origem: partes.append(origem)
    if prox: partes.append(f"Próxima ação: {prox}")
    if obse: partes.append(f"Obs.: {obse}")
    observ = " · ".join(partes)
    k = f"lead-{exid}"
    if exid in CONVERT:
        cli_uuid = uid(CONVERT[exid])
        status = "convertido"
        observ = observ + f"\n[CONVERTIDO] Cliente gerado: {cli_uuid}"
    else:
        status = "novo"
    emit("INSERT INTO cliente_indicacoes (id,nome_indicado,telefone,cliente_indicador_id,data_indicacao,status,observacoes) VALUES "
         f"({q(uid(k))},{q(nome)},{q(tel)},NULL,'2026-06-23',{q(status)},{q(observ)}) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 5: produtos
emit("-- ===== BATCH 05: PRODUTOS (estoque) =====")
produtos = [
 # key, nome, descricao, sku, unidade, custo, venda, qtd, min
 ("pr-inset","Inseticida líquido concentrado (Cipermetrina)","Concentrado para diluição e pulverização","INS-CIP-100","L",120.00,0,18,5),
 ("pr-gel","Gel inseticida para baratas (seringa 20g)","Iscas em gel para controle de baratas","GEL-BAR-20","UN",38.00,0,24,6),
 ("pr-ratic","Raticida bloco parafinado","Blocos para desratização","RAT-BLO-1","KG",45.00,0,12,3),
 ("pr-isca","Porta-iscas para roedores","Estação de isca lacrável","ISC-PIR-1","UN",22.00,0,30,8),
 ("pr-formiga","Isca granulada formicida","Controle de formigas","ISC-FOR-1","KG",36.00,0,8,2),
 ("pr-larv","Larvicida (controle em caixa d'água)","Biolarvicida para reservatórios","LAR-BIO-1","L",95.00,0,6,2),
 ("pr-cupim","Cupinicida (imidacloprido)","Controle e barreira química de cupins","CUP-IMI-1","L",160.00,0,7,2),
 ("pr-pulv","Pulverizador costal manual 20L","Equipamento de aplicação","EQ-PUL-20","UN",210.00,0,4,1),
 ("pr-bomba","Bomba costal motorizada 25L","Equipamento de aplicação motorizado","EQ-BOM-25","UN",980.00,0,2,1),
 ("pr-luva","Luvas nitrílicas (par)","EPI - proteção das mãos","EPI-LUV-1","PAR",9.50,0,60,15),
 ("pr-masc","Máscara respiratória PFF2","EPI - proteção respiratória","EPI-MAS-2","UN",7.00,0,80,20),
 ("pr-macac","Macacão de proteção descartável","EPI - proteção do corpo","EPI-MAC-1","UN",28.00,0,25,6),
 ("pr-oculos","Óculos de proteção","EPI - proteção ocular","EPI-OCU-1","UN",18.00,0,15,4),
 ("pr-desinc","Detergente desincrustante (limpeza caixa d'água)","Higienização de reservatórios","LIM-DES-1","L",32.00,0,10,3),
]
for k,nome,desc,sku,un,custo,venda,qtd,mn in produtos:
    emit("INSERT INTO produtos (id,nome,descricao,sku,unidade,valor_custo,valor_venda,quantidade_atual,quantidade_minima,ativo) VALUES "
         f"({q(uid(k))},{q(nome)},{q(desc)},{q(sku)},{q(un)},{custo},{venda},{qtd},{mn},true) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 6: contratos
emit("-- ===== BATCH 06: CONTRATOS =====")
# key, cliente_key, descricao, tipo(MONTHLY/QUINZENAL), inicio, fim, valor, status, obs
contratos = [
 ("ct-maita","cli-maita","Controle integrado de pragas - áreas comuns, cozinha e eventos","MONTHLY","2026-02-01","2027-01-31",650.00,"ACTIVE","Visita preventiva mensal + revisitas sob demanda."),
 ("ct-prix","cli-prix","Controle preventivo de pragas - áreas comuns e cozinha","MONTHLY","2026-03-01","2027-02-28",550.00,"ACTIVE","Inclui higienização semestral de caixa d'água."),
 ("ct-notre","cli-notredame","Controle de pragas escolar - aplicações fora do horário de aula","MONTHLY","2026-03-15","2027-03-14",700.00,"ACTIVE","Execução aos sábados."),
 ("ct-padaria","cli-padaria","Controle mensal de pragas na produção e salão","MONTHLY","2026-04-01","2027-03-31",280.00,"ACTIVE","Foco em baratas e roedores."),
 ("ct-rest","cli-restaurante","Controle quinzenal de baratas e roedores","QUINZENAL","2026-04-10","2027-04-09",320.00,"ACTIVE","Emissão de certificado sanitário a cada visita."),
 ("ct-itatiaia","cli-itatiaia","Controle preventivo de pragas - hotel","MONTHLY","2025-06-01","2026-05-31",480.00,"EXPIRED","Contrato encerrado; cliente em renegociação."),
]
for k,ck,desc,tipo,ini,fim,valor,status,obs in contratos:
    emit("INSERT INTO contratos (id,cliente_id,descricao,tipo_contrato,data_inicio,data_fim,valor_mensal,status,alerta_ativo,dias_alerta_fim,observacoes) VALUES "
         f"({q(uid(k))},{q(uid(ck))},{q(desc)},{q(tipo)},{q(ini)},{q(fim)},{valor},{q(status)},true,30,{q(obs)}) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 7: ordens de servico
emit("-- ===== BATCH 07: ORDENS DE SERVICO =====")
# key, cliente_key, contrato_key|None, titulo, tipo_servico, status, data_agendada, data_inicio, data_fim, exec_func, resp_func, foi_feito, pago, valor, obs
OS = [
 ("os-maita-1","cli-maita","ct-maita","Desratização e controle de baratas - maio","Desratização","CONCLUIDA","2026-05-12 09:00","2026-05-12 09:10","2026-05-12 11:30","fn-anderson",FUNC_JULIANO,True,True,650.00,"Aplicação em cozinha, depósito e áreas comuns."),
 ("os-prix-1","cli-prix","ct-prix","Controle preventivo de pragas - maio","Dedetização comercial","CONCLUIDA","2026-05-20 14:00","2026-05-20 14:05","2026-05-20 16:00","fn-cristiano",FUNC_JULIANO,True,True,550.00,"Pulverização de áreas comuns e cozinha."),
 ("os-notre-1","cli-notredame","ct-notre","Controle de pragas escolar - junho","Dedetização comercial","CONCLUIDA","2026-06-07 08:00","2026-06-07 08:05","2026-06-07 12:00","fn-anderson",FUNC_JULIANO,True,True,700.00,"Aplicação no sábado, fora do horário de aula."),
 ("os-padaria-1","cli-padaria","ct-padaria","Controle de baratas e roedores - junho","Controle de baratas","CONCLUIDA","2026-06-10 07:30","2026-06-10 07:35","2026-06-10 09:00","fn-cristiano",FUNC_JOAO,True,True,280.00,"Gel em pontos críticos e iscas no depósito."),
 ("os-rest-1","cli-restaurante","ct-rest","Controle quinzenal - 1a quinzena junho","Controle de baratas","CONCLUIDA","2026-06-12 15:00","2026-06-12 15:05","2026-06-12 16:30","fn-anderson",FUNC_JOAO,True,False,320.00,"Emitido certificado sanitário."),
 ("os-maria-1","cli-maria",None,"Dedetização residencial","Dedetização residencial","CONCLUIDA","2026-06-05 10:00","2026-06-05 10:05","2026-06-05 11:15","fn-cristiano",FUNC_JOAO,True,True,190.00,"Aplicação em residência; foco em formigas e baratas."),
 # agendadas (futuro)
 ("os-prix-2","cli-prix","ct-prix","Higienização de caixa d'água","Higienização de caixa d'água","AGENDADA","2026-06-30 09:00",None,None,"fn-anderson",FUNC_JULIANO,False,False,380.00,"Limpeza e desinfecção do reservatório."),
 ("os-jose-1","cli-jose",None,"Controle de formigas - residência","Controle de formigas","AGENDADA","2026-07-02 14:00",None,None,"fn-cristiano",FUNC_JOAO,False,False,170.00,"Primeira visita; avaliar ninhos."),
 ("os-rest-2","cli-restaurante","ct-rest","Controle quinzenal - 2a quinzena junho","Descupinização","AGENDADA","2026-06-27 15:00",None,None,"fn-anderson",FUNC_JOAO,False,False,320.00,"Inspeção de cupins em estrutura de madeira."),
]
for row in OS:
    k,ck,ctk,tit,tipo,status,dag,dini,dfim,execf,respf,feito,pago,valor,obs = row
    ctid = f"{q(uid(ctk))}" if ctk else "NULL"
    diniv = q(dini) if dini else "NULL"
    dfimv = q(dfim) if dfim else "NULL"
    emit("INSERT INTO ordens_servico (id,cliente_id,contrato_id,titulo,tipo_servico,status,data_agendada,data_inicio,data_fim,executado_por_id,responsavel_interno_id,foi_feito,pago,valor_servico,assinatura_cliente,observacoes) VALUES "
         f"({q(uid(k))},{q(uid(ck))},{ctid},{q(tit)},{q(tipo)},{q(status)},{q(dag)},{diniv},{dfimv},{q(uid(execf))},{q(respf)},{str(feito).lower()},{str(pago).lower()},{valor},{str(feito).lower()},{q(obs)}) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 8: agenda
emit("-- ===== BATCH 08: AGENDA EVENTOS =====")
# key, cliente_key, contrato_key|None, os_key|None, titulo, tipo_evento(label), recorrencia, data_inicio, data_fim, status, prioridade, resp_func, notes
AG = [
 ("ag-maita-1","cli-maita","ct-maita","os-maita-1","Desratização - Maitá Palace","Desratização","mensal","2026-05-12 09:00","2026-05-12 11:30","COMPLETED","NORMAL","fn-anderson","Visita mensal concluída."),
 ("ag-prix-1","cli-prix","ct-prix","os-prix-1","Controle preventivo - Prix Hotel","Dedetização comercial","mensal","2026-05-20 14:00","2026-05-20 16:00","COMPLETED","NORMAL","fn-cristiano","Visita mensal concluída."),
 ("ag-notre-1","cli-notredame","ct-notre","os-notre-1","Controle escolar - Notre Dame","Dedetização comercial","mensal","2026-06-07 08:00","2026-06-07 12:00","COMPLETED","HIGH","fn-anderson","Execução no sábado."),
 ("ag-padaria-1","cli-padaria","ct-padaria","os-padaria-1","Controle de baratas - Pão da Casa","Controle de baratas","mensal","2026-06-10 07:30","2026-06-10 09:00","COMPLETED","NORMAL","fn-cristiano","Concluída antes da abertura."),
 ("ag-rest-1","cli-restaurante","ct-rest","os-rest-1","Controle quinzenal - Sabor da Serra","Controle de baratas","quinzenal","2026-06-12 15:00","2026-06-12 16:30","COMPLETED","NORMAL","fn-anderson","Certificado emitido."),
 ("ag-maria-1","cli-maria",None,"os-maria-1","Dedetização residencial - Maria A.","Dedetização residencial","nenhuma","2026-06-05 10:00","2026-06-05 11:15","COMPLETED","NORMAL","fn-cristiano","Serviço avulso concluído."),
 # futuro / agendadas
 ("ag-prix-2","cli-prix","ct-prix","os-prix-2","Higienização caixa d'água - Prix","Higienização de caixa d'água","nenhuma","2026-06-30 09:00","2026-06-30 11:00","SCHEDULED","HIGH","fn-anderson","Reservatório principal."),
 ("ag-jose-1","cli-jose",None,"os-jose-1","Controle de formigas - José Carlos","Controle de formigas","nenhuma","2026-07-02 14:00","2026-07-02 15:00","SCHEDULED","NORMAL","fn-cristiano","Primeira visita."),
 ("ag-rest-2","cli-restaurante","ct-rest","os-rest-2","Descupinização - Sabor da Serra","Descupinização","quinzenal","2026-06-27 15:00","2026-06-27 16:30","SCHEDULED","NORMAL","fn-anderson","Inspeção de cupins."),
 ("ag-maita-2","cli-maita","ct-maita",None,"Visita mensal - Maitá Palace (junho)","Manutenção periódica","mensal","2026-06-28 09:00","2026-06-28 11:00","SCHEDULED","NORMAL","fn-anderson","Visita preventiva de junho."),
 ("ag-notre-2","cli-notredame","ct-notre",None,"Revisita técnica - Notre Dame","Revisita técnica","nenhuma","2026-06-18 08:00","2026-06-18 09:30","MISSED","HIGH","fn-cristiano","Revisita não realizada; reagendar."),
 ("ag-padaria-2","cli-padaria","ct-padaria",None,"Controle de baratas - Pão da Casa (julho)","Controle de baratas","mensal","2026-07-10 07:30","2026-07-10 09:00","SCHEDULED","NORMAL","fn-cristiano","Visita preventiva de julho."),
]
for k,ck,ctk,osk,tit,tipo,rec,dini,dfim,status,prio,respf,notes in AG:
    ctid = f"{q(uid(ctk))}" if ctk else "NULL"
    osid = f"{q(uid(osk))}" if osk else "NULL"
    emit("INSERT INTO agenda_eventos (id,cliente_id,contrato_id,ordem_servico_id,titulo,descricao,tipo_evento,recorrencia,data_inicio,data_fim,dia_inteiro,status,prioridade,responsavel_id,lembrete_ativo,dias_antecedencia_lembrete) VALUES "
         f"({q(uid(k))},{q(uid(ck))},{ctid},{osid},{q(tit)},{q(notes)},{q(tipo)},{q(rec)},{q(dini)},{q(dfim)},false,{q(status)},{q(prio)},{q(uid(respf))},true,1) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 9: certificados
emit("-- ===== BATCH 09: CERTIFICADOS =====")
CERT = [
 ("cr-maita","cli-maita","os-maita-1","Certificado de controle de pragas","2026-05-12","2026-11-12"),
 ("cr-rest","cli-restaurante","os-rest-1","Certificado sanitário de controle de pragas","2026-06-12","2026-12-12"),
 ("cr-padaria","cli-padaria","os-padaria-1","Certificado de controle de pragas","2026-06-10","2026-12-10"),
]
for k,ck,osk,desc,emi,val in CERT:
    emit("INSERT INTO certificados (id,cliente_id,ordem_servico_id,descricao,data_emissao,data_validade,intervalo_meses,alerta_ativo,dias_alerta,status) VALUES "
         f"({q(uid(k))},{q(uid(ck))},{q(uid(osk))},{q(desc)},{q(emi)},{q(val)},6,true,15,'VALIDO') ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 10: estoque movimentacoes
emit("-- ===== BATCH 10: ESTOQUE MOVIMENTACOES =====")
# key, produto_key, tipo(INBOUND/OUTBOUND), qtd, vu, data, func_key|None, cliente_key|None, os_key|None, quem_pegou, quem_comprou, destino, obs
MOV = [
 ("mv-in-inset","pr-inset","INBOUND",10,120.00,"2026-05-02 10:00",None,None,None,"","Patrícia Webber","Estoque central","Compra de inseticida concentrado."),
 ("mv-in-ratic","pr-ratic","INBOUND",8,45.00,"2026-05-02 10:10",None,None,None,"","Patrícia Webber","Estoque central","Compra de raticida em blocos."),
 ("mv-in-gel","pr-gel","INBOUND",12,38.00,"2026-05-15 09:00",None,None,None,"","Patrícia Webber","Estoque central","Compra de gel para baratas."),
 ("mv-out-maita","pr-inset","OUTBOUND",2,120.00,"2026-05-12 08:30","fn-anderson","cli-maita","os-maita-1","Anderson Refatti","","Cliente Maitá Palace","Retirada para desratização."),
 ("mv-out-prix","pr-inset","OUTBOUND",2,120.00,"2026-05-20 13:30","fn-cristiano","cli-prix","os-prix-1","Cristiano Bavaresco","","Cliente Prix Hotel","Retirada para controle preventivo."),
 ("mv-out-notre","pr-gel","OUTBOUND",3,38.00,"2026-06-07 07:30","fn-anderson","cli-notredame","os-notre-1","Anderson Refatti","","Colégio Notre Dame","Gel para controle de baratas."),
 ("mv-out-padaria","pr-gel","OUTBOUND",2,38.00,"2026-06-10 07:00","fn-cristiano","cli-padaria","os-padaria-1","Cristiano Bavaresco","","Padaria Pão da Casa","Gel em pontos críticos."),
 ("mv-out-rest","pr-ratic","OUTBOUND",1,45.00,"2026-06-12 14:30","fn-anderson","cli-restaurante","os-rest-1","Anderson Refatti","","Restaurante Sabor da Serra","Iscas para roedores."),
]
for k,pk,tipo,qtd,vu,data,fk,ck,osk,quem_pegou,quem_comprou,dest,obs in MOV:
    fid = f"{q(uid(fk))}" if fk else "NULL"
    cid = f"{q(uid(ck))}" if ck else "NULL"
    oid = f"{q(uid(osk))}" if osk else "NULL"
    total = round(qtd*vu,2)
    emit("INSERT INTO estoque_movimentacoes (id,produto_id,tipo_movimentacao,quantidade,valor_unitario,valor_total,usuario_id,funcionario_id,cliente_id,ordem_servico_id,quem_pegou,quem_comprou,destino_descricao,observacoes,data_movimentacao) VALUES "
         f"({q(uid(k))},{q(uid(pk))},{q(tipo)},{qtd},{vu},{total},{q(ADMIN_USER)},{fid},{cid},{oid},{q(quem_pegou)},{q(quem_comprou)},{q(dest)},{q(obs)},{q(data)}) ON CONFLICT (id) DO NOTHING;")
emit()

# ---------------------------------------------------------------- BATCH 11: financeiro
emit("-- ===== BATCH 11: FINANCEIRO (lancamentos + parcelas) =====")
# ENTRADAS de servicos concluidos (PAID)
# key, tipo, cat_key, forma_key, descricao, cliente_key|None, os_key|None, valor, emissao, venc, pagamento|None, status, parcelado, qtd_parcelas, obs
LANC = [
 ("fi-ent-maita","ENTRY","fcat-servicos","fpag-pix","Serviço de desratização - Maitá Palace (maio)","cli-maita","os-maita-1",650.00,"2026-05-12","2026-05-12","2026-05-12","PAID",False,1,"Recebido via PIX."),
 ("fi-ent-prix","ENTRY","fcat-servicos","fpag-transf","Controle preventivo - Prix Hotel (maio)","cli-prix","os-prix-1",550.00,"2026-05-20","2026-05-30","2026-05-28","PAID",False,1,"Recebido via transferência."),
 ("fi-ent-notre","ENTRY","fcat-servicos","fpag-boleto","Controle escolar - Notre Dame (junho)","cli-notredame","os-notre-1",700.00,"2026-06-07","2026-06-17","2026-06-16","PAID",False,1,"Boleto quitado."),
 ("fi-ent-padaria","ENTRY","fcat-servicos","fpag-pix","Controle de baratas - Pão da Casa (junho)","cli-padaria","os-padaria-1",280.00,"2026-06-10","2026-06-10","2026-06-10","PAID",False,1,"Recebido via PIX."),
 ("fi-ent-maria","ENTRY","fcat-servicos","fpag-dinheiro","Dedetização residencial - Maria A.","cli-maria","os-maria-1",190.00,"2026-06-05","2026-06-05","2026-06-05","PAID",False,1,"Recebido em dinheiro."),
 # conta a receber (PENDING) - servico concluido nao pago
 ("fi-ent-rest","ENTRY","fcat-servicos","fpag-boleto","Controle quinzenal - Sabor da Serra (1a junho)","cli-restaurante","os-rest-1",320.00,"2026-06-12","2026-06-22",None,"OVERDUE",False,1,"Conta a receber vencida."),
 # contrato parcelado (PENDING) - 3x, 1 paga 2 a vencer
 ("fi-ent-maita-tri","ENTRY","fcat-contratos","fpag-boleto","Contrato trimestral antecipado - Maitá Palace","cli-maita",None,1950.00,"2026-06-01","2026-06-10","2026-06-09","PARTIAL",True,3,"3 parcelas mensais; 1 paga, 2 a vencer."),
 # DESPESAS
 ("fi-exp-prod1","EXPENSE","fcat-produtos","fpag-pix","Compra de inseticida e raticida","",None,1560.00,"2026-05-02","2026-05-02","2026-05-02","PAID",False,1,"Reposição de estoque."),
 ("fi-exp-prod2","EXPENSE","fcat-produtos","fpag-boleto","Compra de gel para baratas e EPIs","",None,890.00,"2026-06-15","2026-07-05",None,"PENDING",False,1,"Conta a pagar - boleto fornecedor."),
 ("fi-exp-comb","EXPENSE","fcat-combustivel","fpag-cartao","Combustível das rotas de atendimento","",None,420.00,"2026-06-01","2026-06-01","2026-06-01","PAID",False,1,"Abastecimento da frota."),
 ("fi-exp-sal","EXPENSE","fcat-salarios","fpag-transf","Salários da equipe técnica (junho)","",None,7600.00,"2026-06-05","2026-07-05",None,"PENDING",False,1,"Folha a pagar - 2 técnicos + auxiliar."),
 ("fi-exp-alim","EXPENSE","fcat-alimentacao","fpag-dinheiro","Refeições da equipe em rota","",None,240.00,"2026-06-10","2026-06-10","2026-06-10","PAID",False,1,"Alimentação durante atendimentos."),
 ("fi-exp-manut","EXPENSE","fcat-extras","fpag-pix","Manutenção da bomba costal motorizada","",None,180.00,"2026-06-03","2026-06-03","2026-06-03","PAID",False,1,"Troca de vedação e revisão."),
]
for row in LANC:
    k,tipo,catk,fpagk,desc,ck,osk,valor,emi,venc,pag,status,parc,qp,obs = row
    cid = f"{q(uid(ck))}" if ck else "NULL"
    oid = f"{q(uid(osk))}" if osk else "NULL"
    pagv = q(pag) if pag else "NULL"
    emit("INSERT INTO financeiro_lancamentos (id,tipo,categoria_id,forma_pagamento_id,descricao,cliente_id,ordem_servico_id,valor_total,data_emissao,data_vencimento,data_pagamento,status,parcelado,quantidade_parcelas,observacoes,criado_por) VALUES "
         f"({q(uid(k))},{q(tipo)},{q(uid(catk))},{q(uid(fpagk))},{q(desc)},{cid},{oid},{valor},{q(emi)},{q(venc)},{pagv},{q(status)},{str(parc).lower()},{qp},{q(obs)},{q(ADMIN_USER)}) ON CONFLICT (id) DO NOTHING;")
# parcelas do lancamento parcelado fi-ent-maita-tri
parc = [
 ("pa-maita-1","fi-ent-maita-tri",1,650.00,"2026-06-10","2026-06-09","PAID"),
 ("pa-maita-2","fi-ent-maita-tri",2,650.00,"2026-07-10",None,"PENDING"),
 ("pa-maita-3","fi-ent-maita-tri",3,650.00,"2026-08-10",None,"PENDING"),
]
for k,lk,num,valor,venc,pag,status in parc:
    pagv = q(pag) if pag else "NULL"
    emit("INSERT INTO financeiro_parcelas (id,lancamento_id,numero_parcela,valor_parcela,data_vencimento,data_pagamento,status) VALUES "
         f"({q(uid(k))},{q(uid(lk))},{num},{valor},{q(venc)},{pagv},{q(status)}) ON CONFLICT (id) DO NOTHING;")
emit()

sql = "\n".join(out)
with open(OUT, "w", encoding="utf-8") as f:
    f.write(sql)

print("OK -> ", OUT)
print("Linhas SQL:", len(out))
print("Leads lidos:", len(leads))
print("Convertidos:", list(CONVERT.keys()))
# pré-visualização de contagens
import re
counts = {}
for line in out:
    m = re.match(r"INSERT INTO (\w+)", line)
    if m:
        counts[m.group(1)] = counts.get(m.group(1),0)+1
for t,c in counts.items():
    print(f"  {t}: {c}")
