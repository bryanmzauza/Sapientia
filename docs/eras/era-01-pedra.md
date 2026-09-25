# Era 1 — Idade da Pedra

**Inspiração:** Neolítico e a revolução agrícola · **Versão:** 2.1.x · **Status:** planejada

## Resumo

A era em que a humanidade passou a plantar. O jogador coleta sementes silvestres, cultiva linho e
ervas, mói grãos para fazer pão, produz carvão vegetal na carvoaria e constrói o primeiro forno
capaz de fundir metal. Quebrar rocha ainda não dá minerais (os primeiros são da era 2).

## Desbloqueio

- Requer a era 0 e a Bancada Sapientia.
- **Item-porta:** Forno de argila.

## Energia

Nenhuma rede. Fornos e carvoaria queimam combustível diretamente.

## Agricultura e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Faca de sílex | `flint_knife` | ferramenta (64 usos) | Bancada: 1 sílex + 1 graveto. | Quebrando grama e samambaia: 30 % de chance de fibra vegetal e 10 % de semente silvestre (linho em planícies, erva medicinal em florestas). |
| Fibra vegetal | `plant_fiber` | material | Faca de sílex em grama e samambaia. | 3 fibras → 1 barbante. Cestos e cordas. |
| Sementes de linho | `flax_seeds` | semente | Faca de sílex em planícies; colheita do linho. | Plantadas em terra arada. |
| Linho | `flax` | planta e produto | Colheita (visual de trigo no mundo, 8 estágios). | Fibra de linho (tecido na era 2) e sementes. |
| Sementes de erva medicinal | `herb_seeds` | semente | Faca de sílex em florestas; colheita. | Plantadas em terra arada. |
| Erva medicinal | `medicinal_herb` | planta e produto | Colheita (visual de beterraba no mundo). | Chá medicinal, pomada curativa. |
| Mó manual | `hand_quern` | bloco | Bancada: 2 pedras lisas + 1 graveto. | Clique com grãos na mão: 1 trigo → 1 farinha (3 cliques). Aceita arroz a partir da era 2. |
| Farinha | `flour` | material | Mó manual. | Massa de pão. |
| Massa de pão | `bread_dough` | material | Bancada: 3 farinhas + 1 balde de água → 3. | Fornalha vanilla → pão de farinha. |
| Pão de farinha | `flour_bread` | comida | Fornalha vanilla. | Sacia 1,5× o pão vanilla. |
| Chá medicinal | `herbal_tea` | bebida | Bancada: 2 ervas medicinais + 1 frasco de água. | Regeneração I por 10 s. |
| Pomada curativa | `healing_salve` | item | Bancada: 3 ervas medicinais + 1 bola de argila → 2. | Clique para recuperar 3 corações (cooldown de 30 s). |
| Composto | `compost` | material | Composteira vanilla com plantas do Sapientia, ou Bancada: 4 folhas + 1 terra. | Aplicado numa planta, adianta 1 estágio num raio 3×3. |

## Madeira e carvão

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Carvoaria | `charcoal_pit` | bloco (máquina) | Bancada: 4 terras, 4 pedregulhos, 1 fogueira. | Queima lenta: 16 toras → 24 carvões vegetais + 4 cinzas de madeira em 4 min. |
| Cinza de madeira | `wood_ash` | material | Carvoaria (e fogueiras). | Lixívia; adubo de cinzas (era 2). |

## Água e químicos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Balde de lixívia | `lye_bucket` | fluido em balde | Bancada: 4 cinzas de madeira + 1 balde de água. | Fornalha vanilla → potassa. |
| Potassa | `potash` | material | Fornalha vanilla com lixívia. | Adubo (era 2), sabão e vidro (era 7). |

## Metalurgia e ferramentas

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Martelo de pedra | `stone_hammer` | ferramenta de bancada (64 usos) | Bancada: 2 pedregulhos (em cima), 1 graveto, 1 fibra vegetal. | Fragmento de mineral → minério triturado. Pedregulho → cascalho → areia. |
| Argila refratária | `fire_clay` | material | Bancada: 4 bolas de argila + 4 cascalhos → 8. | Fornalha vanilla → tijolo refratário. |
| Tijolo refratário | `fire_brick` | material | Fornalha vanilla. | Fornos até a era 8. |
| Forno de argila | `clay_furnace` | bloco (máquina) | Bancada: 8 tijolos refratários em volta de 1 fornalha vanilla. | Funde minérios triturados e misturas de liga das eras 2 a 4. Item-porta. |
| Cesto de fibra | `fiber_basket` | bloco | Bancada: 8 fibras vegetais em volta de 1 baú. | Baú de 9 espaços que mantém o conteúdo quando quebrado. |

## Máquinas

| Máquina | Entrada → saída | Tempo | Combustível | Automação | Limite por chunk |
|---------|-----------------|-------|-------------|-----------|------------------|
| Forno de argila | minério triturado ou mistura → lingote (1:1, sem subprodutos) | 20 s | carvão vegetal, carvão (madeira não serve) | funis | 16 |
| Carvoaria | 16 toras → 24 carvões vegetais + 4 cinzas | 4 min | a própria madeira | funis | 8 |
| Mó manual | trigo ou arroz → farinha | 3 cliques | — | nenhuma (manual) | 16 |

## Tarefas

- [ ] E1.1 Faca de sílex, fibra vegetal e drop de sementes silvestres por bioma.
- [ ] E1.2 Plantas do Sapientia sobre blocos de planta vanilla: linho e erva medicinal.
- [ ] E1.3 Mó manual, farinha, massa e pão; chá medicinal e pomada curativa; composto.
- [ ] E1.4 Carvoaria, cinza, lixívia e potassa.
- [ ] E1.5 Martelo de pedra como ferramenta de bancada.
- [ ] E1.6 Argila e tijolo refratários; forno de argila com interface Java e Bedrock.
- [ ] E1.7 Cesto de fibra.
- [ ] E1.8 Texturas.
