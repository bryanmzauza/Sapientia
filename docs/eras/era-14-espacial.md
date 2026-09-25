# Era 14 — Era Espacial

**Inspiração:** 1957–1975 · **Versão:** 2.14.x · **Status:** planejada

## Resumo

O titânio sai do rejeito e vira estrutura de foguete. A plataforma de lançamento coloca satélites
em órbita, que dão GPS e mapas de veios em grande escala. O RTG usa o plutônio da era atômica, e a
tuneladora escava túneis sozinha.

## Desbloqueio

- Requer a era 13 e a Barra de combustível.
- **Item-porta:** Satélite.

## Energia

EV. O RTG gera energia contínua sem combustível sólido.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Rutilo | titânio | — | areia, pedra | diamante |
| Coltan | nióbio | tântalo | granito, cascalho | diamante |

Titânio (também do rejeito de ilmenita e magnetita), nióbio, tântalo, irídio e ósmio passam a ser
separados.

## Metalurgia aeroespacial

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Reator Kroll | `kroll_reactor` | máquina EV | Bancada: 1 carcaça HV, 4 placas de aço inox, 1 reator químico. | Pó de rutilo ou ilmenita + cloro + coque + magnésio → esponja de titânio. |
| Esponja de titânio | `titanium_sponge` | material | Reator Kroll. | Forno a arco → lingote de titânio. |
| Lingote de titânio | `titanium_ingot` | material | Forno a arco. | Foguetes, ferramentas leves, ligas. |
| Liga de titânio | `titanium_alloy_ingot` | material | Forno de indução: 18 titânio + 1 alumínio + 1 vanádio → 20. | Estruturas de foguete. |
| Inconel | `inconel_ingot` | material | Forno de indução: 7 níquel + 2 cromo + 1 ferro → 10. | Motores de foguete. |
| Hastelloy | `hastelloy_ingot` | material | Forno de indução: 6 níquel + 2 molibdênio + 2 cromo → 10. | Tanques de químicos agressivos. |
| Nióbio, tântalo, irídio, ósmio | `niobium_ingot`, `tantalum_ingot`, `iridium_ingot`, `osmium_ingot` | material | Tanque de lixiviação e refino dos rejeitos de platina. | Supercondutores (era 18), capacitores, contatos. |

## Espaço

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Plataforma de lançamento | `launch_pad_controller` | Multibloco 5×5 de concreto armado + torre + controlador (Bancada: 1 carcaça HV, 2 processadores T2, 4 blocos de aço inox). | Monta e lança foguetes. |
| Motor de foguete | `rocket_engine` | Bancada: 4 placas de inconel, 2 canos pressurizados, 1 processador T2. | Peça do foguete. |
| Tanque de foguete | `rocket_tank` | Bancada: 8 placas de liga de titânio → 1. | Guarda oxigênio e hidrogênio líquidos. |
| Aviônica | `avionics` | Bancada: 2 processadores T2, 1 bússola geológica, 1 bateria. | Peça do foguete e do satélite. |
| Foguete | `rocket` | Plataforma: 1 motor, 4 tanques cheios (oxigênio e hidrogênio líquidos), 1 aviônica, 1 carga. | Leva a carga à órbita; é consumido no lançamento. |
| Satélite | `satellite` | Bancada: 4 placas de liga de titânio, 2 células solares T1, 1 aviônica, 1 módulo de missão. | Em órbita, cumpre a missão do módulo. Item-porta. |
| Célula solar T1 | `solar_cell_t1` | Fotolitografia: 1 wafer → 2. | Energia dos satélites. |
| Módulo de missão: GPS | `gps_module` | Bancada: 1 processador T2, 1 relógio, 1 transmissor GPS. | Cobertura GPS no mundo inteiro. |
| Módulo de missão: prospecção | `survey_module` | Bancada: 1 processador T2, 4 lentes, 1 prospector. | Mapa de veios num raio de 32 chunks de onde o foguete foi lançado. |
| Transmissor, marcador e mapa GPS | `gps_transmitter`, `gps_marker`, `gps_handheld_map` | Bancada: carcaça HV, processadores e lentes. | O transmissor é a estação terrestre; o mapa mostra marcadores e jogadores dentro da cobertura. |

## Energia e mineração

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Cápsula de plutônio | `plutonium_capsule` | Bancada: 1 plutônio + 4 placas de irídio. | Combustível do RTG, dura 20 dias de jogo. |
| RTG | `rtg` | Bancada: 1 carcaça HV, 1 cápsula de plutônio, 4 termopares (constantan). | 64 SU/t contínuos enquanto a cápsula durar. |
| Tuneladora | `tunnel_boring_machine` | Multibloco 5×5×3 + controlador (Bancada: 1 carcaça HV, 4 brocas de aço rápido, 2 processadores T2, 1 motor T2). | Avança sozinha, escavando um túnel 5×5 (1 bloco a cada 10 s, até 256 blocos), e coloca suportes de concreto. Aplica as regras de terreno virgem. |

## Máquinas

| Máquina | Consumo | Tempo | Limite por chunk |
|---------|---------|-------|------------------|
| Reator Kroll | 512 SU/t | 30 s | 4 |
| Plataforma de lançamento | 1.024 SU/t durante a montagem | 5 min por foguete | 1 |
| RTG | produz 64 SU/t | contínuo | 8 |
| Tuneladora | 1.024 SU/t | 10 s por bloco | 1 |

## Conteúdo atual reaproveitado

Titânio (`titanium_*`), RTG (passa a usar cápsula de plutônio), transmissor, marcador e mapa GPS
(passam a funcionar com cobertura real).

## Tarefas

- [ ] E14.1 Rutilo, coltan; separação de titânio, nióbio, tântalo, irídio e ósmio.
- [ ] E14.2 Reator Kroll, titânio, liga de titânio, Inconel, Hastelloy.
- [ ] E14.3 Plataforma, motor, tanques, aviônica, foguete.
- [ ] E14.4 Satélite com módulos de GPS e prospecção.
- [ ] E14.5 GPS funcionando com cobertura.
- [ ] E14.6 Cápsula de plutônio e RTG.
- [ ] E14.7 Tuneladora com regras de terreno virgem.
- [ ] E14.8 Texturas.
