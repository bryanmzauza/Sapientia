# Era 12 — Eletrônica

**Inspiração:** 1947–1970 · **Versão:** 2.12.x · **Status:** planejada

## Resumo

O silício substitui a válvula. O forno a arco produz silício, o refino por zona o purifica, e a
fotolitografia, dentro de uma sala limpa com água ultrapura, transforma wafers em transistores e
processadores. A rede sobe para HV, com eletrolisador, laminador, cortador a laser e gases
industriais. No campo, a hidroponia com luz de LED planta sem solo.

## Desbloqueio

- Requer a era 11 e o Plástico.
- **Item-porta:** Processador T1.

## Energia: HV

Transformador MV→HV, cabos e capacitores HV.

## Minerais que passam a cair

| Mineral | Produto | Onde cai | Picareta mínima |
|---------|---------|----------|-----------------|
| Fluorita (não metálico) | flúor | granito | ferro |

Passam a ser separados: silício, germânio, gálio, índio e selênio.

## Semicondutores

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Forno a arco | `arc_furnace` | máquina HV | Bancada: 1 carcaça MV, 3 eletrodos de grafite, 4 tijolos refratários. | Quartzo ou areia + coque → silício metalúrgico. |
| Eletrodo de grafite | `graphite_electrode` | componente | Compressor: 4 grafites → 1. | Forno a arco, células eletrolíticas melhores. |
| Refinador por zona | `zone_refiner` | máquina HV | Bancada: 1 carcaça MV, 4 bobinas T1, 1 invar. | Silício ou germânio → grau eletrônico; separa traços com 50 %. |
| Lingote de silício | `silicon_ingot` | material | Refinador por zona. | Wafers. |
| Wafer de silício | `silicon_wafer` | componente | Cortador a laser: 1 lingote de silício → 4 wafers. | Chips. |
| Ácido fluorídrico | `hydrofluoric_acid` | fluido | Reator químico: fluorita + ácido sulfúrico. | Gravação de wafers; hexafluoreto de urânio (era 13). |
| Gases dopantes | `dopant_gas` | gás | Reator químico: arsênio ou fosforita + hidrogênio. | Fotolitografia. |
| Fotorresiste | `photoresist` | fluido | Reator químico: plástico + solvente da refinaria. | Fotolitografia. |
| Deionizador | `deionizer` | máquina HV | Bancada: 1 carcaça MV, 4 plásticos, 1 destilador de água. | Água destilada → água ultrapura. |
| Sala limpa | `clean_room_controller` | multibloco (até 9×9×5 de painéis de aço inox e vidro) | Controlador — Bancada: 1 carcaça MV, 2 filtros de água, 2 motores T1. | A fotolitografia só funciona dentro dela. |
| Fotolitografia | `lithography_machine` | máquina HV | Bancada: 1 carcaça MV, 4 lentes, 1 cortador a laser, 1 circuito T1. | Wafer + fotorresiste + ácido fluorídrico + gás dopante + água ultrapura → chips. |
| Transistor | `transistor` | componente | Fotolitografia: 1 wafer → 16. | Circuitos T2. |
| Circuito integrado | `integrated_circuit` | componente | Fotolitografia: 1 wafer → 4. | Processadores. |
| Processador T1 | `processor_t1` | componente | Bancada: 1 circuito integrado, 4 transistores, 1 placa de plástico, 4 fios de ouro. | Item-porta. |
| Circuito T2, motor T2, bobina T2, processador T2 | `circuit_t2`, `motor_t2`, `coil_t2`, `processor_t2` | componentes | Bancada: versão T1 + transistores, fios de alumínio e aço inox. | Máquinas HV. |
| LED | `led` | componente | Fotolitografia: 1 wafer de gálio-arsênio → 16. | Lâmpadas LED, luz de cultivo, diodo do laser. |

