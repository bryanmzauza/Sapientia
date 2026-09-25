# Era 8 — Revolução Industrial

**Inspiração:** 1760–1860 · **Versão:** 2.8.x · **Status:** planejada

## Resumo

O vapor cria a primeira rede de energia. O coque alimenta caldeiras e fornos; o martelo a vapor e o
forno de reverbero processam minério em escala; o conversor Bessemer faz aço. A perfuratriz a vapor
começa a transformar a paisagem. No campo, semeadora e colheitadeira; na indústria têxtil, o tear
mecânico; na química, ácido sulfúrico, soda, borracha vulcanizada, papel e conservas.

## Desbloqueio

- Requer a era 7 e o Mecanismo de precisão.
- **Item-porta:** Caldeira.

## Energia: vapor

- A **caldeira** queima combustível sólido e transforma água em vapor: 100 mB de água → 1.000 mB
  de vapor. 1 carvão rende 1.600 mB de vapor.
- **Canos de vapor** formam redes (sobre o sistema de fluidos atual). Máquinas a vapor consomem
  vapor por tick de trabalho.
- A **máquina a vapor** converte vapor em UM para as máquinas mecânicas das eras 5 a 7.
- O **condensador** devolve 90 % da água do vapor usado.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Pentlandita | níquel | ferro, cobalto | ardósia profunda (Y −60 a 0) | ferro |
| Cromita | cromo | ferro | ardósia profunda, basalto (Y −64 a −20) | ferro |
| Volframita | tungstênio | ferro, manganês | granito (Y −30 a 20) | ferro |
| Molibdenita | molibdênio | — | granito, ardósia profunda (Y −40 a 10) | ferro |
| Ilmenita | titânio (rejeito até a era 14) | ferro | areia de praia, pedra | ferro |
| Fosforita (não metálico) | fosfato | — | calcita, pedra | ferro |
| Grafite (não metálico) | grafite | — | ardósia profunda | ferro |

## Vapor e combustível

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Forno de coque | `coke_oven` | multibloco (3×3×3 de tijolos refratários + controlador) | Controlador — Bancada: 4 tijolos refratários, 4 lingotes de ferro, 1 alto-forno. | 1 carvão → 1 coque + 250 mB de alcatrão em 30 s. |
| Coque | `coke` | combustível | Forno de coque. | 2× a energia do carvão em caldeiras; redutor em fornos. |
| Alcatrão | `coal_tar` | fluido | Forno de coque. | Retorta → creosoto, óleo leve, piche. |
| Caldeira | `boiler` | máquina | Bancada: 5 placas de ferro fundido, 2 canos de vapor, 1 forno de argila, 1 mecanismo de precisão. | Água + combustível → vapor. Item-porta. |
| Cano de vapor | `steam_pipe` | bloco (passivo) | Bancada: 3 ferros fundidos → 6. | Leva vapor e água. |
| Máquina a vapor | `steam_engine` | gerador (20 mB/t de vapor → 8 UM) | Bancada: 4 placas de ferro, 2 engrenagens de bronze, 1 mecanismo de precisão, 1 cano de vapor. | Move máquinas mecânicas. |
| Condensador | `condenser` | máquina | Bancada: 4 placas de cobre, 2 canos de vapor, 1 cisterna. | Vapor usado → 90 % da água de volta. |
| Bomba a vapor | `steam_pump` | máquina (10 mB/t de vapor) | Bancada: 4 placas de ferro, 2 canos de vapor, 1 mecanismo de precisão. | Tira 100 mB/t de água de uma fonte. |
| Tanque de água | `fluid_tank` | bloco | Bancada: 4 placas de ferro, 4 vidros, 1 barril alcatroado. | Guarda 16.000 mB de um fluido. |

