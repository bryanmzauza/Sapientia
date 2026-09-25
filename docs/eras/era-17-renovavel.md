# Era 17 — Energia Renovável

**Inspiração:** 2010–2040 · **Versão:** 2.17.x · **Status:** planejada

## Resumo

Sol, vento, calor da terra e hidrogênio verde substituem os combustíveis. Bancos de baterias guardam
energia em grande escala. A fazenda vertical e as algas elevam a produção agrícola, e o reciclador
transforma sucata em metal: a primeira fonte renovável de minério.

## Desbloqueio

- Requer a era 16 e o Núcleo de IA.
- **Item-porta:** Célula solar avançada.

## Energia

IV, com geração renovável. A produção de painéis e turbinas é calculada por rede, a partir da
quantidade de geradores, da hora e do clima, sem custo por bloco.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Xenotima | ítrio | disprósio, térbio | granito, areia | diamante |
| Calaverita | telúrio | ouro | tufo, pedra | diamante |

## Geração e armazenamento

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Filme de cádmio-telúrio | `cdte_film` | Reator químico: 1 cádmio + 1 telúrio + 1 vidro → 4. | Painel solar T2. |
| Membrana | `membrane` | Polimerizador: 2 plásticos + 1 ácido sulfúrico → 4. | Célula a combustível, bateria de fluxo. |
| Célula de bateria | `battery_cell` | Bancada: 4 baterias de íon-lítio + 1 carcaça IV. | Bloco do banco de baterias (100 milhões de SU cada). |
| Painel solar T1 | `solar_panel_t1` | Bancada: 4 células solares T1, 1 placa de alumínio, 1 vidro, 1 fio de cobre. | 8 SU/t ao sol, proporcional à luz. |
| Painel solar T2 (filme fino) | `solar_panel_t2` | Bancada: 4 filmes de cádmio-telúrio, 1 vidro, 1 circuito T3. | 24 SU/t ao sol. |
| Célula solar avançada | `solar_cell_t3` | Fotolitografia: wafer + índio + gálio + telúrio (célula de várias camadas). | Painel solar T3. Item-porta. |
| Painel solar T3 | `solar_panel_t3` | Bancada: 4 células solares avançadas, 1 vidro, 1 processador T3. | 64 SU/t ao sol. |
| Turbina eólica | `wind_turbine_controller` | Multibloco (torre 1×1×16 + rotor) — controlador: 1 carcaça IV, 4 ímãs de neodímio com disprósio, 1 processador T3. | Até 512 SU/t, conforme altitude e vento. |
| Geradora geotérmica | `geothermal_gen` | Bancada: 1 carcaça IV, 4 canos pressurizados, 1 turbina a vapor. | SU a partir de lava vizinha ou de calor profundo (abaixo de Y −40). |
| Célula a combustível | `fuel_cell` | Bancada: 1 carcaça IV, 4 placas de platina, 1 membrana (plástico + ácido). | Hidrogênio + oxigênio → SU + água. |
| Banco de baterias | `battery_bank_controller` | Multibloco até 5×5×5 de células de bateria + controlador. | Guarda até 1 bilhão de SU. |
| Bateria de fluxo de vanádio | `vanadium_flow_battery` | Bancada: 2 tanques de hastelloy, 4 vanádios, 1 membrana, 1 carcaça IV. | Armazenamento barato e durável para redes. |

## Recursos renováveis

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Reciclador | `recycler` | Bancada: 1 carcaça IV, 1 macerador, 1 centrífuga, 1 processador T3. | Desmonta itens e blocos do Sapientia e do vanilla em metais e materiais (70 % dos ingredientes). Mineração urbana. |
| Dessalinizador | `desalinator_controller` | Multibloco 5×3×3 + controlador. | Água do mar → água doce + sal. |
| Eletrolisador verde | `electrolyzer` (melhoria) | Eletrolisador ligado só a geração renovável. | Hidrogênio verde (vale como hidrogênio). |
| Fábrica de pellets | `pellet_mill` | Bancada: 1 carcaça de máquina, 1 compressor, 1 serra de bancada. | Serragem, palha e restos → pellets de biomassa (combustível para caldeiras e geradores). |

## Agricultura

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Fazenda vertical | `vertical_farm_controller` | Multibloco até 9×9×9 de leitos hidropônicos + controlador (1 carcaça IV, 4 luzes de cultivo, 1 computador). | Andares de hidroponia; crescimento 2× e agendado por evento. |
| Biorreator de algas | `bioreactor` (receita de algas) | Biorreator com kelp e água do mar. | Algas → biodiesel e fertilizante. |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Painéis solares | produzem SU | 256 |
| Turbina eólica | produz SU | 1 |
| Geotérmica, célula a combustível | produzem SU | 8 cada |
| Banco de baterias | — | 1 |
| Reciclador | 512 SU/t | 4 |
| Fazenda vertical | 256 SU/t | 1 |

## Conteúdo atual reaproveitado

Geradora geotérmica, dessalinizador, eletrolisador e biorreator.

## Tarefas

- [ ] E17.1 Xenotima e calaverita.
- [ ] E17.2 Painéis solares T1 a T3 com produção por rede.
- [ ] E17.3 Turbina eólica, geotérmica, célula a combustível.
- [ ] E17.4 Banco de baterias e bateria de fluxo.
- [ ] E17.5 Reciclador (mineração urbana), dessalinizador, hidrogênio verde, pellets.
- [ ] E17.6 Fazenda vertical e algas.
- [ ] E17.7 Texturas.
