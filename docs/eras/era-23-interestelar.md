# Era 23 — Era Interestelar

**Inspiração:** ficção científica (2400–2600) · **Versão:** 2.23.x · **Status:** planejada

## Resumo

Com matéria exótica, o núcleo de dobra permite viajar entre sistemas estelares. Sondas trazem
neutrônio de estrelas mortas e sementes de plantas alienígenas, que viram uma nova fronteira da
agricultura.

## Desbloqueio

- Requer a era 22 e a Célula de antimatéria.
- **Item-porta:** Núcleo de dobra.

## Energia

UV, com reatores de antimatéria.

## Itens

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Matéria exótica | `exotic_matter` | Acelerador de partículas em modo de alta energia (anel de diâmetro 31). | Núcleo de dobra, blindagens. |
| Núcleo de dobra | `warp_core` | Bancada: 4 matérias exóticas, 2 células de antimatéria, 1 computador quântico, 4 placas de metamaterial. | Motor das sondas interestelares; portal de dobra. Item-porta. |
| Sonda interestelar | `interstellar_probe` | Plataforma de lançamento: núcleo de dobra + propulsor de antimatéria + kit científico (Bancada: 1 processador quântico, 1 contador Geiger, 1 câmera, 1 armazenamento quântico). | Missão de 2 h reais: traz neutrônio, matéria exótica ou sementes alienígenas, conforme o alvo escolhido. |
| Neutrônio | `neutronium` | Sonda interestelar (alvo: estrela de nêutrons). | O material mais denso do jogo. |
| Placa de neutrônio | `neutronium_plate` | Compressor: 9 neutrônios → 1. | Blindagem, armadura, blocos indestrutíveis. |
| Portal de dobra | `warp_gate` | Multibloco 5×5 de placas de neutrônio + núcleo de dobra. | Liga duas bases do mesmo jogador para viagem instantânea de jogadores (com custo de energia por viagem). |

## Agricultura alienígena

| Planta | Id | Como é obtida | Produtos |
|--------|----|---------------|----------|
| Fruta estelar | `star_fruit` | Sementes alienígenas (sonda). Só cresce em estufa com luz de cultivo. | Comida que dá Regeneração e Absorção. |
| Musgo luminoso | `lumen_moss` | Sementes alienígenas. | Luz sem energia; corante que brilha. |
| Cana de cristal | `crystal_cane` | Sementes alienígenas. | Fibra cristalina para metamateriais sem ouro. |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Portal de dobra | 1.000.000 SU por viagem | 1 |

## Tarefas

- [ ] E23.1 Matéria exótica no acelerador.
- [ ] E23.2 Núcleo de dobra e sonda interestelar (missões).
- [ ] E23.3 Neutrônio e placas de neutrônio.
- [ ] E23.4 Portal de dobra.
- [ ] E23.5 Plantas alienígenas.
- [ ] E23.6 Texturas.