## Metalurgia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Martelo a vapor | `steam_hammer` | máquina (10 mB/t) | Bancada: 4 blocos de ferro, 1 máquina a vapor, 1 martelo de ferro. | Fragmento → 1,75 triturado. |
| Forno de reverbero | `reverberatory_furnace` | máquina (combustível) | Bancada: 8 tijolos refratários, 1 forno de argila, 1 cano de vapor. | 8 triturados ou lavados → 8 lingotes em 40 s, com 45 % de secundários. Funde níquel, cromo, tungstênio e molibdênio (em pó). |
| Conversor Bessemer | `bessemer_converter` | máquina (vapor) | Bancada: 6 placas de ferro fundido, 2 canos de vapor, 1 mecanismo de precisão. | 4 ferros fundidos + 200 mB de vapor → 4 lingotes de aço-carbono. |
| Lingote de aço-carbono | `steel_ingot` | material | Conversor Bessemer. | Carcaças, trilhos, máquinas elétricas (era 9). |
| Pós de níquel, cromo, tungstênio e molibdênio | `nickel_dust`, `chromium_dust`, `tungsten_dust`, `molybdenum_dust` | material | Forno de reverbero. | Ligas da era 10; constantan e filamentos (era 9). |
| Folha de flandres | `tinplate` | material | Bancada: 1 placa de aço-carbono + 1 lingote de estanho → 2. | Latas de conserva. |
| Aço galvanizado | `galvanized_steel` | material | Bancada: 1 placa de aço-carbono + 1 lingote de zinco → 2. | Chapas que não enferrujam: telhados, dutos. |

## Mineração

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Perfuratriz a vapor | `steam_drill` | máquina (30 mB/t) | Bancada: 4 placas de aço-carbono, 1 máquina a vapor, 1 martelo de mineração, 2 canos de vapor. | Cava um túnel 2×3 à frente, 1 bloco a cada 2 s, até 64 blocos; manda os drops para um baú encostado. Aplica as regras de terreno virgem. |

## Agricultura e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Semeadora | `seed_drill` | máquina (5 mB/t) | Bancada: 4 placas de ferro, 1 máquina a vapor, 1 funil, 1 arado de ferro. | Quando um bloco de terra arada fica vazio num raio 9×9, planta uma semente tirada do baú encostado. |
| Colheitadeira a vapor | `steam_harvester` | máquina (10 mB/t) | Bancada: 4 placas de aço-carbono, 1 máquina a vapor, 1 foice de bronze, 1 baú. | Quando uma planta amadurece num raio 9×9, colhe e manda para o baú; a semeadora replanta. Reage ao amadurecimento, sem varrer a plantação. |
| Descaroçador de algodão | `cotton_gin` | máquina (2 UM) | Bancada: 4 tábuas, 2 engrenagens de bronze, 1 mecanismo de precisão. | 1 capulho → 2 fibras de algodão + 1 semente. |
| Superfosfato | `superphosphate` | material | Bancada: 2 fosforitas trituradas + 1 ácido sulfúrico → 4. | Aplicado numa planta: +50 % de velocidade num raio 3×3 por 1 dia de jogo. |
| Usina de açúcar | `sugar_mill` | máquina (2 UM) | Bancada: 4 placas de ferro, 1 moinho de grãos, 1 caldeirão. | 1 beterraba → 2 açúcares; 1 cana → 2 açúcares. |
| Lata de conserva | `canned_food` | comida | Autoclave: 1 comida + 1 folha de flandres. | Sacia 2× a comida original; empilha até 64. |

## Fibras e têxteis

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Fiadora | `spinning_jenny` | máquina (2 UM) | Bancada: 4 tábuas, 4 engrenagens de bronze, 1 mecanismo de precisão. | 8 fibras (linho ou algodão) → 8 fios. |
| Tear mecânico | `power_loom` | máquina (4 UM) | Bancada: 4 placas de ferro, 1 tear de pedal, 1 mecanismo de precisão. | 4 fios → 4 tecidos. |
| Tecido de algodão | `cotton_cloth` | material | Tear mecânico. | Filtros (era 9), correias, isolamento de cabos. |

