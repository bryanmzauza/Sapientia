# Era 11 — Petróleo e Química

**Inspiração:** 1900–1950 · **Versão:** 2.11.x · **Status:** planejada

## Resumo

O petróleo muda a economia: refinaria, combustíveis, plásticos, borracha sintética e nylon. A
agricultura vira fonte de energia com etanol e biodiesel, e a estufa de plástico permite plantar em
qualquer bioma. A penicilina inaugura a farmácia moderna. A sonda rotativa perfura poços até a
rocha-mãe.

## Desbloqueio

- Requer a era 10 e o Lingote de aço inox.
- **Item-porta:** Plástico.

## Energia

MV, com geradores a combustão, a biogás e a turbina a gás.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Berilo | berílio | alumínio | granito (Y −20 a 40) | ferro |
| Espodumênio | lítio | alumínio | granito (Y −30 a 30) | ferro |

O cádmio passa a ser separado da esfalerita.

## Petróleo e energia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Pumpjack | `pumpjack` | máquina MV | Bancada: 1 carcaça MV, 1 bloco de ferro, 2 pistões, 1 motor T1. | Retira petróleo do reservatório finito do chunk. |
| Refinaria | `oil_refinery_controller` | multibloco (5×5×7 de carcaças de aço inox) | Controlador — Bancada: 1 carcaça MV, 4 destiladores de água, 2 tanques. | Petróleo → diesel, gasolina, lubrificante, nafta e resíduo (asfalto). |
| Craqueador | `cracker` | máquina MV | Bancada: 1 carcaça MV, 1 forno de indução, 1 tanque. | Nafta → etileno, propileno, butadieno. |
| Gerador a combustão | `combustion_gen` | gerador MV | Bancada: 1 carcaça MV, 1 gerador, 2 pistões. | Diesel, gasolina, etanol ou biodiesel → SU. |
| Gerador a biogás | `biogas_gen` | gerador LV | Bancada: 1 carcaça de máquina, 1 fermentador, 1 gerador. | Biogás → SU. |
| Turbina a gás | `gas_turbine` | gerador MV | Bancada: 1 carcaça MV, 4 placas de aço inox, 2 motores T1. | Gás natural ou hidrogênio → SU. |
| Sonda rotativa | `drill_rig_controller` | multibloco (5×5×8) | Controlador — Bancada: 1 carcaça MV, 2 brocas de aço rápido, 1 motor T1. | Perfura uma coluna 3×3 até a rocha-mãe e coleta os fragmentos; também encontra bolsões de gás. Aplica as regras de terreno virgem. |
| Extrator de gás | `gas_extractor` | máquina MV | Bancada: 1 carcaça MV, 1 compressor, 1 cano pressurizado. | Tira gás natural (e hélio, a partir da era 18) de bolsões encontrados pela sonda. |
| Cano pressurizado | `pressurized_pipe` | bloco (passivo) | Bancada: 3 placas de aço inox + 1 borracha sintética → 6. | Leva gases. |
| Compressor de gás | `gas_compressor` | máquina MV | Bancada: 1 carcaça MV, 2 pistões, 1 cano pressurizado. | Comprime gás para tanques e canos. |
| Broca de aço rápido | `hss_drill_bit` | componente | Bancada: 3 lingotes de aço rápido + 1 vara de aço rápido. | Sonda rotativa e melhorias da pedreira. |
| Asfalto | `asphalt` | bloco | Refinaria (resíduo) + areia. | Estrada: +20 % de velocidade ao andar. |

## Química e materiais

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Polimerizador | `polymerizer` | máquina MV | Bancada: 1 carcaça MV, 1 misturador, 1 tanque. | Etileno → plástico; butadieno → borracha sintética; ácidos e amônia → nylon. |
| Plástico | `plastic` | material | Polimerizador. | Item-porta; peças, estufa, cabos. |
| Borracha sintética | `synthetic_rubber` | material | Polimerizador. | Substitui a borracha natural com 2× a durabilidade. |
| Nylon | `nylon` | material | Polimerizador. | Tecido de nylon, cordas. |
| Baquelite | `bakelite` | material | Reator de síntese: fenol (do alcatrão) + metanol. | Placas de circuito. |
| Reator de síntese | `synthesis_reactor` | máquina MV | Bancada: 1 carcaça MV, 2 placas de aço inox, 1 cadinho de platina. | Reações orgânicas: baquelite, biodiesel, penicilina. |
| Cadinho de platina | `platinum_crucible` | ferramenta de bancada (64 usos) | Bancada: 5 placas de platina em U. | Química de laboratório. |
| Tanque de lixiviação | `leaching_tank` | máquina MV | Bancada: 1 carcaça MV, 4 placas de aço inox, 1 tanque. | Pó + ácido → 3 do principal, 85 % de secundários, 35 % de traços. |
| Duralumínio | `duralumin_ingot` | material | Forno de indução: 8 alumínio + 1 cobre + 1 magnésio → 10. | Estruturas leves. |
| Cobre-berílio | `beryllium_copper_ingot` | material | Forno de indução: 9 cobre + 1 berílio → 10. | Molas e ferramentas que não soltam faísca. |
| Graxa de lítio | `lithium_grease` | item | Misturador: 1 lubrificante + 1 pó de lítio → 4. | Aplicada numa máquina: +10 % de velocidade por 1 hora. |
| Válvula termiônica | `vacuum_tube` | componente | Bancada: 1 vidro, 1 filamento de tungstênio, 1 placa de níquel. | Circuito T1. |
| Circuito T1 | `circuit_t1` | componente | Bancada: 1 placa de baquelite, 2 válvulas, 4 fios de cobre, 1 solda. | Controle das máquinas avançadas. |

