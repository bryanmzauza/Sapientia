# Era 3 — Idade do Bronze

**Inspiração:** 3300–1200 a.C. · **Versão:** 2.3.x · **Status:** planejada

## Resumo

O estanho chega, e com ele o bronze. A bateia recupera pela primeira vez os metais secundários. A
foice de bronze acelera a colheita, o queijo aproveita o leite e o tear de pedal produz tecido em
volume.

## Desbloqueio

- Requer a era 2 e o Martelo de cobre.
- **Item-porta:** Lingote de bronze.

## Energia

Nenhuma rede.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Calcopirita | cobre | ferro | diorito, pedra (Y −20 a 50) | cobre |
| Cassiterita | estanho | — | granito, cascalho de rio (Y 0 a 60) | cobre |
| Arsenopirita | arsênio | ferro | ardósia profunda, pedra (Y −30 a 30) | cobre |

O ferro desses minerais vai para o rejeito até a era 4.

## Metalurgia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Fragmentos da era | `chalcopyrite_fragment`, `cassiterite_fragment`, `arsenopyrite_fragment` | mineral | Rocha virgem. | Martelo → triturado. |
| Minérios triturados | `crushed_chalcopyrite`, `crushed_tin`, `crushed_arsenopyrite` | material | Martelo + fragmento. | Forno de argila → lingote; bateia → minério lavado. |
| Minério lavado | `washed_<mineral>` | material | Bateia sobre um minério triturado. | Forno de argila → lingote; o secundário já foi separado. |
| Rejeito de mineral | `tailings_<mineral>` | material | Sobra da bateia quando o secundário é de era bloqueada. | Reprocessado em eras futuras. |
| Bateia | `gold_pan` | ferramenta (128 usos) | Bancada: 3 tábuas em forma de tigela + 1 placa de cobre. | Em pé na água, com minério triturado na outra mão: vira minério lavado com 20 % de chance de dar o secundário triturado. |
| Lingote de estanho | `tin_ingot` | material | Forno de argila. | Bronze; solda (era 9); latas (era 8). |
| Lingote de arsênio | `arsenic_ingot` | material | Forno de argila. | Bronze arsenical; semicondutores (era 12). |
| Mistura de bronze | `bronze_blend` | material | Bancada: 3 cobre triturado + 1 estanho triturado → 4. | Forno de argila → lingote de bronze. |
| Mistura de bronze arsenical | `arsenical_bronze_blend` | material | Bancada: 3 cobre triturado + 1 arsenopirita triturada → 4. | Forno de argila → lingote de bronze arsenical. |
| Lingote de bronze | `bronze_ingot` | material | Forno de argila. | Ferramentas, engrenagens, fole. Item-porta. |
| Lingote de bronze arsenical | `arsenical_bronze_ingot` | material | Forno de argila. | Aceito no lugar do bronze em ferramentas (80 % da durabilidade). |
| Placa, vara e engrenagem de bronze | `bronze_plate`, `bronze_rod`, `bronze_gear` | material | Bancada com martelo: placa (2 lingotes → 1), vara (1 → 2); engrenagem: 4 placas em volta de 1 vara. | Máquinas das eras 4 a 6. |
| Ferramentas de bronze | `bronze_pickaxe`, `bronze_axe`, `bronze_shovel`, `bronze_hoe`, `bronze_sword` | ferramentas | Bancada: formato vanilla com lingotes de bronze. | Entre cobre e ferro (durabilidade 350). A picareta minera os minerais da era 4. |
| Armadura de bronze | `bronze_helmet`, `bronze_chestplate`, `bronze_leggings`, `bronze_boots` | armadura | Bancada: formato vanilla com placas de bronze. | Entre cobre e ferro. |

## Agricultura e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Foice de bronze | `bronze_scythe` | ferramenta (300 usos) | Bancada: 3 lingotes de bronze em arco, 2 gravetos. | Colhe plantas maduras em 3×3 e replanta automaticamente se houver sementes no inventário. |
| Barril de cura | `curing_barrel` | bloco (máquina) | Bancada: 6 tábuas, 2 placas de bronze, 1 barril vanilla. | Cura alimentos por tempo: queijo, carne curada. |
| Queijo | `cheese` | comida | Barril de cura: 1 balde de leite + 1 sal → 3 queijos em 5 min. | Sacia bem e dura; ingrediente de receitas futuras. |
| Carne curada | `cured_meat` | comida | Barril de cura: 4 carnes salgadas → 4 carnes curadas em 5 min. | Sacia 2× a carne cozida. |

## Fibras

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Tear de pedal | `pedal_loom` | bloco | Bancada: 4 tábuas, 2 engrenagens de bronze, 1 tear manual. | 4 fios → 2 tecidos de linho (2 cliques). |
| Vela de tecido | `cloth_sail` | material | Bancada: 6 tecidos de linho + 3 gravetos. | Pás do moinho de vento (era 6). |

## Máquinas

| Máquina | Entrada → saída | Tempo | Automação | Limite por chunk |
|---------|-----------------|-------|-----------|------------------|
| Barril de cura | leite + sal → queijo; carne salgada → curada | 5 min | funis | 32 |
| Tear de pedal | 4 fios → 2 tecidos | 2 cliques | nenhuma (manual) | 16 |

## Tarefas

- [ ] E3.1 Drop dos três minerais novos.
- [ ] E3.2 Bateia: interação com água, minério lavado, secundários e rejeito.
- [ ] E3.3 Estanho, arsênio, bronze e bronze arsenical.
- [ ] E3.4 Placas, varas e engrenagens de bronze.
- [ ] E3.5 Ferramentas e armaduras de bronze (tier de picareta entre cobre e ferro).
- [ ] E3.6 Foice de bronze com replantio.
- [ ] E3.7 Barril de cura, queijo e carne curada.
- [ ] E3.8 Tear de pedal e vela de tecido.
- [ ] E3.9 Texturas.