## Madeira, papel e química

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Retorta | `wood_retort` | máquina (combustível) | Bancada: 6 placas de ferro fundido, 1 alambique, 1 cano de vapor. | 8 toras → 8 carvões vegetais + 250 mB de metanol + 250 mB de ácido acético + 100 mB de creosoto. Também destila alcatrão. |
| Creosoto | `creosote` | fluido | Retorta. | Madeira tratada. |
| Madeira tratada | `treated_planks` | bloco | Bancada: 8 tábuas + 1 balde de creosoto → 8. | Não queima nem apodrece; postes e dormentes. |
| Fábrica de papel | `paper_mill` | máquina (vapor) | Bancada: 4 placas de ferro, 1 serraria hidráulica, 1 tanque de água. | 4 serragens + 1 soda + água → 8 papéis; papel → papelão. |
| Papelão | `cardboard` | material | Fábrica de papel. | Caixas, embalagens (embalador, era 11). |
| Câmara de chumbo | `lead_chamber` | máquina (vapor) | Bancada: 8 lingotes de chumbo + 1 cano de vapor. | 1 enxofre + vapor → 250 mB de ácido sulfúrico, contínuo. |
| Ácido sulfúrico | `sulfuric_acid` | fluido | Câmara de chumbo. | Superfosfato, soda, flotação, baterias. |
| Soda | `soda_ash` | material | Forno de reverbero: 2 sais + 1 balde de ácido sulfúrico + 1 calcita + 1 carvão → 2 (processo Leblanc). | Papel, vidro plano, sabão industrial. |
| Autoclave | `autoclave` | máquina (vapor) | Bancada: 6 placas de aço-carbono, 1 caldeirão, 2 canos de vapor. | Vulcaniza borracha e esteriliza latas de conserva. |
| Borracha | `rubber` | material | Autoclave: 4 látex + 1 enxofre → 4. | Correias, vedações, isolamento de cabos (era 9). |
| Correia de borracha | `rubber_belt` | bloco (passivo) | Bancada: 3 borrachas + 2 tecidos de algodão → 4. | Leva UM entre duas polias a até 16 blocos, em qualquer direção. |
| Cimento Portland | `portland_cement` | material | Forno de cal: 3 calcitas + 1 argila. | Concreto armado. |
| Concreto armado | `reinforced_concrete` | bloco | Bancada: 4 cimentos + 4 areias + 1 vara de aço-carbono + água → 8. | Bloco estrutural muito resistente. |

## Máquinas

| Máquina | Consumo | Tempo | Limite por chunk |
|---------|---------|-------|------------------|
| Caldeira | combustível + 100 mB de água → 1.000 mB de vapor | contínuo | 8 |
| Máquina a vapor | 20 mB/t → 8 UM | contínuo | 8 |
| Martelo a vapor | 10 mB/t | 6 s por fragmento | 16 |
| Forno de reverbero | combustível | 40 s por lote de 8 | 8 |
| Conversor Bessemer | 200 mB por lote | 30 s | 4 |
| Perfuratriz a vapor | 30 mB/t | 2 s por bloco | 2 |
| Semeadora, colheitadeira | 5 e 10 mB/t quando agem | por evento | 8 cada |
| Fiadora, tear mecânico, descaroçador | 2 a 4 UM | 5 s | 16 cada |
| Forno de coque | — | 30 s | 4 |
| Retorta, câmara de chumbo, autoclave, fábrica de papel | vapor ou combustível | 20 a 60 s | 4 cada |

## Conteúdo atual reaproveitado

- **Caldeira** e **condensador** (hoje na cadeia HV de gases) passam a ser vapor.
- **Cano, bomba, tanque e dreno de fluidos** entram aqui como infraestrutura de água e vapor.
- **Níquel** (`nickel_*`).

## Tarefas

- [ ] E8.1 Vapor como fluido; caldeira, cano de vapor, máquina a vapor, condensador, bomba a vapor.
- [ ] E8.2 Forno de coque, coque, alcatrão.
- [ ] E8.3 Martelo a vapor, forno de reverbero, conversor Bessemer, aço-carbono, folha de flandres, aço galvanizado.
- [ ] E8.4 Minerais da era e pós de níquel, cromo, tungstênio e molibdênio.
- [ ] E8.5 Perfuratriz a vapor com regras de terreno virgem.
- [ ] E8.6 Semeadora e colheitadeira por evento; descaroçador; superfosfato; usina de açúcar; latas de conserva.
- [ ] E8.7 Fiadora, tear mecânico, tecido de algodão.
- [ ] E8.8 Retorta, creosoto, madeira tratada; fábrica de papel e papelão.
- [ ] E8.9 Câmara de chumbo, ácido sulfúrico, soda, autoclave, borracha, correia, cimento e concreto armado.
- [ ] E8.10 Reencaixe de caldeira, condensador e fluidos atuais.
- [ ] E8.11 Texturas.
