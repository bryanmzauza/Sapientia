# Era 19 — Nanotecnologia

**Inspiração:** futuro (2080–2120) · **Versão:** 2.19.x · **Status:** planejada

## Resumo

A matéria passa a ser montada átomo por átomo. Grafeno, nanotubos e nanocelulose criam materiais
leves e ultrarresistentes; a separação molecular extrai 100 % de cada minério; o montador molecular
fabrica itens automaticamente; o replicador copia itens a partir de matéria bruta. No campo, a
edição genética cria sementes melhores e a bioimpressora produz carne cultivada.

## Desbloqueio

- Requer a era 18 e a Bobina supercondutora.
- **Item-porta:** Montador molecular.

## Energia

LuV, com o reator de fusão.

## Materiais

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Reator de deposição (CVD) | `cvd_reactor` | máquina LuV | Bancada: 1 carcaça IV, 1 reator químico, 2 bobinas supercondutoras. | Metano + folha de cobre → grafeno; metano + catalisador → nanotubos. |
| Grafeno | `graphene` | material | Reator de deposição. | Eletrônica, baterias e blindagens avançadas. |
| Nanotubos de carbono | `carbon_nanotubes` | material | Reator de deposição. | Fibra de nanotubos. |
| Fibra de nanotubos | `nanotube_fiber` | material | Fiadora industrial: nanotubos → fibra. | Cabos e tecidos ultrarresistentes. |
| Nanocelulose | `nanocellulose` | material | Reator de síntese: polpa de madeira + ácido → nanocelulose. | Estruturas leves (blocos e ferramentas). |
| Metamaterial | `metamaterial` | material | Montador molecular: grafeno + ouro + terras raras. | Camuflagem, lentes perfeitas, antenas. |
| Bateria de grafeno | `graphene_battery` | item | Bancada: 1 bateria de íon-lítio + 2 grafenos. | Guarda 5 milhões de SU; energia de ferramentas e armaduras avançadas. |
| Separador molecular | `molecular_separator` | máquina LuV | Bancada: 1 carcaça IV, 1 extração por solvente, 1 processador T3, 2 grafenos. | Rendimento 4 do principal, 100 % de secundários e traços. Reprocessa qualquer rejeito. |

## Fabricação

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Montador molecular | `molecular_assembler` | Bancada: 1 carcaça IV, 4 grafenos, 2 processadores T3, 1 computador. | Fabrica automaticamente qualquer receita da Bancada a partir de um padrão, com os ingredientes de contêineres ligados. Um evento por item fabricado. Item-porta. |
| Padrão | `pattern` | Montador molecular: 1 papel + 1 circuito T3. | Guarda uma receita. |
| Matéria UU | `uu_matter` | Replicador: sucata (do reciclador) + muita energia. | Matéria-prima universal. |
| Replicador | `replicator_controller` | Multibloco 7×7×3 + controlador (bobinas supercondutoras, montador molecular). | Matéria UU + padrão → cópia do item. Itens de fim de era, combustíveis nucleares, núcleos de IA e itens das eras 20 a 24 não podem ser replicados. |

## Agricultura, alimentos e medicina

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Editor genético | `gene_editor` | Bancada: 1 carcaça IV, 1 processador T3, 1 montador molecular. | Semente + característica → semente editada. |
| Semente editada | `edited_seeds` | Editor genético. | Características: rendimento +50 %, crescimento +30 %, cresce em qualquer bioma, resistente a pisoteio. Até duas por semente. |
| Bioimpressora | `bioprinter` | Bancada: 1 carcaça IV, 1 biorreator, 1 montador molecular. | Caldo nutritivo + células → carne cultivada. |
| Carne cultivada | `cultured_meat` | comida | Bioimpressora. | Sacia como carne cozida e dá Saturação por 10 s. |
| Nanomédicos | `nanomedic` | item | Montador molecular: grafeno + penicilina + processador. | Cura completa e remove todos os efeitos negativos. |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Reator de deposição | 2.048 SU/t | 4 |
| Separador molecular | 4.096 SU/t | 2 |
| Montador molecular | 512 SU/t por item | 16 |
| Replicador | 16.384 SU/t | 1 |
| Editor genético, bioimpressora | 1.024 SU/t | 4 cada |

## Tarefas

- [ ] E19.1 Reator de deposição, grafeno, nanotubos, fibra, nanocelulose, metamaterial.
- [ ] E19.2 Separador molecular.
- [ ] E19.3 Montador molecular e padrões (autocrafting por evento).
- [ ] E19.4 Matéria UU e replicador com lista de itens proibidos.
- [ ] E19.5 Editor genético, sementes editadas, bioimpressora, carne cultivada, nanomédicos.
- [ ] E19.6 Texturas.
