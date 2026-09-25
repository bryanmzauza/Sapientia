# Era 9 — Eletricidade

**Inspiração:** 1870–1900 · **Versão:** 2.9.x · **Status:** planejada

## Resumo

A rede elétrica do Sapientia começa aqui. O dínamo converte vapor e força mecânica em SU; o
gerador queima combustível; cabos isolados com borracha levam a energia às primeiras máquinas
elétricas, que dobram o rendimento do minério. A eletrólise refina o cobre, produz alumínio e abre
a química do cloro e da soda cáustica. A água passa a ser tratada e a comida, refrigerada.

## Desbloqueio

- Requer a era 8 e a Caldeira.
- **Item-porta:** Carcaça de máquina.

## Energia: elétrica LV

- **Dínamo:** 8 UM, ou 20 mB/t de vapor → 16 SU/t.
- **Gerador:** queima combustível sólido e só produz enquanto tem combustível (4 SU/t; 1 carvão
  dura 80 s).
- **Cabos LV** e **capacitores** formam redes, resolvidas por rede e não por bloco.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Bauxita | alumínio | ferro, titânio | terracota, argila, areia vermelha (superfície) | ferro |
| Magnesita | magnésio | — | calcita, pedra (Y 0 a 60) | ferro |

O paládio passa a ser separado (lama anódica e rejeitos de platina).

## Energia e componentes

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Fieira | `draw_plate` | ferramenta de bancada (256 usos) | Bancada: 2 placas de aço-carbono + martelo. | 1 lingote → 2 fios. |
| Fio de cobre | `copper_wire` | material | Fieira. | Cabos e bobinas. |
| Cabo LV | `cable` | bloco (passivo) | Bancada: 3 fios de cobre + 2 borrachas + 1 tecido de algodão → 8. | Leva SU na rede LV. |
| Dínamo | `dynamo` | gerador | Bancada: 4 placas de aço-carbono, 2 bobinas T1, 1 mecanismo de precisão, 1 eixo de madeira. | 8 UM ou vapor → 16 SU/t. |
| Gerador | `generator` | gerador | Bancada: 1 carcaça de máquina + 1 fornalha + 2 bobinas T1. | Combustível → 4 SU/t. |
| Capacitor LV | `capacitor` | bloco | Bancada: 4 placas de chumbo, 2 baldes de ácido sulfúrico, 2 placas de cobre, 1 carcaça de máquina. | Guarda 40.000 SU (bateria de chumbo-ácido estacionária). |
| Lâmpada elétrica | `consumer` | bloco | Bancada: 1 vidro + 1 filamento de tungstênio + 1 fio de cobre. | Ilumina (luz 15) consumindo 1 SU/t. |
| Filamento de tungstênio | `tungsten_filament` | material | Fieira: pó de tungstênio fundido no forno elétrico. | Lâmpadas, válvulas (era 11). |
| Solda | `solder` | material | Mistura: 1 estanho + 1 chumbo; forno. | Circuitos e bobinas. |
| Constantan | `constantan_ingot` | material | Mistura: 1 cobre + 1 níquel; forno elétrico. | Resistores, termopares. |
| Bobina T1 | `coil_t1` | componente | Bancada: 8 fios de cobre em volta de 1 vara de ferro. | Motores, dínamos, transformadores. |
| Motor T1 | `motor_t1` | componente | Bancada: 2 bobinas T1, 1 fragmento de magnetita (ímã natural), 1 vara de aço-carbono, 2 placas de aço-carbono. | Máquinas elétricas. |
| Carcaça de máquina | `machine_casing` | bloco e componente | Bancada: 4 placas de aço-carbono, 2 fios de cobre, 1 mecanismo de precisão, 1 solda, 1 bloco de redstone. | Base de todas as máquinas elétricas. Item-porta. |
| Bateria de chumbo-ácido | `lead_acid_battery` | item | Bancada: 3 placas de chumbo, 1 balde de ácido sulfúrico, 1 placa de cobre. | Guarda 20.000 SU para ferramentas. |
| Carregador | `charger` | máquina | Bancada: 1 carcaça de máquina, 2 fios de cobre, 1 capacitor LV. | Carrega baterias e ferramentas elétricas. |
| Chave inglesa | `wrench` | ferramenta | Bancada: 3 placas de aço-carbono + 1 vara de aço-carbono. | Gira e recolhe máquinas; mostra energia e estado ao mirar. |
| Console | `console` | bloco | Bancada: 1 carcaça de máquina, 1 lâmpada elétrica, 4 fios de cobre. | Mostra geração, consumo e armazenamento da rede. |

