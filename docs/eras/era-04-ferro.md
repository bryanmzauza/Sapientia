# Era 4 — Idade do Ferro

**Inspiração:** 1200–500 a.C. · **Versão:** 2.4.x · **Status:** planejada

## Resumo

O ferro exige o forno de lupa e o fole trabalhando. A galena traz o chumbo e, pela copelação, a
prata escondida nele; o cinábrio dá o mercúrio. O forno de cal abre a química da construção e
corrige o solo; o arado de ferro amplia as plantações.

## Desbloqueio

- Requer a era 3 e o Lingote de bronze.
- **Item-porta:** Lupa de ferro.

## Energia

Nenhuma rede. O fole é acionado à mão.

## Minerais que passam a cair

| Mineral | Principal | Secundários | Onde cai | Picareta mínima |
|---------|-----------|-------------|----------|-----------------|
| Hematita | ferro | — | pedra, terracota (Y −10 a 80) | bronze |
| Magnetita | ferro | titânio | pedra, diorito (Y −40 a 40) | bronze |
| Galena | chumbo | prata | pedra, calcita (Y −20 a 40) | bronze |
| Cinábrio | mercúrio | — | netherrack, basalto (Nether) | bronze |

O minério de ferro vanilla passa a render como hematita.

## Metalurgia

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Fragmentos da era | `hematite_fragment`, `magnetite_fragment`, `galena_fragment`, `cinnabar_fragment` | mineral | Rocha virgem. | Martelo → triturado. |
| Minério de ferro triturado | `crushed_iron` | material | Martelo + hematita, magnetita ou ferro bruto vanilla. | Forno de lupa → lupa de ferro. |
| Galena triturada | `crushed_galena` | material | Martelo + galena. | Forno de argila → chumbo argentífero. |
| Cinábrio triturado | `crushed_cinnabar` | material | Martelo + cinábrio. | Forno de argila com frasco de vidro → frasco de mercúrio. |
| Fole | `bellows` | bloco | Bancada: 3 tábuas, 2 couros, 1 placa de bronze, 1 vara de bronze. | Encostado num forno de lupa: cada clique mantém a temperatura por 10 s. |
| Forno de lupa | `bloomery` | bloco (máquina) | Bancada: 7 tijolos refratários, 1 forno de argila, 1 placa de bronze. | Minério de ferro → lupa de ferro. |
| Lupa de ferro | `iron_bloom` | material | Forno de lupa. | Bancada: lupa + martelo → 2 lingotes de ferro. Item-porta. |
| Lingote de ferro | vanilla | material | Lupa martelada, ou fornalha vanilla com ferro bruto. | Ferramentas vanilla de ferro, arado, máquinas futuras. |
| Chumbo argentífero | `argentiferous_lead` | material | Forno de argila: galena triturada. | Copela → chumbo + prata. |
| Copela | `cupel` | bloco (máquina) | Bancada: 4 pós de osso, 4 argilas refratárias, 1 forno de argila. | 4 chumbos argentíferos + 1 carvão → 3 chumbos + 1 prata em 40 s. |
| Lingote de chumbo | `lead_ingot` | material | Copela, ou forno de argila com galena lavada. | Canos (era 5), peltre, blindagem (era 13). |
| Frasco de mercúrio | `mercury_flask` | material | Forno de argila: cinábrio triturado + frasco de vidro. | Amálgama na bateia; instrumentos (era 7). |
| Amálgama (uso da bateia) | — | — | Bateia com frasco de mercúrio na outra mão. | Recupera 50 % dos traços de ouro do minério lavado (1 frasco a cada 16 lavagens). |
| Martelo de ferro | `iron_hammer` | ferramenta de bancada (256 usos) | Bancada: 3 lingotes de ferro, 2 gravetos. | Como o de cobre, com 35 % de chance de rendimento duplo. |

## Agricultura e alimentos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Arado de ferro | `iron_plow` | ferramenta (400 usos) | Bancada: 3 lingotes de ferro, 1 tábua, 2 gravetos. | Ara 5 blocos em linha. |
| Adubo de cal | `lime_fertilizer` | material | Bancada: 1 cal virgem + 2 adubos de cinzas → 3. | Aplicado na terra arada: +25 % de velocidade de crescimento naquele bloco por 1 dia de jogo. |

## Água e químicos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Forno de cal | `lime_kiln` | bloco (máquina) | Bancada: 8 tijolos refratários em volta de 1 fornalha vanilla + 1 placa de bronze. | Calcita (vanilla) → cal virgem, com carvão, em 30 s. |
| Cal virgem | `quicklime` | material | Forno de cal. | Argamassa, adubo de cal, fundente do alto-forno (era 6), concreto romano (era 5). |
| Argamassa | `mortar` | material | Bancada: 1 cal virgem + 2 areias + 1 balde de água → 4. | Tijolos de pedra reforçados (blocos decorativos resistentes a explosões). |

## Máquinas

| Máquina | Entrada → saída | Tempo | Condição | Automação | Limite por chunk |
|---------|-----------------|-------|----------|-----------|------------------|
| Forno de lupa | 4 minérios de ferro + 4 carvões → 1 lupa (2 lingotes) | 60 s | fole ativo encostado; sem fole por 10 s, o tempo pausa | funis (o fole automático chega na era 5) | 8 |
| Copela | 4 chumbos argentíferos + 1 carvão → 3 chumbos + 1 prata | 40 s | — | funis | 8 |
| Forno de cal | calcita + carvão → cal virgem | 30 s | — | funis | 8 |

## Tarefas

- [ ] E4.1 Drop dos quatro minerais novos; ferro bruto vanilla como hematita.
- [ ] E4.2 Fole e forno de lupa com temperatura mantida por cliques.
- [ ] E4.3 Lupa de ferro martelada em lingotes.
- [ ] E4.4 Chumbo argentífero e copela.
- [ ] E4.5 Mercúrio e amálgama na bateia.
- [ ] E4.6 Martelo de ferro e arado de ferro.
- [ ] E4.7 Forno de cal, cal virgem, argamassa e adubo de cal.
- [ ] E4.8 Texturas.
