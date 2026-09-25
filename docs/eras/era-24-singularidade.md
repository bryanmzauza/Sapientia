# Era 24 — Singularidade

**Inspiração:** especulação (além de 2600) · **Versão:** 2.24.x · **Status:** planejada

## Resumo

O fim da linha do tempo. A transmutação transforma energia em qualquer elemento, a matéria
programável muda de forma sob comando, o sintetizador produz qualquer alimento e um enxame de Dyson
captura a energia do sol. O núcleo da singularidade é o troféu final.

## Desbloqueio

- Requer a era 23 e o Núcleo de dobra.
- Não tem item-porta: é a última era.

## Energia

Estelar: enxame de Dyson.

## Itens

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Transmutador | `transmuter` | Bancada: 1 carcaça IV, 2 núcleos de dobra, 1 computador quântico, 4 placas de neutrônio. | Energia + matéria UU → qualquer elemento ou metal escolhido. Custo proporcional à raridade. Fonte renovável de qualquer metal. |
| Matéria programável | `programmable_matter` | Transmutador + montador molecular. | Bloco que assume a forma de qualquer outro bloco sob comando de um computador. |
| Sintetizador de alimentos | `food_synthesizer` | Bancada: 1 carcaça IV, 1 transmutador, 1 bioimpressora. | Energia → qualquer comida, inclusive as do Sapientia. |
| Banco genético universal | `universal_seed_vault` | Bancada: 1 armazenamento quântico, 1 editor genético, 1 sintetizador. | Guarda e reproduz qualquer semente descoberta pelo jogador. |
| Segmento de Dyson | `dyson_segment` | Bancada: 8 placas de neutrônio, 16 painéis solares T3, 1 processador quântico. | Lançado por sonda; cada segmento em órbita soma energia a uma rede escolhida. |
| Receptor de Dyson | `dyson_receiver` | Bancada: 1 receptor de micro-ondas, 4 matérias exóticas. | Recebe a energia do enxame (65.536 SU/t por segmento). |
| Núcleo da singularidade | `singularity_core` | Bancada: 1 de cada item-porta de todas as eras + 1 matéria programável. | Troféu final: título e efeito visual para o jogador. |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Transmutador | conforme o elemento (de 10.000 a 10.000.000 SU por lingote) | 2 |
| Sintetizador de alimentos | 1.000 a 100.000 SU por comida | 4 |
| Receptor de Dyson | produz SU | 4 |

## Tarefas

- [ ] E24.1 Transmutador com custo por raridade.
- [ ] E24.2 Matéria programável controlada por computador.
- [ ] E24.3 Sintetizador de alimentos e banco genético universal.
- [ ] E24.4 Enxame de Dyson (segmentos e receptor).
- [ ] E24.5 Núcleo da singularidade.
- [ ] E24.6 Texturas.
