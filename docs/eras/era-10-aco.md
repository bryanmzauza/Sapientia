# Era 10 — Era do Aço

**Inspiração:** Segunda Revolução Industrial (1880–1920) · **Versão:** 2.10.x · **Status:** planejada

## Resumo

O aço vira família: inox, nicromo, invar, kanthal e aço rápido saem do forno de indução. A rede
sobe para MV; a flotação e a centrífuga separam mais metais de cada minério; a pedreira abre cavas
a céu aberto. A química do nitrogênio (Haber-Bosch) cria os fertilizantes que multiplicam a
colheita, e a soja entra no campo.

## Desbloqueio

- Requer a era 9 e a Carcaça de máquina.
- **Item-porta:** Lingote de aço inox.

## Energia: MV

Transformador LV→MV, cabos e capacitores MV. Máquinas MV exigem a rede MV.

## Minerais que passam a cair

| Mineral | Produto | Onde cai | Picareta mínima |
|---------|---------|----------|-----------------|
| Vanadinita | vanádio (+ chumbo) | areia vermelha, terracota (desertos) | ferro |
| Silvita (não metálico) | potássio | calcita, pedra (Y −30 a 20) | ferro |

## Energia e metalurgia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Transformador LV→MV | `transformer_lv_mv` | bloco | Bancada: 1 carcaça MV, 4 bobinas T1, 2 placas de aço-carbono. | Liga redes LV e MV. |
| Cabo MV | `cable_t2` | bloco (passivo) | Bancada: 3 fios de alumínio + 2 borrachas + 1 tecido → 8. | Rede MV. |
| Capacitor MV | `capacitor_t2` | bloco | Bancada: 4 capacitores LV + 1 carcaça MV. | Guarda 400.000 SU. |
| Carcaça MV | `machine_casing_mv` | bloco e componente | Bancada: 4 placas de aço inox, 2 motores T1, 1 carcaça de máquina, 2 fios de alumínio. | Base das máquinas MV. |
| Carcaça de aço inox | `stainless_steel_casing` | bloco | Bancada: 4 placas de aço inox em volta de 1 carcaça MV → 8. | Estrutura da pedreira, da refinaria e da sonda. |
| Forno de indução | `induction_furnace_controller` | multibloco (3×3×3) | Controlador — Bancada: 1 carcaça de máquina, 4 bobinas T1, 2 filamentos de tungstênio, 2 placas de cobre. | Ligas por proporção (tabela abaixo). |
| Aço inox | `stainless_steel_ingot` | material | Forno de indução: 6 aço-carbono + 2 pós de cromo + 1 pó de níquel → 9. | Item-porta; carcaças MV, químicos. |
| Nicromo | `nichrome_ingot` | material | Forno de indução: 4 pós de níquel + 1 pó de cromo → 5. | Resistências de aquecimento. |
| Invar | `invar_ingot` | material | Forno de indução: 2 aço-carbono + 1 pó de níquel → 3. | Instrumentos de precisão que não dilatam. |
| Kanthal | `kanthal_ingot` | material | Forno de indução: 3 aço-carbono + 1 pó de cromo + 1 lingote de alumínio → 5. | Fornos de alta temperatura. |
| Aço rápido | `high_speed_steel_ingot` | material | Forno de indução: 4 aço-carbono + 1 tungstênio + 1 molibdênio + 1 vanádio → 7. | Ferramentas de corte e brocas. |
| Misturador | `mixer` | máquina MV | Bancada: 1 carcaça MV, 1 motor T1, 1 caldeirão. | Mistura várias entradas por proporção (ligas em pó, fertilizantes). |
| Compressor | `compressor` | máquina MV | Bancada: 1 carcaça MV, 2 pistões, 1 motor T1. | 9 lingotes → 1 bloco; pós → pastilhas. |
| Prensa de placas | `plate_press` | máquina MV | Bancada: 1 carcaça MV, 1 bigorna, 1 motor T1. | 1 lingote → 1 placa. |
| Extrator | `extractor` | máquina MV | Bancada: 1 carcaça MV, 1 fieira, 1 motor T1. | 1 lingote → 4 fios. |
| Célula de flotação | `flotation_cell` | máquina MV | Bancada: 1 carcaça MV, 1 tanque de água, 1 sabão, 2 placas de aço inox. | Pó + água + óleo → concentrado: 2,5 do principal, 70 % de secundários, 20 % de traços. |
| Centrífuga | `centrifuge` | máquina MV | Bancada: 1 carcaça MV, 2 motores T1, 1 invar. | Separa rejeitos e líquidos em frações. |

