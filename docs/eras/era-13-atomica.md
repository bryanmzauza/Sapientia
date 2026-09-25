# Era 13 — Era Atômica

**Inspiração:** 1942–1970 · **Versão:** 2.13.x · **Status:** planejada

## Resumo

O átomo vira fonte de energia. O urânio passa por lixiviação, fluoretação e enriquecimento até
virar barra de combustível; o reator de fissão, moderado por água pesada e controlado por barras
de háfnio, gera vapor para a turbina EV. A radiação exige blindagem, traje e cuidado com o
combustível usado.

## Desbloqueio

- Requer a era 12 e o Processador T1.
- **Item-porta:** Barra de combustível.

## Energia: EV

Transformador HV→EV, cabos e capacitores EV. A turbina a vapor de alta pressão é o gerador da era.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Uraninita | urânio | tório | ardósia profunda, granito (abaixo de Y −30) | diamante |
| Torianita | tório | urânio | areia de praia, granito | diamante |
| Zircão | zircônio | háfnio | areia, granito | diamante |

Rádio e háfnio passam a ser separados.

## Ciclo do combustível

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Yellowcake | `yellowcake` | material | Tanque de lixiviação: pó de uraninita + ácido sulfúrico. | Hexafluoreto de urânio. |
| Hexafluoreto de urânio | `uranium_hexafluoride` | gás | Reator químico: yellowcake + ácido fluorídrico. | Enriquecimento. |
| Centrífuga de gás | `gas_centrifuge` | máquina EV | Bancada: 1 carcaça HV, 2 motores T2, 1 invar, 1 processador T1. | Hexafluoreto → urânio enriquecido + urânio empobrecido. Em cascata, várias centrífugas encostadas enriquecem mais rápido. |
| Pastilha de urânio | `uranium_pellet` | material | Compressor: urânio enriquecido. | Barra de combustível. |
| Zircaloy | `zircaloy_ingot` | material | Forno de indução: 49 zircônio + 1 estanho → 50 (proporção simplificada 9:1). | Revestimento da barra. |
| Barra de combustível | `fuel_rod` | componente | Bancada: 6 pastilhas de urânio em tubo de zircaloy (3 placas de zircaloy). | Combustível do reator; dura 2 dias de jogo. Item-porta. |
| Barra de controle | `control_rod` | componente | Bancada: 4 lingotes de háfnio + 2 placas de aço inox. | Regula a potência do reator. |
| Água pesada | `heavy_water` | fluido | Coluna de destilação isotópica: água destilada → água pesada (1 a cada 1.000 mB). | Moderador do reator; deutério (era 18). |
| Coluna de destilação isotópica | `isotope_column` | multibloco (1×1×8) | Controlador — Bancada: 1 carcaça HV, 4 tanques, 1 processador T1. | Produz água pesada. |
| Fonte de nêutrons | `neutron_source` | componente | Bancada: 1 rádio + 4 berílios. | Partida do reator. |

## Reator e energia

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Reator de fissão | `fission_reactor_controller` | Multibloco 5×5×5 de blindagem de reator + controlador (Bancada: 1 carcaça HV, 4 processadores T1, 1 fonte de nêutrons). | Barras de combustível + moderador + refrigerante → vapor de alta pressão. Potência ajustada pelas barras de controle. Sem refrigerante, aquece e entra em colapso. |
| Blindagem de reator | `reactor_casing` | Bancada: 4 concretos armados + 4 lingotes de chumbo → 8. | Estrutura do reator. |
| Turbina a vapor | `steam_turbine` | Bancada: 1 carcaça HV, 8 placas de aço inox, 2 bobinas T2. | Vapor de alta pressão → SU (EV). |
| Carcaça EV | `machine_casing_ev` | Bancada: 1 carcaça HV, 4 placas de zircaloy, 2 bobinas T2, 1 processador T1. | Base das máquinas das eras 15 e 16. |
| Transformador HV→EV, cabo EV, capacitor EV | `transformer_hv_ev`, `cable_t4`, `capacitor_t4` | Bancada: bobinas T2, fios de cobre-berílio, capacitores HV. | Rede EV. |
| Unidade de reprocessamento | `reprocessing_unit` | Bancada: 1 carcaça HV, 1 tanque de lixiviação, 4 blocos de chumbo. | Combustível usado → urânio recuperado + plutônio + resíduo radioativo. |
| Plutônio | `plutonium` | material | Reprocessamento. | Cápsulas de RTG (era 14); combustível avançado. |
| Barril de resíduo | `waste_barrel` | bloco | Bancada: 7 placas de chumbo + 2 concretos armados. | Guarda resíduo radioativo sem vazar radiação, se enterrado abaixo de Y 0. |

## Radiação e proteção

- Combustível usado, resíduo solto e reatores sem blindagem emitem **radiação** num raio pequeno.
- A exposição acumula uma **dose** que decai com o tempo; doses altas dão fraqueza, náusea e dano.
- A dose é calculada por evento (ao entrar e sair das zonas), não por tick.

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Traje antirradiação | `hazmat_helmet`, `hazmat_chestplate`, `hazmat_leggings`, `hazmat_boots` | Bancada: tecido revestido de chumbo + borracha sintética + vidro (capacete). | Com o conjunto completo, a dose não acumula. |
| Tecido revestido de chumbo | `lead_lined_cloth` | Bancada: 4 tecidos de nylon + 1 placa de chumbo → 4. | Traje antirradiação. |
| Contador Geiger | `geiger_counter` | Bancada: 1 processador T1, 1 tubo de vidro com gás (argônio), 1 bateria. | Mostra a radiação no local e a dose acumulada. |
| Iodeto de potássio | `potassium_iodide` | Misturador: 1 potássio + 1 tintura de iodo → 4. | Reduz a dose acumulada pela metade. |

## Máquinas

| Máquina | Consumo | Tempo | Limite por chunk |
|---------|---------|-------|------------------|
| Centrífuga de gás | 512 SU/t | 30 s | 16 |
| Coluna de destilação isotópica | 256 SU/t | contínuo | 2 |
| Reator de fissão | produz vapor para até 8 turbinas | contínuo | 1 |
| Turbina a vapor | produz até 2.048 SU/t | contínuo | 8 |
| Reprocessamento | 512 SU/t | 60 s | 2 |

## Tarefas

- [ ] E13.1 Uraninita, torianita, zircão; separação de rádio e háfnio.
- [ ] E13.2 Yellowcake, hexafluoreto, centrífuga de gás em cascata, pastilhas, zircaloy, barras.
- [ ] E13.3 Água pesada e coluna isotópica; fonte de nêutrons.
- [ ] E13.4 Reator de fissão (multibloco) com controle, refrigeração e colapso.
- [ ] E13.5 Turbina a vapor e rede EV.
- [ ] E13.6 Reprocessamento, plutônio e resíduo.
- [ ] E13.7 Radiação por evento, dose, traje, contador Geiger, iodeto de potássio.
- [ ] E13.8 Texturas.
