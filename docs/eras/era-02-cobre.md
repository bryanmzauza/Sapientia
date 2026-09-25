# Era 2 — Idade do Cobre

**Inspiração:** Calcolítico (5000–3300 a.C.) · **Versão:** 2.2.x · **Status:** planejada

## Resumo

Os primeiros metais: cobre, ouro e prata nativos, triturados e fundidos no forno de argila. A
agricultura se expande com o arado de cobre e o arroz; o sal da salina conserva a carne; o linho
vira tecido no tear manual.

## Desbloqueio

- Requer a era 1 e o Forno de argila.
- **Item-porta:** Martelo de cobre.

## Energia

Nenhuma rede.

## Minerais que passam a cair

| Mineral | Produto principal | Onde cai | Picareta mínima |
|---------|-------------------|----------|-----------------|
| Cobre nativo | cobre | pedra, andesito (Y 0 a 80) | pedra |
| Malaquita | cobre | calcita, pedra (cavernas) | pedra |
| Ouro nativo | ouro (+ prata) | cascalho de rio, pedra (Y −20 a 40) | pedra |
| Prata nativa | prata (+ cobre) | diorito (Y 0 a 40) | pedra |
| Halita | sal | pedra, calcita (Y −20 a 40) | pedra |

## Metalurgia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Fragmentos da era | `native_copper_fragment`, `malachite_fragment`, `native_gold_fragment`, `native_silver_fragment` | mineral | Rocha virgem. | Martelo → triturado. |
| Minérios triturados | `crushed_copper`, `crushed_gold`, `crushed_silver` | material | Martelo + fragmento. | Forno de argila → lingote. |
| Lingote de cobre e de ouro | vanilla | material | Forno de argila (ou minério vanilla). | Base da era. |
| Lingote de prata | `silver_ingot` | material | Forno de argila. | Electrum, eletrônica futura. |
| Mistura de electrum | `electrum_blend` | material | Bancada: 1 ouro triturado + 1 prata triturada → 2. | Forno de argila → lingote de electrum. |
| Lingote de electrum | `electrum_ingot` | material | Forno de argila. | Instrumentos (era 7), contatos elétricos (era 9). |
| Placa de cobre | `copper_plate` | material | Bancada: 2 lingotes de cobre + martelo → 1. | Martelo de cobre, bateia, arado. |
| Martelo de cobre | `copper_hammer` | ferramenta de bancada (128 usos) | Bancada: 2 placas de cobre + 1 lingote de cobre, 2 gravetos. | Como o de pedra, com 20 % de chance de 2 triturados. Item-porta. |

As ferramentas e armaduras de cobre do vanilla são as melhores da era.

## Agricultura e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Arado de cobre | `copper_plow` | ferramenta (200 usos) | Bancada: 2 placas de cobre, 1 tábua, 2 gravetos. | Ara 3 blocos em linha de uma vez. |
| Sementes de arroz | `rice_seeds` | semente | Faca de sílex em pântanos e margens de rio; colheita. | Só cresce em terra arada com água a até 1 bloco; nessas condições, cresce 20 % mais rápido que o trigo. |
| Arroz | `rice` | produto | Colheita. | Mó manual → farinha de arroz; fogueira → arroz cozido. |
| Arroz cozido | `cooked_rice` | comida | Fogueira ou fornalha vanilla. | Sacia como pão de farinha. |
| Carne salgada | `salted_meat` | comida | Bancada: 1 carne crua qualquer + 1 sal. | Sacia 1,5× a carne cozida, sem precisar de fogo. |
| Adubo de cinzas | `ash_fertilizer` | material | Bancada: 1 potassa + 2 compostos → 3. | Como o composto, mas adianta 2 estágios num raio 3×3. |

## Madeira e fibras

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Serra manual | `hand_saw` | ferramenta de bancada (128 usos) | Bancada: 1 placa de cobre, 2 gravetos. | 1 tora → 6 tábuas (em vez de 4). |
| Fuso | `spindle` | ferramenta de bancada (128 usos) | Bancada: 1 graveto, 1 pedra lisa. | 2 fibras de linho → 1 fio de linho. |
| Fio de linho | `linen_thread` | material | Bancada: 2 linhos + fuso. | Tear manual. |
| Tear manual | `hand_loom` | bloco | Bancada: 4 tábuas, 2 gravetos, 1 barbante. | 4 fios de linho → 1 tecido de linho (4 cliques). |
| Tecido de linho | `linen_cloth` | material | Tear manual. | Sacos, velas, filtros, armaduras acolchoadas. |
| Saco de tecido | `cloth_sack` | bloco | Bancada: 7 tecidos de linho + 1 barbante. | Como o cesto de fibra, com 18 espaços. |

## Água e químicos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Salina | `salt_pan` | bloco (máquina) | Bancada: 4 tijolos refratários + 1 caldeirão vanilla. | Colocada sobre água de oceano ou praia: produz 1 sal a cada 2 min, se estiver ao ar livre durante o dia. |
| Sal | `salt` | material | Salina, ou fragmento de halita moído no martelo. | Conservar comida, queijo (era 3), cloro-álcali (era 9). |

## Máquinas

| Máquina | Entrada → saída | Tempo | Condição | Limite por chunk |
|---------|-----------------|-------|----------|------------------|
| Salina | água do mar → sal | 2 min | ao ar livre, de dia | 16 |
| Tear manual | 4 fios → 1 tecido | 4 cliques | — | 16 |

## Tarefas

- [ ] E2.1 Drop de cobre nativo, malaquita, ouro nativo, prata nativa e halita.
- [ ] E2.2 Minérios triturados e receitas do forno de argila; integração com o minério vanilla.
- [ ] E2.3 Electrum, placa de cobre, martelo de cobre.
- [ ] E2.4 Arado de cobre, arroz (regra de água), carne salgada, adubo de cinzas.
- [ ] E2.5 Serra manual, fuso, tear manual, tecido e saco.
- [ ] E2.6 Salina com produção por evento agendado.
- [ ] E2.7 Texturas.
