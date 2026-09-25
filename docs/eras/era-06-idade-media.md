# Era 6 — Idade Média

**Inspiração:** 500–1450 · **Versão:** 2.6.x · **Status:** planejada

## Resumo

O moinho de vento leva a força para longe dos rios. O alto-forno produz ferro em escala e o aço de
Damasco aparece nos martelos de mineração. O alambique inaugura a química: álcool, ácidos e a
água-forte, que separa o ouro da prata. A apicultura e a rotação de culturas melhoram o campo.

## Desbloqueio

- Requer a era 5 e a Roda d'água.
- **Item-porta:** Alto-forno.

## Energia

Mecânica local (UM): rodas d'água e moinhos de vento.

## Minerais que passam a cair

| Mineral | Produto | Onde cai | Picareta mínima |
|---------|---------|----------|-----------------|
| Bismutinita | bismuto | granito (Y −10 a 30) | ferro |
| Pirolusita | manganês (+ ferro) | terracota, pedra (Y 0 a 60) | ferro |
| Enxofre nativo | enxofre | netherrack, basalto (Nether) | ferro |
| Salitre | nitrato de potássio | areia vermelha, terracota (desertos) | ferro |

## Energia e metalurgia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Moinho de vento | `windmill` | bloco (gerador) | Bancada: 4 velas de tecido, 1 eixo de madeira, 4 tábuas. | 3 UM acima de Y 80; 1 UM abaixo; ×1,5 durante tempestades. |
| Alto-forno | `blast_furnace_controller` | multibloco (3×3×4 de tijolos refratários + controlador) | Controlador — Bancada: 4 tijolos refratários, 2 lingotes de ferro, 2 foles automáticos, 1 forno de lupa. | Minério de ferro + carvão vegetal + cal virgem → 2 ferros fundidos por minério. Item-porta. |
| Ferro fundido | `cast_iron_ingot` | material | Alto-forno. | Forja de refino → lingote de ferro; canos de vapor (era 8); grades. |
| Forja de refino | `finery_forge` | máquina (1 UM) | Bancada: 6 tijolos refratários, 1 fole automático, 2 lingotes de ferro. | 1 ferro fundido → 1 lingote de ferro. |
| Cadinho | `crucible` | ferramenta de bancada (16 usos) | Fornalha vanilla: argila refratária moldada (Bancada: 5 argilas refratárias em forma de U). | Aço de Damasco. |
| Lingote de aço de Damasco | `damascus_steel_ingot` | material | Alto-forno: 1 lingote de ferro + 2 carvões vegetais + 1 vidro, com cadinho. | Ferramentas de alto nível, martelo de mineração, molas (era 7). |
| Martelo de mineração | `mining_hammer` | ferramenta (600 usos) | Bancada: 3 lingotes de aço de Damasco, 2 blocos de ferro, 2 varas de bronze. | Quebra 3×3 com metade da velocidade de uma picareta de ferro. Aplica as regras de terreno virgem. |
| Triturados da era | `crushed_bismuth`, `crushed_manganese` | material | Martelo ou pilão. | Forno de argila → lingote de bismuto; pó de manganês. |
| Lingote de bismuto | `bismuth_ingot` | material | Forno de argila. | Liga de baixa fusão para fusíveis (era 9). |
| Pó de manganês | `manganese_dust` | material | Pilão com pirolusita. | Vidro óptico (era 7), aço (era 8). |

## Química: o alambique

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Alambique | `alembic` | máquina (combustível) | Bancada: 4 placas de cobre, 2 frascos de vidro, 2 varas de cobre, 1 forno de argila. | Destila líquidos; receitas abaixo. |
| Álcool | `alcohol` | fluido (garrafa) | Alambique: 4 vinhos → 1 álcool. | Combustível de lamparina; desinfetante; tintura. |
| Ácido sulfúrico diluído | `dilute_sulfuric_acid` | fluido (garrafa) | Alambique: 1 enxofre + 1 frasco de água. | Ácido nítrico, ácido clorídrico. |
| Ácido clorídrico | `hydrochloric_acid` | fluido (garrafa) | Alambique: 1 sal + 1 ácido sulfúrico diluído. | Água-régia (era 7). |
| Ácido nítrico | `nitric_acid` | fluido (garrafa) | Alambique: 1 salitre + 1 ácido sulfúrico diluído. | Água-forte; fertilizantes (era 10). |
| Água-forte (uso) | — | — | Bancada: 1 lingote de electrum + 1 ácido nítrico. | Separa em 1 ouro + 1 prata. Também recupera 35 % da prata dos rejeitos. |
| Tintura de ervas | `herbal_tincture` | bebida | Alambique: 4 ervas medicinais + 1 álcool. | Regeneração I por 30 s. |

## Agricultura e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Rotação de culturas | mecânica | — | — | A terra arada lembra a última planta. Plantar uma espécie diferente da anterior dá +20 % de velocidade; a mesma espécie três vezes seguidas dá −20 %. |
| Apiário | `apiary` | bloco | Bancada: 6 tábuas, 1 colmeia vanilla, 2 favos. | Abrigo de abelhas vanilla que rende 2× mel e favos e produz cera de abelha. |
| Cera de abelha | `beeswax` | material | Apiário. | Velas, selante, moldes de cera perdida (era 7). |
| Hidromel | `mead` | bebida | Barril de cura: 2 garrafas de mel + 2 garrafas de água → 4 em 10 min. | Absorção I por 30 s. |
| Pão de mel | `honey_bread` | comida | Bancada: 3 farinhas + 1 garrafa de mel → massa; fornalha. | Sacia 2× o pão vanilla. |
| Poço com sarilho | `well` | bloco | Bancada: 8 tijolos de pedra, 1 balde, 1 barbante. | Fonte de água para canos e saídas de irrigação: 1 balde a cada 10 s, sem precisar de rio. |

## Máquinas

| Máquina | Entrada → saída | Tempo | Força | Limite por chunk |
|---------|-----------------|-------|-------|------------------|
| Moinho de vento | — → 1 a 3 UM | contínuo | vento | 8 |
| Alto-forno | minério de ferro + carvão + cal → 2 ferros fundidos | 30 s | combustível + fole automático | 2 |
| Forja de refino | ferro fundido → lingote de ferro | 10 s | 1 UM | 8 |
| Alambique | receitas acima | 20 s | combustível | 8 |
| Apiário | abelhas → mel, favos, cera | ciclo das abelhas vanilla | — | 8 |
| Poço | — → água | 1 balde / 10 s | — | 4 |

## Conteúdo atual reaproveitado

- **Aço de Damasco** (`damascus_steel_*`).

## Tarefas

- [ ] E6.1 Moinho de vento com altitude e clima.
- [ ] E6.2 Alto-forno (multibloco), ferro fundido, forja de refino.
- [ ] E6.3 Cadinho, aço de Damasco, martelo de mineração 3×3 com regras de terreno virgem.
- [ ] E6.4 Bismutinita, pirolusita, enxofre nativo e salitre.
- [ ] E6.5 Alambique e seus produtos; água-forte.
- [ ] E6.6 Rotação de culturas; apiário, cera, hidromel, pão de mel; poço.
- [ ] E6.7 Texturas.
