# Era 22 — Antimatéria

**Inspiração:** futuro (2300–2400) · **Versão:** 2.22.x · **Status:** planejada

## Resumo

Um acelerador de partículas em anel produz antiprótons, guardados em armadilhas magnéticas. Nas
células de antimatéria, cada grama vale mais que qualquer combustível anterior; o reator de
antimatéria abre a rede UV e a propulsão que leva às estrelas.

## Desbloqueio

- Requer a era 21 e o Processador quântico.
- **Item-porta:** Célula de antimatéria.

## Energia: UV

Transformador ZPM→UV; reator de antimatéria.

## Itens

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Acelerador de partículas | `particle_accelerator_controller` | Multibloco em anel (diâmetro 15 a 31) de ímãs supercondutores + controlador (computador quântico). | Consome energia e hidrogênio; produz antiprótons (e matéria exótica na era 23). Anéis maiores produzem mais. |
| Ímã de acelerador | `accelerator_magnet` | Bancada: 4 bobinas supercondutoras, 1 criostato, 4 placas de aço inox. | Bloco do anel. |
| Armadilha de Penning | `penning_trap` | Bancada: 1 carcaça IV, 2 bobinas supercondutoras, 1 processador quântico. | Captura antiprótons e forma células de antimatéria. |
| Célula de antimatéria | `antimatter_cell` | Armadilha de Penning. | Combustível do reator e da propulsão. Item-porta. Quebrar uma célula cheia libera energia (sem destruir terreno). |
| Reator de antimatéria | `antimatter_reactor_controller` | Multibloco 5×5×5 de blindagem de metamaterial + controlador. | Células de antimatéria → até 65.536 SU/t (UV). |
| Propulsor de antimatéria | `antimatter_thruster` | Bancada: 1 célula de antimatéria, 4 placas de metamaterial, 1 processador quântico. | Motor das sondas interestelares (era 23). |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Acelerador | 32.768 SU/t | 1 anel por região de 4×4 chunks |
| Armadilha de Penning | 4.096 SU/t | 4 |
| Reator de antimatéria | produz até 65.536 SU/t | 1 |

## Tarefas

- [ ] E22.1 Acelerador em anel e ímãs.
- [ ] E22.2 Armadilha de Penning e células de antimatéria.
- [ ] E22.3 Reator de antimatéria e rede UV.
- [ ] E22.4 Propulsor de antimatéria.
- [ ] E22.5 Texturas.