## Máquinas elétricas

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Macerador | `macerator` | Bancada: 1 carcaça, 1 motor T1, 2 martelos de ferro, 2 placas de aço-carbono. | Fragmento ou triturado → 2 pós. |
| Lavador de minério | `ore_washer` | Bancada: 1 carcaça, 1 motor T1, 1 calha de lavagem, 1 cano de chumbo. | Pó + água → pó limpo, 50 % de secundários e 10 % de traços. |
| Forno elétrico | `electric_furnace` | Bancada: 1 carcaça, 4 filamentos de tungstênio, 4 tijolos refratários. | Pó ou lavado → lingote. |
| Serra de bancada | `bench_saw` | Bancada: 1 carcaça, 1 motor T1, 1 serra manual. | Tora → 9 tábuas + serragem; lingote → 2 varas. |
| Célula eletrolítica | `electrolytic_cell` | Bancada: 1 carcaça, 4 placas de chumbo, 2 grafites, 1 tanque de água. | Refino de cobre (cobre + lama anódica com ouro, prata e paládio); alumina → alumínio; salmoura → cloro + soda cáustica + hidrogênio; magnesita → magnésio. |
| Digestor Bayer | `bayer_digester` | Bancada: 1 carcaça, 1 tanque de água, 4 placas de aço-carbono. | Pó de bauxita + soda cáustica → alumina (ferro e titânio vão para o rejeito). |
| Filtro de água | `water_filter` | Bancada: 1 carcaça, 2 tecidos de algodão, 2 carvões vegetais, 2 areias. | Água → água tratada (consome o filtro aos poucos). |
| Bomba elétrica | `electric_pump` | Bancada: 1 carcaça, 1 motor T1, 1 bomba a vapor. | Tira água de uma fonte e alimenta canos e saídas de irrigação. |
| Câmara frigorífica | `cold_storage` | Bancada: 1 carcaça, 1 motor T1, 4 blocos de gelo, 1 baú. | Baú refrigerado: comidas guardadas viram congeladas. |
| Broca elétrica | `electric_drill` | Bancada: 1 motor T1, 1 bateria de chumbo-ácido, 1 martelo de mineração. | Ferramenta que quebra 3×3 com velocidade de picareta de diamante; consome SU. Aplica as regras de terreno virgem. |

## Logística básica

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Cabo de itens | `item_cable` | Bancada: 3 placas de aço galvanizado + 1 vidro → 8. | Leva itens entre contêineres na rede de itens. |
| Produtor de itens | `item_producer` | Bancada: 1 carcaça, 1 liberador, 1 motor T1. | Tira itens de um contêiner e põe na rede. |
| Consumidor de itens | `item_consumer` | Bancada: 1 carcaça, 1 funil, 1 motor T1. | Entrega itens da rede num contêiner. |
| Filtro de itens | `item_filter` | Bancada: 1 carcaça, 1 alçapão de ferro, 2 fios de cobre. | Deixa passar só os itens configurados. |

## Química, água e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Cloro | `chlorine` | gás | Célula eletrolítica. | Desinfeta água; titânio (era 14); plásticos (era 11). |
| Soda cáustica | `caustic_soda` | material | Célula eletrolítica. | Processo Bayer; sabão industrial; papel. |
| Hidrogênio | `hydrogen` | gás | Célula eletrolítica. | Amônia (era 10); combustível. |
| Alumina | `alumina` | material | Digestor Bayer. | Célula eletrolítica → alumínio. |
| Lingote de alumínio | `aluminum_ingot` | material | Célula eletrolítica. | Cabos leves, ligas (era 11), aeroespacial (era 14). |
| Lingote de magnésio | `magnesium_ingot` | material | Célula eletrolítica. | Duralumínio (era 11), titânio (era 14). |
| Água tratada | `treated_water` | fluido | Filtro de água (+ cloro para 4× mais filtragem). | Exigida pela célula eletrolítica e pelas máquinas da era 10. |
| Comida congelada | `frozen_<comida>` | comida | Câmara frigorífica. | Recupera 1 coração a mais que a comida original. |
| Tintura de iodo | `iodine_tincture` | item | Bancada: 4 kelps secos + 1 álcool. | Cura envenenamento. |

## Máquinas

| Máquina | Consumo | Tempo | Limite por chunk |
|---------|---------|-------|------------------|
| Dínamo | 8 UM ou 20 mB/t de vapor → 16 SU/t | contínuo | 8 |
| Gerador | combustível → 4 SU/t | contínuo | 16 |
| Macerador | 8 SU/t | 5 s | 32 |
| Lavador de minério | 8 SU/t + 100 mB de água | 5 s | 32 |
| Forno elétrico | 12 SU/t | 5 s | 32 |
| Serra de bancada | 6 SU/t | 3 s | 32 |
| Célula eletrolítica | 32 SU/t | 20 s | 8 |
| Digestor Bayer | 16 SU/t | 15 s | 8 |
| Filtro de água | 4 SU/t | 100 mB/s | 16 |
| Bomba elétrica | 8 SU/t | 200 mB/t | 8 |
| Câmara frigorífica | 2 SU/t | contínuo | 16 |

## Conteúdo atual reaproveitado

Gerador, cabo, capacitor, consumidor (vira lâmpada), console, chave inglesa, carcaça de máquina,
macerador, lavador, forno elétrico, serra de bancada, cabo de itens, produtor, consumidor e filtro
de itens, alumínio, motor T1 e bobina T1. Todos ganham receitas novas e as funções reais descritas
acima (o gerador passa a exigir combustível e o lavador passa a separar secundários).

## Tarefas

- [ ] E9.1 Dínamo e gerador com combustível; rede LV no agendador com orçamento.
- [ ] E9.2 Fieira, fio de cobre, cabo LV isolado, bobina e motor T1, carcaça de máquina.
- [ ] E9.3 Capacitor, lâmpada, bateria, carregador, console, chave inglesa.
- [ ] E9.4 Macerador, lavador (secundários e traços), forno elétrico, serra de bancada.
- [ ] E9.5 Célula eletrolítica (cobre, alumínio, cloro-álcali, magnésio) e digestor Bayer.
- [ ] E9.6 Filtro de água, bomba elétrica, câmara frigorífica, tintura de iodo.
- [ ] E9.7 Broca elétrica com regras de terreno virgem.
- [ ] E9.8 Logística básica com novas receitas.
- [ ] E9.9 Bauxita e magnesita.
- [ ] E9.10 Texturas dos itens novos.
