# Era 5 — Antiguidade Clássica

**Inspiração:** Grécia e Roma (500 a.C.–500 d.C.) · **Versão:** 2.5.x · **Status:** planejada

## Resumo

A água começa a trabalhar pelo jogador. A roda d'água move o pilão, a serraria, o moinho de grãos
e o fole; aquedutos levam água às plantações. A videira dá vinho e vinagre, o zinco dá o latão e a
cal com tufo vira concreto romano.

## Desbloqueio

- Requer a era 4 e a Lupa de ferro.
- **Item-porta:** Roda d'água.

## Energia: mecânica local (UM)

- A **roda d'água** gera 4 UM quando tem água corrente encostada em pelo menos 3 das pás.
- O **eixo de madeira** leva a força em linha reta por até 8 blocos; a **caixa de engrenagens**
  vira 90° ou divide em duas saídas.
- Cada máquina consome UM enquanto trabalha; se a força disponível não bastar, todas as máquinas
  ligadas à mesma roda trabalham mais devagar, na proporção.
- Não existe rede: cada roda com seus eixos forma um conjunto pequeno e isolado.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Esfalerita | zinco | cádmio, ferro | pedra, calcita, tufo (Y −30 a 30) | ferro |
| Estibinita | antimônio | — | tufo, netherrack (Y −20 a 20) | ferro |

## Energia mecânica e máquinas

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Roda d'água | `water_wheel` | bloco (gerador) | Bancada: 4 tábuas, 4 varas de bronze, 1 engrenagem de bronze. | 4 UM com água corrente. Item-porta. |
| Eixo de madeira | `wooden_axle` | bloco (passivo) | Bancada: 3 toras em linha → 6. | Leva UM em linha reta (até 8 blocos por conjunto). |
| Caixa de engrenagens | `gearbox` | bloco (passivo) | Bancada: 4 tábuas, 4 engrenagens de bronze, 1 vara de bronze. | Vira a força 90° ou divide em duas saídas. |
| Pilão hidráulico | `trip_hammer` | máquina (2 UM) | Bancada: 4 toras, 2 lingotes de ferro, 2 engrenagens de bronze, 1 martelo de ferro. | Fragmento → 1 triturado + 50 % de chance de outro. |
| Fole automático | `mechanical_bellows` | máquina (1 UM) | Bancada: 1 fole + 2 engrenagens de bronze. | Mantém o forno de lupa aceso sem cliques. |
| Serraria hidráulica | `water_sawmill` | máquina (2 UM) | Bancada: 4 tábuas, 1 serra manual, 2 engrenagens de bronze, 2 lingotes de ferro. | 1 tora → 9 tábuas + 1 serragem. |
| Moinho de grãos | `grain_mill` | máquina (2 UM) | Bancada: 2 mós manuais, 2 engrenagens de bronze, 4 tábuas. | 1 trigo ou arroz → 1,5 farinha. |
| Calha de lavagem | `sluice_box` | máquina (água corrente) | Bancada: 5 tábuas, 2 placas de bronze, 1 bateia. | Minério triturado → lavado, com 25 % de chance de secundário; automatiza a bateia. |

## Metalurgia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Fragmentos e triturados da era | `sphalerite_fragment`, `stibnite_fragment`, `crushed_zinc`, `crushed_antimony` | mineral, material | Rocha virgem; martelo ou pilão. | Forno de argila → lingote. |
| Lingote de zinco | `zinc_ingot` | material | Forno de argila. | Latão; galvanização (era 8). |
| Lingote de antimônio | `antimony_ingot` | material | Forno de argila. | Liga de tipos (era 7); baterias (era 9). |
| Mistura de latão | `brass_blend` | material | Bancada: 3 cobre triturado + 1 zinco triturado → 4. | Forno de argila → lingote de latão. |
| Lingote de latão | `brass_ingot` | material | Forno de argila. | Engrenagens finas, instrumentos (era 7). |
| Mistura de peltre | `pewter_blend` | material | Bancada: 3 estanho triturado + 1 galena triturada → 4. | Forno de argila → lingote de peltre. |
| Lingote de peltre | `pewter_ingot` | material | Forno de argila. | Canecas, pratos e revestimento do cano de chumbo. |

