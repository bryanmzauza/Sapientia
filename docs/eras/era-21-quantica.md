# Era 21 — Era Quântica

**Inspiração:** futuro (2200–2300) · **Versão:** 2.21.x · **Status:** planejada

## Resumo

Qubits supercondutores resfriados a quase zero absoluto formam o processador quântico. Com ele vêm
o teletransporte de itens, o armazenamento quântico (milhões de itens num bloco) e os equipamentos
quânticos, o conjunto de ferramentas e armadura de fim de jogo.

## Desbloqueio

- Requer a era 20 e o Módulo orbital.
- **Item-porta:** Processador quântico.

## Energia

ZPM. O extrator de ponto zero é a fonte conceitual da era.

## Itens

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Criostato de diluição | `dilution_cryostat` | Bancada: 1 criostato, 1 hélio-3, 4 placas de ouro, 1 processador T3. | Resfria qubits. |
| Qubit | `qubit` | Fotolitografia: 1 fita supercondutora + 1 lingote de nióbio → 4. | Processador quântico. |
| Processador quântico | `quantum_processor` | Bancada: 8 qubits, 1 criostato de diluição, 1 metamaterial. | Item-porta. |
| Computador quântico | `quantum_computer` | Bancada: 1 carcaça IV, 2 processadores quânticos, 1 servidor. | Executa programas com limite de instruções 16× maior e otimiza redes de itens inteiras. |
| Teletransportador de itens | `quantum_teleporter` | Bancada: 1 carcaça IV, 1 processador quântico, 4 metamateriais. | Pares ligados enviam itens instantaneamente a qualquer distância, até entre dimensões. |
| Armazenamento quântico | `quantum_storage` | Bancada: 1 carcaça IV, 1 processador quântico, 4 grafenos. | Guarda até 2 bilhões de itens de um único tipo num bloco. |
| Extrator de ponto zero | `zero_point_extractor` | Bancada: 1 carcaça IV, 2 processadores quânticos, 4 bobinas supercondutoras. | 2.048 SU/t contínuos, sem combustível. |

## Equipamentos quânticos

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Armadura quântica | `quantum_helmet`, `quantum_chestplate`, `quantum_leggings`, `quantum_boots` | Bancada: placas de metamaterial, nanotubos, processador quântico, bateria de grafeno. | Proteção máxima; com o conjunto: voo, visão noturna, imunidade a radiação e quedas. Consome SU. |
| Broca quântica | `quantum_drill` | Bancada: carboneto de tungstênio, processador quântico, bateria de grafeno. | Quebra 5×5 instantaneamente; aplica as regras de terreno virgem. |
| Motosserra quântica | `quantum_chainsaw` | Idem, com lâmina de nanotubos. | Corta árvores inteiras de uma vez. |
| Multiferramenta quântica | `quantum_multitool` | Bancada: broca + motosserra + chave inglesa quânticas. | Funciona como qualquer ferramenta. |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Computador quântico | 1.024 SU/t | 2 |
| Teletransportador | 256 SU/t por pilha enviada | 8 |
| Armazenamento quântico | 16 SU/t | 16 |
| Extrator de ponto zero | produz 2.048 SU/t | 4 |

## Tarefas

- [ ] E21.1 Criostato de diluição, qubit, processador quântico.
- [ ] E21.2 Computador quântico.
- [ ] E21.3 Teletransportador e armazenamento quântico.
- [ ] E21.4 Extrator de ponto zero.
- [ ] E21.5 Armadura e ferramentas quânticas.
- [ ] E21.6 Texturas.
