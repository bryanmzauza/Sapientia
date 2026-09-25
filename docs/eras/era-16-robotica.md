# Era 16 — Robótica

**Inspiração:** 2000–2020 · **Versão:** 2.16.x · **Status:** planejada

## Resumo

Os androides saem do laboratório: agricultores, lenhadores, mineradores, pescadores, açougueiros,
construtores, combatentes e comerciantes que agem no mundo real, programados por computador e
alimentados por baterias de íon-lítio. Drones sem entidade polinizam plantações.

## Desbloqueio

- Requer a era 15 e o Computador.
- **Item-porta:** Núcleo de IA.

## Energia

IV, com baterias de íon-lítio para androides e ferramentas.

## Componentes

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Cátodo NMC | `nmc_cathode` | material | Misturador: 1 níquel + 1 manganês + 1 cobalto + 1 lítio → 4. | Baterias de íon-lítio. |
| Bateria de íon-lítio | `li_ion_battery` | item | Bancada: 2 cátodos NMC, 2 grafites, 1 eletrólito (lítio + solvente), 1 placa de alumínio. | Guarda 500.000 SU; energia de androides e ferramentas. |
| Núcleo de IA | `ai_core` | componente | Bancada: 2 processadores T3, 2 RAM T3, 1 SSD, 1 relógio atômico. | Cérebro dos androides de nível 4. Item-porta. |
| SSD | `storage_ssd` | componente | Fotolitografia: wafers + circuito T3. | Armazenamento rápido. |
| Câmera | `camera_sensor` | componente | Bancada: 1 lente, 1 circuito T3, 1 LED. | Visão dos androides; sensor de presença na rede de dados. |

## Androides

Todos agem no mundo real e respeitam os limites (4 por chunk e o limite do servidor).

| Androide | Id | O que faz |
|----------|----|-----------|
| Agricultor | `android_farmer` | Colhe e replanta plantações maduras na sua área, reagindo ao amadurecimento. |
| Lenhador | `android_lumberjack` | Corta árvores inteiras e replanta mudas. |
| Minerador | `android_miner` | Minera veios e áreas definidos por programa, com as regras de terreno virgem. |
| Pescador | `android_fisherman` | Pesca em água próxima, com a tabela de pesca do vanilla. |
| Açougueiro | `android_butcher` | Abate animais adultos de um cercado, mantendo um mínimo para reprodução. |
| Construtor | `android_builder` | Constrói uma planta salva, bloco a bloco, com os blocos de um contêiner. |
| Combatente | `android_slayer` | Defende uma área contra monstros hostis. |
| Comerciante | `android_trader` | Negocia com aldeões próximos usando os itens de um contêiner. |

Receita geral: Bancada com 1 carcaça EV, 1 núcleo de IA (a partir do nível 4) ou processador T3,
1 bateria de íon-lítio e a ferramenta do ofício (enxada, machado, picareta, vara de pescar, espada,
andaime, esmeralda).

| Melhoria | Ids | Efeito por nível (1 a 4) |
|----------|-----|--------------------------|
| Chip de IA | `ai_chip_t1` a `ai_chip_t4` | Raio de trabalho: 4, 6, 9, 13 blocos |
| Chip de motor | `motor_chip_t1` a `motor_chip_t4` | Intervalo entre ações: 20, 14, 9, 5 ticks |
| Placa de blindagem | `armour_plate_t1` a `armour_plate_t4` | Vida: 100, 200, 400, 800 |
| Módulo de energia | `fuel_module_t1` a `fuel_module_t4` | Capacidade de bateria: 1×, 4×, 16×, 64× |

## Automação agrícola

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Estação de drones | `drone_station` | Bancada: 1 carcaça EV, 4 motores T3, 1 câmera, 1 bateria de íon-lítio. | Drones virtuais (sem entidades) polinizam um raio 16×16: +20 % de velocidade nas plantas. |
| Estação de recarga | `android_charger` | Bancada: 1 carregador, 1 carcaça EV, 1 processador T3. | Recarrega androides na área. |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Androides | bateria própria | 4 no total |
| Estação de drones | 64 SU/t | 2 |
| Estação de recarga | até 256 SU/t ao recarregar | 2 |

## Conteúdo atual reaproveitado

Os oito androides e as dezesseis melhorias, que deixam de gerar loot simulado e passam a agir no
mundo; o módulo de combustível vira módulo de energia; SSD.

## Tarefas

- [ ] E16.1 Cátodo NMC, bateria de íon-lítio, SSD, câmera, núcleo de IA.
- [ ] E16.2 Androides agindo no mundo real, com as regras de desempenho e terreno virgem.
- [ ] E16.3 Melhorias com os efeitos da tabela.
- [ ] E16.4 Estação de drones sem entidades; estação de recarga.
- [ ] E16.5 Texturas.