## Agricultura e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Muda de videira | `grape_vine` | semente | Faca de sílex em cipós de florestas e bosques; colheita. | Plantada ao lado de uma treliça. |
| Treliça | `trellis` | bloco | Bancada: 6 gravetos → 4. | Suporte da videira. |
| Uva | `grapes` | comida e produto | Colheita da videira. | Comida leve; lagar → suco de uva. |
| Lagar | `wine_press` | máquina (1 UM ou cliques) | Bancada: 4 tábuas, 1 barril, 1 vara de bronze. | 4 uvas → 1 garrafa de suco de uva. |
| Vinho | `wine` | bebida | Barril de cura: 4 sucos de uva → 4 vinhos em 10 min. | Resistência I por 30 s; destilado em álcool (era 6). |
| Vinagre | `vinegar` | material | Barril de cura: vinho deixado mais 10 min. | Picles, químicos, pigmentos. |
| Picles | `pickles` | comida | Barril de cura: 4 cenouras ou beterrabas + 1 vinagre + 1 sal → 4. | Sacia bem. |
| Varal de secagem | `drying_rack` | bloco | Bancada: 3 gravetos, 2 barbantes, 1 tábua. | Seca itens colocados nele: uva → passa, carne → carne seca, erva → erva seca (2 min). |
| Passas | `raisins` | comida | Varal de secagem. | Leve e com efeito de saciedade longa. |
| Sementes de anil | `indigo_seeds` | semente | Faca de sílex em selvas e savanas. | Plantadas em terra arada. |
| Anil | `indigo` | produto | Colheita. | Bancada: 2 anis + 1 balde de água → 4 corantes azuis. |

## Água e químicos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Canal de aqueduto | `aqueduct` | bloco (passivo) | Bancada: 5 tijolos de pedra + 1 argamassa → 4. | Leva água de uma fonte em linha reta e em declive leve (até 64 blocos). |
| Saída de irrigação | `irrigation_outlet` | bloco | Bancada: 4 tijolos de pedra, 1 cano de chumbo. | No fim de um aqueduto, hidrata terra arada num raio 9×9 sem espalhar água. |
| Cano de chumbo | `lead_pipe` | bloco (passivo) | Bancada: 3 lingotes de chumbo + 1 peltre → 6. | Leva água de um aqueduto ou cisterna até máquinas (calha, lagar, tanques). |
| Cisterna | `cistern` | bloco | Bancada: 8 tijolos de pedra, 1 caldeirão. | Guarda 16 baldes de água e enche com chuva. |
| Tufo triturado (pozolana) | `pozzolana` | material | Martelo ou pilão em tufo. | Concreto romano. |
| Concreto romano | `roman_concrete` | bloco | Bancada: 2 cal virgem + 4 pozolanas + 1 balde de água → 8. | Bloco muito resistente (explosões e fogo), inclusive embaixo d'água. |

## Máquinas

| Máquina | Entrada → saída | Tempo | Força | Limite por chunk |
|---------|-----------------|-------|-------|------------------|
| Roda d'água | — → 4 UM | contínuo | água corrente | 16 |
| Pilão hidráulico | fragmento → 1,5 triturado | 10 s | 2 UM | 16 |
| Serraria hidráulica | tora → 9 tábuas + serragem | 5 s | 2 UM | 16 |
| Moinho de grãos | trigo ou arroz → 1,5 farinha | 5 s | 2 UM | 16 |
| Calha de lavagem | triturado → lavado + 25 % de secundário | 8 s | água corrente | 16 |
| Lagar | 4 uvas → 1 suco | 5 s | 1 UM ou 4 cliques | 16 |

Blocos passivos (eixo, aqueduto, cano) seguem o limite geral de blocos por chunk.

## Conteúdo atual reaproveitado

- **Latão** (`brass_*`) e **pedestal** (decorativo).

## Tarefas

- [ ] E5.1 Energia mecânica local: roda d'água, eixo, caixa de engrenagens, divisão proporcional.
- [ ] E5.2 Pilão, fole automático, serraria, moinho de grãos, calha de lavagem.
- [ ] E5.3 Esfalerita, estibinita, zinco, antimônio, latão e peltre.
- [ ] E5.4 Videira, treliça, lagar, vinho, vinagre, picles; varal de secagem; anil.
- [ ] E5.5 Aqueduto, saída de irrigação, cano de chumbo, cisterna.
- [ ] E5.6 Pozolana e concreto romano.
- [ ] E5.7 Texturas.