## Mineração e logística

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Pedreira | `quarry_controller` | Multibloco 3×3×4 de carcaças de aço inox + controlador (Bancada: 1 carcaça MV, 2 brocas elétricas, 1 mecanismo de precisão). | Escava uma área 16×16 camada por camada até a rocha-mãe (até 64×64 com melhorias), mandando os drops para contêineres. Aplica as regras de terreno virgem. |
| Prospector | `prospector` | Bancada: 1 bússola geológica, 1 bateria, 2 fios de cobre, 1 lente. | Mostra os veios num raio de 8 chunks. |
| Buffer, divisor, câmara de filtros, transbordo | `item_buffer`, `item_splitter`, `filter_chamber`, `overflow_module` | Bancada: carcaça MV + motor T1 + componente vanilla (barril, observador, alçapão, funil). | Logística avançada de itens. |
| Sensor comparador | `comparator_sensor` | Bancada: 1 carcaça MV, 1 comparador. | Lê o nível de um contêiner. |
| Esteira | `conveyor_belt` | Bancada: 3 borrachas, 2 motores T1, 3 placas de aço → 8. | Move itens no chão. |
| Válvula e sensor de nível | `fluid_valve`, `fluid_level_sensor` | Bancada: carcaça MV + alavanca ou comparador. | Controle de fluidos. |

## Química, agricultura e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Separador de ar | `air_separator` | máquina MV | Bancada: 1 carcaça MV, 2 compressores, 1 tanque. | Ar → nitrogênio + oxigênio (gases). |
| Reator de amônia | `ammonia_reactor` | máquina MV | Bancada: 1 carcaça MV, 4 placas de aço inox, 1 platina. | Nitrogênio + hidrogênio → amônia (Haber-Bosch). |
| Torre de ácido nítrico | `nitric_acid_tower` | máquina MV | Bancada: 1 carcaça MV, 1 platina, 2 tanques. | Amônia + oxigênio → ácido nítrico (Ostwald). |
| Fertilizante NPK | `npk_fertilizer` | material | Misturador: 1 nitrato de amônio + 1 superfosfato + 1 potássio → 3. | +75 % de velocidade num raio 5×5 por 3 dias de jogo. |
| Sementes de soja | `soybean_seeds` | semente | Faca de sílex em planícies; colheita. | Terra arada. Também devolve nitrogênio ao solo (+20 % para a próxima planta). |
| Soja | `soybean` | produto | Colheita. | Prensa de óleo → óleo de soja + farelo. |
| Ração animal | `animal_feed` | item | Misturador: 1 farelo de soja + 1 milho + 1 trigo → 4. | Dada a animais: reprodução 50 % mais frequente e filhotes crescem 2× mais rápido. |
| Destilador de água | `water_distiller` | máquina MV | Bancada: 1 carcaça MV, 1 alambique, 1 condensador. | Água tratada → água destilada. |
| Aspirina | `aspirin` | item | Misturador: 1 casca de bétula + 1 ácido acético → ácido salicílico; + 1 ácido sulfúrico → 4 aspirinas. | Remove efeitos negativos e cura 2 corações. |
| Casca de bétula | `birch_bark` | material | Descascar tronco de bétula com a faca de sílex. | Aspirina. |

## Máquinas

| Máquina | Consumo | Tempo | Limite por chunk |
|---------|---------|-------|------------------|
| Forno de indução | 64 SU/t | 20 s | 2 |
| Misturador, compressor, prensa, extrator | 16 a 24 SU/t | 4 a 8 s | 32 cada |
| Célula de flotação | 32 SU/t | 10 s | 16 |
| Centrífuga | 32 SU/t | 10 s | 16 |
| Pedreira | 128 SU/t | 1 bloco a cada 0,5 s | 1 |
| Separador de ar, reator de amônia, torre de ácido nítrico | 48 a 96 SU/t | contínuo | 4 cada |
| Destilador de água | 24 SU/t | 100 mB/s | 8 |

## Conteúdo atual reaproveitado

Cabo e capacitor MV, transformador LV→MV, carcaça MV, misturador, compressor, prensa de placas,
extrator, forno de indução, aço inox, nicromo, carcaça de aço inox, pedreira (passa a escavar de verdade), prospector
(passa a mostrar veios) e a logística avançada. As receitas de logística deixam de pedir circuito T3.

## Tarefas

- [ ] E10.1 Rede MV: transformador, cabo, capacitor, carcaça MV.
- [ ] E10.2 Forno de indução com as cinco ligas.
- [ ] E10.3 Misturador com várias entradas; compressor, prensa, extrator.
- [ ] E10.4 Flotação e centrífuga (tabela de separação da era).
- [ ] E10.5 Pedreira escavando o mundo; prospector de veios.
- [ ] E10.6 Logística avançada com receitas coerentes.
- [ ] E10.7 Separador de ar, amônia, ácido nítrico, fertilizante NPK.
- [ ] E10.8 Soja, ração animal, destilador de água, aspirina.
- [ ] E10.9 Vanadinita e silvita.
- [ ] E10.10 Texturas.
