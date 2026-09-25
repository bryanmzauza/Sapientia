# Era 20 — Era Orbital

**Inspiração:** futuro (2120–2200) · **Versão:** 2.20.x · **Status:** planejada

## Resumo

A indústria sai do planeta. Estações orbitais coordenam missões: mineração de asteroides, coleta de
hélio-3 na Lua e fazendas em órbita. Satélites solares enviam energia para a superfície. É a
primeira fonte renovável de minério em grande escala.

## Desbloqueio

- Requer a era 19 e o Montador molecular.
- **Item-porta:** Módulo orbital.

## Energia: ZPM

Transformador LuV→ZPM. Satélites solares transmitem energia a receptores no chão.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Traços | Onde cai |
|---------|-----------|-------------|--------|----------|
| Fragmento de meteorito | ferro, níquel | cobalto | irídio, platina | pedra do End |

## Estação orbital e missões

As missões não criam dimensões nem entidades: são processos com tempo real, que consomem recursos
no lançamento e entregam a carga depois.

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Módulo orbital | `orbital_module` | Bancada: 8 placas de liga de titânio, 1 computador, 2 células solares avançadas, 1 criostato. | Lançado por foguete, amplia a estação orbital do jogador. Item-porta. |
| Estação orbital | `orbital_station_terminal` | Terminal no chão — Bancada: 1 carcaça IV, 1 terminal, 1 transmissor GPS. | Mostra os módulos em órbita e as missões ativas. Cada módulo permite uma missão ao mesmo tempo. |
| Foguete reutilizável | `reusable_rocket` | Plataforma: 2 motores de foguete, 4 tanques de foguete, 1 aviônica, 4 placas de grafeno. | Volta após a missão; exige revisão (peças de reposição) a cada 5 voos. |
| Missão de asteroide | `asteroid_mission` | Terminal: foguete reutilizável + combustível + 1 kit de mineração. | Após 30 min reais, entrega caixas de minério de asteroide: ferro-níquel, platina, paládio, irídio, ósmio, ródio. |
| Missão lunar | `lunar_mission` | Terminal: foguete reutilizável + combustível + 1 kit de coleta. | Após 45 min reais, entrega regolito lunar com hélio-3. |
| Kit de mineração e de coleta | `mining_kit`, `collection_kit` | Bancada: brocas de carboneto de tungstênio, processador T3, contêineres. | Consumidos na missão. |
| Hélio-3 | `helium3` | Separador molecular: regolito lunar. | Combustível de fusão avançada (2× a energia do trítio, sem nêutrons). |

## Energia e agricultura

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Satélite solar | `solar_satellite` | Bancada: 16 painéis solares T3, 1 módulo orbital, 1 transmissor de micro-ondas. | Em órbita, envia energia a um receptor do mesmo jogador. |
| Receptor de micro-ondas | `rectenna` | Bancada: 1 carcaça IV, 8 fibras de nanotubos, 1 processador T3. | Recebe até 4.096 SU/t de um satélite solar (ZPM). |
| Fazenda orbital | `orbital_farm_module` | Módulo orbital + fazenda vertical. | Entrega periodicamente comida e fibras escolhidas (renovável, sem ocupar espaço no mundo). |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Terminal da estação | 64 SU/t | 1 |
| Receptor de micro-ondas | produz até 4.096 SU/t | 2 |

## Tarefas

- [ ] E20.1 Fragmento de meteorito na pedra do End.
- [ ] E20.2 Módulo orbital, terminal e estação (missões como processos com tempo real).
- [ ] E20.3 Foguete reutilizável, missões de asteroide e lunar, kits, hélio-3.
- [ ] E20.4 Satélite solar e receptor; rede ZPM.
- [ ] E20.5 Fazenda orbital.
- [ ] E20.6 Texturas.