## Agricultura, alimentos e farmácia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Fermentador | `fermenter` | máquina MV | Bancada: 1 carcaça MV, 1 barril de cura, 1 tanque. | Cana, milho ou beterraba → mosto; esterco e restos → biogás. |
| Destilador | `still` | máquina MV | Bancada: 1 carcaça MV, 1 alambique, 1 condensador. | Mosto → etanol. |
| Etanol | `ethanol` | fluido | Destilador. | Combustível. |
| Biodiesel | `biodiesel` | fluido | Reator de síntese: óleo vegetal + metanol. | Combustível. |
| Biorreator | `bioreactor` | máquina MV | Bancada: 1 carcaça MV, 1 tanque, 1 vidro, 1 composto. | Caldo nutritivo, culturas de fungos. |
| Estufa de plástico | `greenhouse_controller` | multibloco (até 16×16, paredes de vidro ou plástico) | Controlador — Bancada: 1 carcaça de máquina, 4 plásticos, 1 filtro de água. | Plantas dentro dela crescem 30 % mais rápido e em qualquer bioma. |
| Câmara de fungos | `fungus_chamber` | máquina | Bancada: 1 carcaça de máquina, 1 biorreator, 1 vidro. | Pão + água tratada → cultura de Penicillium em 10 min. |
| Penicilina | `penicillin` | item | Reator de síntese: 1 cultura de Penicillium + 1 água destilada → 4. | Remove efeitos negativos e dá Regeneração II por 10 s. |
| Refeição pronta | `ready_meal` | comida | Bancada: 1 carne curada + 1 arroz cozido + 1 pão + 1 papelão. | Sacia completamente. |

## Logística

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Embalador | `packager` | Bancada: 1 carcaça MV, 1 liberador, 2 papelões, 1 circuito T1. | Empacota uma pilha do contêiner de cima num pacote. |
| Desembalador | `unpackager` | Bancada: 1 carcaça MV, 1 ejetor, 1 circuito T1. | Desfaz pacotes. |

## Máquinas

| Máquina | Consumo | Tempo | Limite por chunk |
|---------|---------|-------|------------------|
| Pumpjack | 48 SU/t | 50 mB por ciclo de 5 s | 4 |
| Refinaria | 128 SU/t | 100 mB de petróleo por ciclo | 1 |
| Craqueador, polimerizador, reator de síntese | 48 a 64 SU/t | 10 a 20 s | 8 cada |
| Tanque de lixiviação | 48 SU/t | 15 s | 8 |
| Sonda rotativa | 192 SU/t | 1 bloco por segundo | 1 |
| Fermentador, destilador, biorreator | 16 a 32 SU/t | 20 s | 16 cada |
| Estufa de plástico | 8 SU/t | contínuo | 1 controlador |
| Geradores a combustão, biogás, turbina a gás | produzem SU | contínuo | 8 cada |

## Conteúdo atual reaproveitado

Pumpjack, refinaria, geradores a combustão e biogás, turbina a gás,
craqueador, fermentador, destilador, biorreator, cano pressurizado, compressor de gás, sonda (passa
a perfurar o mundo em vez de gerar petróleo virtual), extrator de gás, embalador, desembalador,
lítio e circuito T1.

## Tarefas

- [ ] E11.1 Pumpjack, refinaria, craqueador e combustíveis; asfalto.
- [ ] E11.2 Geradores a combustão, biogás e turbina a gás na rede MV.
- [ ] E11.3 Sonda rotativa perfurando o mundo; extrator de gás.
- [ ] E11.4 Polimerizador, plástico, borracha sintética, nylon; baquelite e reator de síntese.
- [ ] E11.5 Tanque de lixiviação; duralumínio, cobre-berílio, graxa de lítio.
- [ ] E11.6 Válvula termiônica e circuito T1.
- [ ] E11.7 Etanol, biodiesel, estufa de plástico, câmara de fungos, penicilina, refeição pronta.
- [ ] E11.8 Embalador e desembalador com papelão.
- [ ] E11.9 Berilo e espodumênio.
- [ ] E11.10 Texturas.