## Energia e máquinas HV

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Carcaça HV | `machine_casing_hv` | Bancada: 1 carcaça MV, 4 placas de aço inox, 2 motores T2, 1 circuito T2. | Base das máquinas das eras 13 e 14. |
| Transformador MV→HV | `transformer_mv_hv` | Bancada: 1 carcaça MV, 8 bobinas T1, 2 placas de aço inox. | Liga redes MV e HV. |
| Cabo HV e capacitor HV | `cable_t3`, `capacitor_t3` | Bancada: fios de cobre de alta pureza, borracha sintética; capacitores MV + carcaça MV. | Rede HV. |
| Eletrolisador | `electrolyzer` | Bancada: 1 carcaça MV, 4 eletrodos de grafite, 1 tanque. | Água → hidrogênio + oxigênio. |
| Laminador | `rolling_mill` | Bancada: 1 carcaça MV, 2 motores T2, 2 blocos de aço rápido. | Lingote → 2 fios ou 2 folhas finas. |
| Cortador a laser | `laser_cutter` | Bancada: 1 carcaça MV, 2 lentes, 1 filamento de tungstênio, 1 circuito T1. | Corta lingotes em wafers e peças de precisão. Com LEDs (melhoria), corta 2× mais rápido. |
| Reator químico | `chemical_reactor` | Bancada: 1 carcaça MV, 1 reator de síntese, 1 circuito T2. | Química de alta pureza (ácido fluorídrico, dopantes, fotorresiste). |
| Liquefator | `liquefier` | Bancada: 1 carcaça MV, 2 compressores de gás, 4 blocos de gelo compactado. | Gases → líquidos (oxigênio e hidrogênio líquidos). |
| Separador de fases | `phase_separator` | Bancada: 1 carcaça MV, 1 centrífuga, 1 tanque. | Separa misturas de líquidos e gases. |
| Coletor atmosférico | `atmospheric_collector` | Bancada: 1 carcaça MV, 1 separador de ar, 1 motor T2. | Versão HV do separador de ar, com argônio e CO₂. |

## Agricultura

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Leito hidropônico | `hydroponic_bed` | Bancada: 4 plásticos, 1 cano, 1 LED. | Planta sem solo, com água e solução nutritiva; cresce 50 % mais rápido. O crescimento é agendado por evento (sem custo contínuo). |
| Solução nutritiva | `nutrient_solution` | Misturador: 1 fertilizante NPK + água destilada. | Alimenta leitos hidropônicos. |
| Luz de cultivo | `grow_light` | Bancada: 4 LEDs, 1 placa de alumínio, 1 fio de cobre. | Permite crescer plantas sem luz do sol num raio 5×5. |

## Máquinas

| Máquina | Consumo | Tempo | Limite por chunk |
|---------|---------|-------|------------------|
| Forno a arco | 256 SU/t | 20 s | 2 |
| Refinador por zona | 128 SU/t | 30 s | 4 |
| Fotolitografia | 256 SU/t | 30 s | 4 |
| Deionizador | 32 SU/t | 100 mB/s | 8 |
| Eletrolisador, laminador, cortador a laser, reator químico | 64 a 192 SU/t | 5 a 20 s | 16 cada |
| Liquefator, separador de fases, coletor | 96 a 128 SU/t | contínuo | 4 cada |
| Leito hidropônico | 2 SU/t | por evento | 64 |

## Conteúdo atual reaproveitado

Silício, wafer, processador T1 e componentes T2, cabo e capacitor HV, transformador MV→HV,
eletrolisador, laminador, cortador a laser, reator químico, liquefator, separador de fases e coletor
atmosférico.

## Tarefas

- [ ] E12.1 Rede HV.
- [ ] E12.2 Forno a arco, eletrodos, refinador por zona, silício.
- [ ] E12.3 Química de alta pureza: ácido fluorídrico, dopantes, fotorresiste, água ultrapura.
- [ ] E12.4 Sala limpa e fotolitografia; transistor, circuito integrado, LED.
- [ ] E12.5 Processador T1 e componentes T2.
- [ ] E12.6 Máquinas HV atuais com receitas novas.
- [ ] E12.7 Hidroponia, solução nutritiva, luz de cultivo.
- [ ] E12.8 Fluorita; separação de germânio, gálio, índio e selênio.
- [ ] E12.9 Texturas.
