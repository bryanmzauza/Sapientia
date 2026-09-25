# Era 7 — Renascença e Navegações

**Inspiração:** 1450–1750 · **Versão:** 2.7.x · **Status:** planejada

## Resumo

A precisão muda tudo: engrenagens finas, molas e parafusos formam o mecanismo de precisão, base
das máquinas a vapor e elétricas. O vidro óptico dá lentes para instrumentos que encontram veios. A
imprensa espalha conhecimento entre jogadores. As plantas do Novo Mundo chegam, e as árvores passam
a dar resina e látex.

## Desbloqueio

- Requer a era 6 e o Alto-forno.
- **Item-porta:** Mecanismo de precisão.

## Energia

Mecânica local (UM).

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Cobaltita | cobalto | arsênio | ardósia profunda (Y −50 a 0) | ferro |
| Platina nativa | platina | paládio, irídio | cascalho de rio; ardósia profunda abaixo de Y −40 | ferro |
| Betume (não metálico) | betume | — | areia vermelha, terracota (desertos) | ferro |

## Precisão e instrumentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Torno mecânico | `lathe` | máquina (2 UM) | Bancada: 2 lingotes de ferro, 2 engrenagens de bronze, 1 vara de aço de Damasco, 4 tábuas. | 1 lingote → 2 varas; 1 vara → 4 parafusos; 1 placa de latão → 2 engrenagens finas. |
| Parafuso de latão | `brass_screw` | material | Torno. | Mecanismos e instrumentos. |
| Engrenagem fina de latão | `brass_fine_gear` | material | Torno. | Mecanismo de precisão. |
| Mola de aço | `steel_spring` | material | Bancada: 1 vara de aço de Damasco + martelo → 2. | Mecanismo de precisão. |
| Mecanismo de precisão | `precision_mechanism` | material | Bancada: 4 engrenagens finas de latão, 2 molas de aço, 2 parafusos de latão, 1 placa de electrum. | Base das máquinas a vapor e elétricas. Item-porta. |
| Forno de vidreiro | `glass_furnace` | máquina (combustível) | Bancada: 8 tijolos refratários + 1 forno de argila. | 4 areias + 1 potassa + 1 pó de manganês → 4 vidros ópticos em 20 s. |
| Vidro óptico | `optical_glass` | material | Forno de vidreiro. | Lentes. |
| Lente | `lens` | material | Bancada: 1 vidro óptico + martelo → 2. | Instrumentos; laser (era 12). |
| Bússola geológica | `geologist_compass` | ferramenta | Bancada: 1 bússola vanilla, 2 lentes, 1 lingote de electrum, 1 mecanismo de precisão. | Escolha um mineral; aponta para o veio mais próximo dele num raio de 3 chunks. |
| Lupa de geólogo | `geologist_loupe` | ferramenta | Bancada: 1 lente, 2 lingotes de latão. | Clique com um fragmento: mostra a composição (principal, secundários e traços). |

## Metalurgia e química

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Triturados da era | `crushed_cobalt`, `crushed_platinum` | material | Martelo ou pilão. | Cobalto: forno de argila → lingote; platina: água-régia. |
| Lingote de cobalto | `cobalt_ingot` | material | Forno de argila. | Pigmento azul; superligas (era 14). |
| Azul de cobalto | `cobalt_blue` | material | Bancada: 1 pó de cobalto + 1 vidro → 4. | Tinta, vidro azul decorativo. |
| Água-régia | `aqua_regia` | fluido (garrafa) | Bancada: 1 ácido nítrico + 3 ácidos clorídricos. | Dissolve platina e ouro. |
| Lingote de platina | `platinum_ingot` | material | Bancada: 4 platinas trituradas + 1 água-régia → sal de platina; forno de argila → lingote. Paládio e irídio vão para o rejeito. | Contatos, termopares, cadinhos de laboratório (era 11). |
| Liga de tipos | `type_metal` | material | Forno de argila: mistura de 2 chumbos, 1 estanho e 1 antimônio triturados. | Tipos móveis da imprensa. |
| Imprensa | `printing_press` | máquina (1 UM) | Bancada: 4 tábuas, 2 lingotes de ferro, 1 mecanismo de precisão, 2 ligas de tipos. | Livro técnico: 1 livro + 1 tinta de impressão + a categoria escolhida → cópia das descobertas do jogador que opera a imprensa. |
| Tinta de impressão | `printing_ink` | material | Bancada: 1 carvão + 1 óleo de linhaça → 4. | Imprensa. |
| Livro técnico | `technical_book` | item | Imprensa. | Ler desbloqueia as receitas descobertas por quem imprimiu, dentro das eras liberadas. |
| Sabão | `soap` | item | Barril de cura: 1 potassa + 1 óleo de linhaça → 4 em 5 min. | Remove efeitos negativos ao usar; limpeza de minérios na flotação (era 10). |

