# Era 15 — Era da Informação

**Inspiração:** 1970–2000 · **Versão:** 2.15.x · **Status:** planejada

## Resumo

O computador vira o centro da base. Programas de lógica rodam em computadores ligados por cabos de
dados a sensores e máquinas. As terras raras dão os ímãs de neodímio dos motores eficientes, e a
agricultura de precisão controla irrigação e fertilização por programa.

## Desbloqueio

- Requer a era 14 e o Satélite.
- **Item-porta:** Computador.

## Energia: IV

Transformador EV→IV, cabos e capacitores IV.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Lepidolita | lítio | rubídio, césio | granito | diamante |
| Monazita | cério, lantânio, neodímio | tório | areia de praia, cascalho | diamante |
| Bastnasita | cério, lantânio, neodímio | — | calcita, pedra | diamante |
| Bórax (não metálico) | boro | — | areia vermelha, terracota (desertos) | diamante |

Rubídio, césio, cério, lantânio, neodímio, európio, rênio, ródio e rutênio passam a ser separados.

## Materiais

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Cascata de extração por solvente | `solvent_extraction_controller` | multibloco (1×8 de tanques + controlador) | Controlador — Bancada: 1 carcaça EV, 8 tanques de hastelloy, 1 processador T2. | Separa terras raras: 3 do principal, 95 % de secundários, 75 % de traços. |
| Ímã de neodímio | `neodymium_magnet` | componente | Forno de indução: 2 neodímio + 14 ferro + 1 boro → 17 (proporção simplificada); compressor → ímãs. | Motor T3, geradores eficientes, turbinas eólicas (era 17). |
| Motor T3, bobina T3, circuito T3, processador T3 | `motor_t3`, `coil_t3`, `circuit_t3`, `processor_t3` | componentes | Bancada: versão T2 + ímãs de neodímio, circuitos integrados e fios de ouro. | Máquinas IV, computadores. |
| RAM T2 e T3 | `ram_t2`, `ram_t3` | componentes | Fotolitografia: wafers + processadores. | Memória dos computadores. |
| HDD | `storage_hdd` | componente | Bancada: 1 motor T2, 2 placas de alumínio, 1 ímã de neodímio, 1 circuito T2. | Armazenamento de programas. |
| Relógio atômico | `atomic_clock` | componente | Bancada: 1 rubídio, 1 processador T3, 1 invar. | Sincroniza redes de computadores e melhora o GPS. |

## Computação

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Computador | `computer` | Bancada: 1 carcaça EV, 1 processador T3, 2 RAM T2, 1 HDD. | Executa programas de lógica (até 8 por computador). Item-porta. |
| Terminal | `terminal` | Bancada: 1 tela, 1 teclado (plástico + botões), 1 circuito T2. | Edita e acompanha programas. |
| Tela | `display_panel` | Bancada: 4 LEDs, 1 vidro, 1 circuito T2 → 2. | Mostra informações de redes e máquinas. |
| Cabo de dados | `data_cable` | Bancada: 3 fios de cobre + 1 plástico → 8. | Liga computadores, sensores e máquinas numa rede de dados. |
| Carcaça IV | `machine_casing_iv` | Bancada: 1 carcaça EV, 4 placas de liga de titânio, 2 motores T3, 1 processador T3. | Base das máquinas das eras 17 e 18. |
| Servidor | `server_rack` | Bancada: 4 computadores + 1 carcaça EV. | Executa até 64 programas. |

Os programas rodam só em computadores e servidores, e cada um tem um limite de instruções por
ciclo, o que mantém o custo da lógica previsível.

## Agricultura de precisão

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Sensor de solo | `soil_sensor` | Bancada: 1 circuito T2, 1 placa de cobre, 1 fio. | Informa à rede de dados umidade, fertilidade e estágio das plantas num raio 9×9. |
| Aspersor programável | `smart_sprinkler` | Bancada: 1 saída de irrigação, 1 circuito T2, 1 cano. | Irriga e aplica fertilizante ou solução nutritiva num raio 9×9 quando o programa mandar. |
| Estação meteorológica | `weather_station` | Bancada: 1 circuito T2, 1 lente, 1 moinho de vento. | Informa chuva, vento e hora à rede de dados. |
| Suplemento nutricional | `nutrition_supplement` | Misturador: farelo de soja + mel + ervas secas. | Sacia completamente e dá Saturação por 30 s. |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Extração por solvente | 1.024 SU/t | 1 |
| Computador | 32 SU/t | 8 |
| Servidor | 256 SU/t | 2 |
| Sensor de solo, aspersor, estação | 1 a 8 SU/t | 16 cada |

## Conteúdo atual reaproveitado

Componentes T3, RAM, HDD e os **programas de lógica**, que passam a rodar em computadores; sensor
comparador e sensor de nível entram na rede de dados.

## Tarefas

- [ ] E15.1 Lepidolita, monazita, bastnasita, bórax; extração por solvente.
- [ ] E15.2 Ímã de neodímio; componentes T3; RAM e HDD; relógio atômico.
- [ ] E15.3 Computador, terminal, tela, cabo de dados, servidor; programas de lógica nos computadores.
- [ ] E15.4 Sensor de solo, aspersor programável, estação meteorológica.
- [ ] E15.5 Rede IV.
- [ ] E15.6 Suplemento nutricional.
- [ ] E15.7 Texturas.