## Agricultura, madeira e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Sementes de milho | `corn_seeds` | semente | Faca de sílex em selvas; colheita. | Terra arada. |
| Milho | `corn` | produto | Colheita (visual de trigo alto). | Farinha de milho, pipoca, ração (era 10), etanol (era 11). |
| Sementes de algodão | `cotton_seeds` | semente | Faca de sílex em savanas e desertos; colheita. | Terra arada. |
| Capulho de algodão | `cotton_boll` | produto | Colheita. | Descaroçador (era 8) → fibra e sementes; à mão: 1 fibra por capulho. |
| Muda de seringueira | `rubber_sapling` | muda | 2 % de chance em folhas de selva quebradas. | Cresce como árvore (visual de árvore de selva pequena). |
| Prensa de óleo | `oil_press` | máquina (1 UM) | Bancada: 4 tábuas, 1 lagar, 2 placas de ferro. | 4 sementes de linho ou algodão → 1 garrafa de óleo. |
| Óleo de linhaça | `linseed_oil` | material | Prensa de óleo. | Tinta, sabão, lubrificante: máquinas mecânicas lubrificadas trabalham 10 % mais rápido por 1 hora. |
| Torneira | `tree_tap` | bloco | Bancada: 2 lingotes de ferro, 1 placa de bronze. | Em tronco natural de pinheiro: 1 resina a cada 5 min; em seringueira natural: 1 látex a cada 5 min. Troncos colocados não produzem. |
| Resina | `resin` | material | Torneira em pinheiro. | Alambique → breu + terebintina. |
| Látex | `latex` | material | Torneira em seringueira. | Borracha (era 8). |
| Breu | `pitch` | material | Alambique: 4 resinas, ou forno de argila: 1 betume. | Impermeabilização. |
| Barril alcatroado | `tarred_barrel` | bloco | Bancada: 6 tábuas + 2 breus + 1 barril. | Guarda 8 baldes de um líquido e pode ser carregado com o conteúdo. |
| Chocolate | `chocolate` | comida | Bancada: 2 cacaus + 1 açúcar + 1 balde de leite → 4. | Velocidade I por 20 s. |
| Pipoca | `popcorn` | comida | Fogueira ou fornalha: milho. | Lanche leve. |
| Tortilha | `tortilla` | comida | Fornalha: massa de farinha de milho. | Sacia como pão de farinha. |

## Máquinas

| Máquina | Entrada → saída | Tempo | Força | Limite por chunk |
|---------|-----------------|-------|-------|------------------|
| Torno mecânico | lingote → varas; vara → parafusos; placa → engrenagens finas | 5 s | 2 UM | 16 |
| Forno de vidreiro | areia + potassa + manganês → vidro óptico | 20 s | combustível | 8 |
| Imprensa | livro + tinta → livro técnico | 30 s | 1 UM | 4 |
| Prensa de óleo | 4 sementes → 1 óleo | 10 s | 1 UM | 16 |
| Torneira | — → resina ou látex | 5 min (evento agendado) | — | 32 |

## Tarefas

- [ ] E7.1 Torno, parafusos, engrenagens finas, molas e mecanismo de precisão.
- [ ] E7.2 Forno de vidreiro, vidro óptico, lentes; bússola e lupa de geólogo (veios).
- [ ] E7.3 Cobaltita, platina nativa, betume; água-régia e platina; azul de cobalto.
- [ ] E7.4 Liga de tipos, imprensa, tinta e livros técnicos (compartilhamento de descobertas).
- [ ] E7.5 Milho, algodão, seringueira; prensa de óleo, óleo de linhaça e lubrificação.
- [ ] E7.6 Torneira em troncos naturais; resina, látex, breu, barril alcatroado.
- [ ] E7.7 Sabão; chocolate, pipoca, tortilha.
- [ ] E7.8 Texturas.
