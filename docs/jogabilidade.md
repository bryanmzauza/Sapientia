# Jogabilidade do Sapientia

**Status:** proposta para revisão. Este documento define as regras gerais de como o jogador
progride no Sapientia. O conteúdo de cada era (itens, receitas, máquinas e tarefas) está em
[`eras/`](eras/README.md), e o desenvolvimento do plugin é feito era por era. As decisões em aberto
estão na seção 13.

---

## 1. Visão

O Sapientia conta a história da tecnologia humana dentro do Minecraft. O jogador começa com pedra
lascada, fogo e as primeiras sementes, descobre o cobre, funde o bronze, forja o ferro, constrói
máquinas a vapor, eletrifica a base e segue pela era do petróleo, da eletrônica, do átomo e do
espaço, até tecnologias que ainda não existem: fusão, nanotecnologia, mineração de asteroides,
computação quântica e transmutação da matéria.

São **25 eras**. O admin do servidor decide quando cada uma é liberada, e dentro de cada era o
jogador pesquisa o que quer construir.

A tecnologia se apoia em **cadeias de materiais**:

- **Agricultura e plantas**, a base do servidor: comida, fibras, óleos, açúcares, látex, resinas e
  remédios alimentam quase todas as outras cadeias.
- **Metais**, escondidos nas rochas em minerais que misturam vários elementos.
- **Madeira**, **água**, **químicos** e **petróleo**, que evoluem de era em era.

### Princípios

1. **Todo item é obtido jogando.** Nenhum item depende de `/sapientia give` para existir.
2. **A agricultura é a base.** Cada era depende de plantas para algo essencial: comida, fibra para
   tecidos e filtros, óleo para lubrificar máquinas, látex para isolar cabos, açúcar e amido para
   combustíveis e plásticos.
3. **Os recursos minerais do mundo são finitos.** Minério só sai de rocha natural, nunca de blocos
   colocados por jogadores. Minerar esgota o terreno. Plantas e florestas são o recurso renovável.
4. **Minerais são misturas.** Um minério rende vários metais; separar mais deles exige tecnologia
   de eras mais avançadas.
5. **Manual antes de automático.** Cada processo tem uma versão manual, lenta e barata, antes da
   máquina que o automatiza.
6. **Uma era de cada vez.** O servidor começa numa era básica e o admin libera as seguintes.
7. **Nada vem de graça.** Energia exige combustível ou infraestrutura; ligas exigem todos os
   ingredientes; automação de mineração consome o mundo.
8. **Liso em qualquer escala.** O servidor não pode travar com 10 milhões de blocos do Sapientia
   ativos (seção 9).
9. **Java e Bedrock jogam igual.**

---

## 2. Diagnóstico do estado atual

Problemas encontrados no código da versão 1.11.0:

| # | Problema | Efeito no jogo |
|---|----------|----------------|
| 1 | A Bancada Sapientia e o Guia não têm receita, e o plugin não registra nenhuma receita vanilla. | Um jogador sem op não consegue começar. |
| 2 | Os dez minérios brutos do Sapientia não são gerados no mundo e não caem de nada. | Toda a metalurgia só existe por comando. |
| 3 | O macerador só aceita os minérios brutos do Sapientia; os do vanilla não entram na cadeia. | O jogador não aproveita o que já minera. |
| 4 | O gerador básico produz 4 SU/t sem combustível. | Energia infinita desde o primeiro bloco. |
| 5 | O misturador faz ligas com um único pó; latão nunca é produzido. | Ligas sem sentido. |
| 6 | O lavador de minério devolve o mesmo pó que recebe. | Máquina sem função. |
| 7 | A sonda gera petróleo "virtual" abaixo da rocha-mãe e os androides produzem loot simulado. | Recursos infinitos que não respeitam o mundo. |
| 8 | Não existem cadeias de agricultura, madeira, água ou químicos; plantas não têm papel na indústria. | A progressão é só metal e energia. |
| 9 | O guia mostra tudo de uma vez e não existe noção de era. | O admin não controla o ritmo. |
| 10 | Receitas de níveis diferentes se misturam (logística MV pede circuito T3, de eletrônica HV). | A ordem de progressão não é garantida. |
| 11 | Quatro loops percorrem todos os nós de energia carregados a cada 5 a 10 ticks, na thread principal, buscando mundo e bloco de cada nó. A logística roda todo tick. Não há orçamento nem limite por chunk. | O custo cresce com o total de máquinas; dezenas de milhares já derrubam o TPS. |

O que já funciona e deve ser aproveitado: as redes de energia, itens e fluidos; o processamento das
máquinas; o guia com categorias; o desbloqueio de receitas por jogador; as overrides em YAML; o
gerador de texturas.

---

## 3. As eras

### 3.1 Linha do tempo

A ordem segue a história de forma aproximada; onde a jogabilidade pede, a ordem muda (a eletrônica
vem antes da era atômica porque o reator depende de processadores).

#### Passado

| Era | Nome | Inspiração | Destaques | Energia | Item-porta |
|-----|------|------------|-----------|---------|------------|
| 0 | Chegada | — | Guia, Bancada Sapientia | — | Bancada Sapientia |
| 1 | Idade da Pedra | Neolítico | Revolução agrícola (linho, mó manual, pão), carvoaria, argila refratária, forno de argila | fogo | Forno de argila |
| 2 | Idade do Cobre | 5000–3300 a.C. | Cobre, ouro e prata nativos; arroz; salina; tecelagem manual | carvão vegetal | Martelo de cobre |
| 3 | Idade do Bronze | 3300–1200 a.C. | Estanho e bronze; bateia; foice; queijo; tear | carvão vegetal | Lingote de bronze |
| 4 | Idade do Ferro | 1200–500 a.C. | Forno de lupa, fole, chumbo, copelação, mercúrio; cal; arado de ferro | carvão vegetal | Lupa de ferro |
| 5 | Antiguidade Clássica | 500 a.C.–500 d.C. | Roda d'água, pilão, serraria e moinho hidráulicos; aqueduto; uva, vinho e vinagre; latão; concreto romano | hidráulica (local) | Roda d'água |
| 6 | Idade Média | 500–1450 | Moinho de vento, alto-forno, aço de Damasco; alambique e ácidos; apicultura; rotação de culturas | hidráulica e eólica (local) | Alto-forno |
| 7 | Renascença e Navegações | 1450–1750 | Mecanismos de precisão, imprensa, vidro óptico; milho, algodão e seringueira; resina e breu; prospecção | hidráulica e eólica (local) | Mecanismo de precisão |
| 8 | Revolução Industrial | 1760–1860 | Vapor, coque, perfuratriz a vapor; tear mecânico, colheitadeira; ácido sulfúrico, vulcanização, papel; conservas | vapor | Caldeira |

#### Presente

| Era | Nome | Inspiração | Destaques | Energia | Item-porta |
|-----|------|------------|-----------|---------|------------|
| 9 | Eletricidade | 1870–1900 | Gerador, cabos isolados com borracha, eletrólise (alumínio, cloro-álcali), tratamento de água, refrigeração | elétrica LV | Carcaça de máquina |
| 10 | Era do Aço | 1880–1920 | Ligas de aço, forno de indução, pedreira; fertilizantes (amônia, NPK); soja; aspirina | elétrica MV | Lingote de aço inox |
| 11 | Petróleo e Química | 1900–1950 | Refinaria, plásticos, borracha sintética, nylon; etanol e biodiesel; penicilina; sonda rotativa | combustão, biogás | Plástico |
| 12 | Eletrônica | 1947–1970 | Silício, transistores, laser; água ultrapura e químicos de alta pureza; hidroponia | elétrica HV | Processador T1 |
| 13 | Era Atômica | 1942–1970 | Urânio, fissão, água pesada, radiação | fissão · EV | Barra de combustível |
| 14 | Era Espacial | 1957–1975 | Titânio, foguetes, satélites, GPS, RTG, tuneladora | RTG | Satélite |
| 15 | Era da Informação | 1970–2000 | Computadores, lógica programável, terras raras, agricultura de precisão | elétrica IV | Computador |
| 16 | Robótica | 2000–2020 | Androides, baterias de lítio, drones agrícolas | baterias | Núcleo de IA |
| 17 | Energia Renovável | 2010–2040 | Solar, eólica, hidrogênio verde, fazenda vertical, biocombustível de algas, reciclagem | solar, eólica, hidrogênio | Célula solar avançada |

#### Futuro

| Era | Nome | Inspiração | Destaques | Energia | Item-porta |
|-----|------|------------|-----------|---------|------------|
| 18 | Fusão | 2040–2080 | Reator de fusão, supercondutores, deutério e trítio | fusão · LuV | Bobina supercondutora |
| 19 | Nanotecnologia | 2080–2120 | Grafeno, nanocelulose, montador molecular, replicador, sementes editadas | fusão | Montador molecular |
| 20 | Era Orbital | 2120–2200 | Estações orbitais, mineração de asteroides, hélio-3, fazenda orbital | solar orbital · ZPM | Módulo orbital |
| 21 | Era Quântica | 2200–2300 | Computação quântica, teletransporte de itens, equipamentos quânticos | vácuo (conceitual) | Processador quântico |
| 22 | Antimatéria | 2300–2400 | Acelerador, contenção e reator de antimatéria | antimatéria · UV | Célula de antimatéria |
| 23 | Era Interestelar | 2400–2600 | Matéria exótica, neutrônio, núcleo de dobra | estelar | Núcleo de dobra |
| 24 | Singularidade | além de 2600 | Transmutação, matéria programável, síntese de alimentos, esfera de Dyson | estelar total | — |

A era 0 está sempre liberada. Por padrão, um servidor novo começa com as eras 0 e 1.

### 3.2 Energia ao longo das eras

- **Eras 1 a 4:** sem rede. Fornos queimam combustível direto; o resto é trabalho manual.
- **Eras 5 a 7 — energia mecânica local (UM):** rodas d'água e moinhos de vento movem máquinas
  encostadas neles ou ligadas por eixos de até 8 blocos. Não há rede.
- **Era 8 — vapor:** caldeiras produzem vapor, levado por canos às máquinas a vapor.
- **Era 9 em diante — eletricidade (SU):** LV (9), MV (10), HV (12), EV (13), IV (15), LuV (18),
  ZPM (20) e UV (22).

### 3.3 Liberação pelo admin

```
/sapientia era                 mostra a era atual e o que ela libera
/sapientia era set <n>         define a era máxima do servidor
/sapientia era next            libera a próxima era
```

- Permissão `sapientia.command.era` (padrão: op).
- Ao liberar uma era, o servidor anuncia no chat o nome e o que ela traz (configurável).
- A era do servidor fica salva no banco de dados.

```yaml
progression:
  starting-era: 1
  announce: true
  research: true
  give-guide-on-join: true
```

### 3.4 Pesquisa do jogador dentro da era

- Cada receita tem **pré-requisitos**: os itens que o jogador precisa ter descoberto antes. Por
  padrão, são os ingredientes do Sapientia da própria receita.
- Um item é **descoberto** quando o jogador o fabrica, o colhe, o pega do chão ou o tira de uma
  máquina.
- Com todos os pré-requisitos descobertos, a receita é desbloqueada e aparece no guia.
- **Livros técnicos** (era 7): um jogador pode imprimir o que já descobriu e entregar a outro,
  desbloqueando as mesmas receitas (dentro das eras liberadas).

Com `progression.research: false`, liberar a era desbloqueia todas as receitas dela para todos.

### 3.5 Conteúdo de uma era bloqueada

| Situação | Comportamento |
|----------|---------------|
| Guia | A era aparece como seção trancada, sem mostrar os itens. |
| Bancada e máquinas | Não produzem itens de eras bloqueadas. |
| Colocar blocos e plantar | Blocos e sementes de eras bloqueadas não podem ser colocados. |
| Minérios e plantas silvestres | Minerais e sementes de eras bloqueadas não aparecem. |
| Processamento | Metais de eras bloqueadas vão para o rejeito, reprocessável depois. |
| Itens já obtidos | Continuam no inventário, mas não podem ser colocados nem processados. |
| Admins | A permissão `sapientia.era.bypass` ignora os bloqueios. |

### 3.6 Regra de coerência

**Um item da era N só pode ser feito com ingredientes das eras 0 a N.** Um teste automatizado
verifica essa regra em todas as receitas.

---

## 4. Mineração

### 4.1 Como um minério aparece

Os metais do Sapientia não existem como blocos de minério. Eles aparecem **ao quebrar rochas
naturais**:

1. O jogador quebra um bloco hospedeiro natural (pedra, ardósia profunda, granito, cascalho, areia
   e outros).
2. Além do drop normal do bloco, há uma chance de cair um **fragmento de mineral**.
3. O mineral depende do **bloco**, da **profundidade**, do **bioma**, da **dimensão** e da **era do
   servidor**.
4. O mineral exige uma picareta mínima: pedra para os da era 2, cobre para os da era 3, bronze
   para os da era 4, ferro para os das eras 5 a 12 e diamante da era 13 em diante.
5. **Fortuna** aumenta a chance; **Toque de Seda** não dá fragmento.

### 4.2 Blocos hospedeiros

| Bloco | Chance base | Minerais típicos |
|-------|-------------|------------------|
| Pedra, andesito | 2 % | cobre nativo, malaquita, galena, hematita, magnetita, esfalerita |
| Diorito | 3 % | prata nativa, magnetita, calcopirita |
| Granito | 4 % | cassiterita, volframita, molibdenita, espodumênio, lepidolita, berilo, coltan, bismutinita, fluorita |
| Ardósia profunda | 3,5 % | pentlandita, cromita, cobaltita, uraninita, platina, arsenopirita, grafite |
| Tufo | 3 % | estibinita, calaverita, esfalerita |
| Calcita | 3 % | malaquita, galena, esfalerita, magnesita, bastnasita, fosforita |
| Cascalho | 3 % | ouro nativo, cassiterita, platina nativa, coltan (aluvião de rio) |
| Areia (praia e rio) | 1,5 % | ilmenita, rutilo, zircão, monazita, torianita |
| Areia vermelha, terracota | 2,5 % | bauxita, hematita, vanadinita, pirolusita, salitre, bórax |
| Argila | 1 % | bauxita |
| Netherrack | 1,5 % | cinábrio, estibinita, enxofre nativo |
| Basalto, pedra-negra | 2,5 % | cromita, cinábrio, enxofre nativo |
| Pedra do End | 1 % | fragmento de meteorito |

### 4.3 Terreno virgem

**Só blocos gerados pelo mundo dão minério.**

| Caso | Dá minério? |
|------|-------------|
| Bloco gerado pelo mundo e nunca mexido | Sim |
| Bloco colocado por jogador, ejetor ou enderman | Não |
| Pedra, pedregulho, basalto ou obsidiana formados por lava e água | Não |
| Bloco movido por pistão | Não, a partir do movimento |
| Areia ou cascalho natural que cai por gravidade | Sim, mantém a origem |
| Areia ou cascalho colocado que cai | Não |
| Qualquer bloco quebrado por explosão | Não |
| Bloco natural quebrado por máquina do Sapientia | Sim, pelas regras da máquina (seção 4.9) |
| Construções coladas com ferramentas de edição (por exemplo, WorldEdit) | Contam como naturais; o admin pode marcar a região com `/sapientia mining mark` |

Explosões nunca dão minério, para que TNT e creepers não virem atalho de mineração.

**Como o plugin sabe:** cada chunk guarda um mapa compacto dos blocos colocados, apenas nas seções
onde houve colocação, nos dados persistentes do próprio chunk.

**Mundos antigos:** chunks gerados antes de o plugin ser instalado são tratados como naturais por
padrão (`mining.legacy-chunks: natural`); com `placed`, só chunks novos dão minério.

### 4.4 Minerais metálicos

| Mineral | Principal | Secundários | Traços | Onde | Era |
|---------|-----------|-------------|--------|------|-----|
| Cobre nativo | Cobre | — | Prata | pedra, andesito · Y 0 a 80 | 2 |
| Malaquita | Cobre | — | Zinco | calcita, pedra · cavernas | 2 |
| Ouro nativo | Ouro | Prata | — | cascalho de rio, pedra · Y −20 a 40 | 2 |
| Prata nativa | Prata | Cobre | — | diorito · Y 0 a 40 | 2 |
| Calcopirita | Cobre | Ferro | Ouro, selênio, telúrio | diorito, pedra · Y −20 a 50 | 3 |
| Cassiterita | Estanho | — | Nióbio, tântalo | granito, cascalho · Y 0 a 60 | 3 |
| Arsenopirita | Arsênio | Ferro | Ouro | ardósia profunda, pedra · Y −30 a 30 | 3 |
| Hematita | Ferro | — | Manganês | pedra, terracota · Y −10 a 80 | 4 |
| Magnetita | Ferro | Titânio | Vanádio | pedra, diorito · Y −40 a 40 | 4 |
| Galena | Chumbo | Prata | Bismuto, antimônio | pedra, calcita · Y −20 a 40 | 4 |
| Cinábrio | Mercúrio | — | — | netherrack, basalto · Nether | 4 |
| Esfalerita | Zinco | Cádmio, ferro | Índio, germânio, gálio | pedra, calcita, tufo · Y −30 a 30 | 5 |
| Estibinita | Antimônio | — | Ouro | tufo, netherrack · Y −20 a 20 | 5 |
| Bismutinita | Bismuto | — | — | granito · Y −10 a 30 | 6 |
| Pirolusita | Manganês | Ferro | — | terracota, pedra · Y 0 a 60 | 6 |
| Cobaltita | Cobalto | Arsênio | Níquel | ardósia profunda · Y −50 a 0 | 7 |
| Platina nativa | Platina | Paládio, irídio | Ósmio, ródio, rutênio | cascalho de rio · ardósia profunda abaixo de Y −40 | 7 |
| Pentlandita | Níquel | Ferro, cobalto | Platina, paládio | ardósia profunda · Y −60 a 0 | 8 |
| Cromita | Cromo | Ferro | — | ardósia profunda, basalto · Y −64 a −20 | 8 |
| Volframita | Tungstênio | Ferro, manganês | Estanho | granito · Y −30 a 20 | 8 |
| Molibdenita | Molibdênio | — | Rênio | granito, ardósia profunda · Y −40 a 10 | 8 |
| Ilmenita | Titânio | Ferro | — | areia de praia, pedra · Y −20 a 20 | 8 |
| Bauxita | Alumínio | Ferro, titânio | Gálio | terracota, argila, areia vermelha · superfície | 9 |
| Magnesita | Magnésio | — | — | calcita, pedra · Y 0 a 60 | 9 |
| Vanadinita | Vanádio | Chumbo | — | areia vermelha, terracota · desertos | 10 |
| Berilo | Berílio | Alumínio | — | granito · Y −20 a 40 | 11 |
| Espodumênio | Lítio | Alumínio | — | granito · Y −30 a 30 | 11 |
| Uraninita | Urânio | Tório | Rádio, chumbo | ardósia profunda, granito · abaixo de Y −30 | 13 |
| Torianita | Tório | Urânio | — | areia de praia, granito | 13 |
| Zircão | Zircônio | Háfnio | Urânio | areia, granito | 13 |
| Rutilo | Titânio | — | — | areia, pedra | 14 |
| Coltan | Nióbio | Tântalo | Estanho | granito, cascalho | 14 |
| Lepidolita | Lítio | Rubídio, césio | — | granito | 15 |
| Monazita | Cério, lantânio, neodímio | Tório | Ítrio | areia de praia, cascalho | 15 |
| Bastnasita | Cério, lantânio, neodímio | — | Európio | calcita, pedra | 15 |
| Xenotima | Ítrio | Disprósio, térbio | — | granito, areia | 17 |
| Calaverita | Telúrio | Ouro | — | tufo, pedra | 17 |
| Fragmento de meteorito | Ferro, níquel | Cobalto | Irídio, platina | pedra do End | 20 |

### 4.5 Minerais não metálicos

Também caem de rocha virgem e são a base da cadeia química.

| Mineral | Produto | Onde | Era |
|---------|---------|------|-----|
| Halita (sal-gema) | Sal | pedra, calcita · Y −20 a 40 | 2 |
| Enxofre nativo | Enxofre | netherrack, basalto · Nether | 6 |
| Salitre | Nitrato de potássio | areia vermelha, terracota · desertos | 6 |
| Fosforita | Fosfato | calcita, pedra | 8 |
| Grafite | Carbono (grafite) | ardósia profunda | 8 |
| Silvita | Potássio | calcita, pedra · Y −30 a 20 | 10 |
| Fluorita | Flúor | granito | 12 |
| Bórax | Boro | areia vermelha, terracota · desertos | 15 |

O silício vem de **quartzo** (Nether) e **areia**, no forno a arco da era 12. A cal vem da
**calcita** (vanilla), queimada a partir da era 4.

### 4.6 Veios e eras bloqueadas

- **Veios:** os minerais se concentram em regiões de alguns chunks definidas pela semente do mundo.
  Instrumentos de prospecção (era 7), o prospector (era 10) e satélites (era 14) revelam veios cada
  vez mais longe.
- **Minerais de eras bloqueadas não caem.** A rocha quebrada é consumida mesmo assim: minerar cedo
  demais gasta o recurso do futuro.

### 4.7 Processamento e separação

| Era | Método | Rendimento do principal | Secundários | Traços |
|-----|--------|-------------------------|-------------|--------|
| 1–2 | Martelo + forno de argila | 1 | perdidos | perdidos |
| 3 | Bateia | 1 | 20 % de chance | perdidos |
| 4 | Copelação, forno de lupa, amálgama | 1 | 35 % | ouro: 50 % |
| 5–6 | Pilão hidráulico, alto-forno, água-forte | 1,5 | 35 % | perdidos |
| 8 | Martelo a vapor, forno de reverbero | 1,75 | 45 % | perdidos |
| 9 | Macerador, lavador, eletrólise | 2 | 50 % | 10 % |
| 10 | Flotação, centrífuga | 2,5 | 70 % | 20 % |
| 11 | Lixiviação química | 3 | 85 % | 35 % |
| 12 | Refino por zona | 3 | 90 % | 50 % |
| 15 | Extração por solvente | 3 | 95 % | 75 % |
| 19 | Separação molecular | 4 | 100 % | 100 % |

**Rejeito:** secundários e traços não separados saem como **rejeito do mineral**, que pode ser
reprocessado com um método melhor.

### 4.8 Minérios do vanilla

| Minério vanilla | Entra no Sapientia como |
|-----------------|------------------------|
| Ferro bruto | hematita |
| Cobre bruto | calcopirita |
| Ouro bruto | ouro nativo |
| Esmeralda | berilo de gema (berílio a partir da era 11) |
| Quartzo do Nether | silício a partir da era 12 |
| Carvão | combustível; coque (era 8); grafite sintético (era 12) |
| Detrito ancestral | netherite (vanilla) |

### 4.9 Automação da extração

Toda automação de mineração **consome o mundo** e aplica as regras de terreno virgem.

| Era | Método | Como funciona | Marca no mundo |
|-----|--------|---------------|----------------|
| 1–5 | Picaretas | Manual, um bloco por vez | galerias |
| 6 | Martelo de mineração | Ferramenta que quebra 3×3, lenta e com muito desgaste | galerias largas |
| 8 | Perfuratriz a vapor | Cava um túnel 2×3 à frente, a vapor, e manda os drops para um baú | túneis retos |
| 9 | Broca elétrica | Ferramenta a bateria que quebra 3×3 | galerias largas |
| 10 | Pedreira | Escava uma área camada por camada até a rocha-mãe (16×16, até 64×64 com melhorias) | cavas a céu aberto |
| 11 | Sonda rotativa | Perfura uma coluna 3×3 até a rocha-mãe | poços |
| 14 | Tuneladora | Multibloco que avança sozinho e escava um túnel 5×5 | túneis longos |
| 16 | Androides mineradores | Mineram veios e áreas definidos por programa | minas complexas |
| 17 | Mineração urbana | Reciclador desmonta itens e sucata em metais | nenhuma (renovável a partir de itens) |
| 20 | Mineração de asteroides | Missões orbitais trazem carga de minério | nenhuma (renovável, fora do mundo) |
| 24 | Transmutação | Converte energia em qualquer elemento | nenhuma (renovável, custo enorme) |

Máquinas que quebram blocos agem em nome do dono e respeitam plugins de proteção de terreno.

---

## 5. Metais e ligas

### 5.1 Metais por era de extração

| Era | Metais |
|-----|--------|
| 2 | Cobre, ouro, prata |
| 3 | Estanho, arsênio |
| 4 | Ferro, chumbo, mercúrio |
| 5 | Zinco, antimônio |
| 6 | Bismuto, manganês |
| 7 | Cobalto, platina |
| 8 | Níquel, cromo, tungstênio, molibdênio |
| 9 | Alumínio, magnésio, paládio |
| 10 | Vanádio |
| 11 | Berílio, lítio, cádmio |
| 12 | Silício, germânio, gálio, índio, selênio |
| 13 | Urânio, tório, zircônio, háfnio, rádio |
| 14 | Titânio, nióbio, tântalo, irídio, ósmio |
| 15 | Rubídio, césio, cério, lantânio, neodímio, európio, rênio, ródio, rutênio |
| 17 | Ítrio, disprósio, térbio, telúrio |
| 19 | Carbono avançado (grafeno, nanotubos) |
| 20 | Hélio-3 |
| 22 | Antimatéria |
| 23 | Matéria exótica, neutrônio |
| 24 | Matéria programável |

Nem todo metal tem as nove formas atuais (bruto, pó, lingote, bloco, placa, fio, vara, engrenagem,
parafuso): metais estruturais têm todas; metais químicos ou eletrônicos só pó e lingote.

### 5.2 Ligas

| Era | Ligas |
|-----|-------|
| 2 | Electrum |
| 3 | Bronze, bronze arsenical |
| 5 | Latão, peltre |
| 6 | Ferro fundido, aço de Damasco |
| 7 | Liga de tipos (chumbo, estanho, antimônio) |
| 8 | Aço-carbono |
| 9 | Solda, constantan |
| 10 | Aço inox, nicromo, invar, kanthal, aço rápido |
| 11 | Duralumínio, cobre-berílio |
| 13 | Zircaloy |
| 14 | Ligas de titânio, Inconel, Hastelloy |
| 15 | Ímã de neodímio |
| 16 | Cátodo de bateria (níquel, manganês, cobalto) |
| 17 | Filme fino de cádmio e telúrio |
| 18 | Nióbio-titânio, nióbio-estanho, óxido de ítrio-bário-cobre, carboneto de tungstênio |
| 19 | Metamateriais |

---

## 6. Cadeias de materiais

Cada cadeia evolui de era em era. Os itens exatos estão nos documentos de cada era.

### 6.1 Agricultura e plantas (a base do servidor)

As plantas do Sapientia crescem com o mecanismo do próprio Minecraft: o plugin não gasta nada
enquanto elas crescem e só age na colheita. No mundo, cada planta usa o visual de uma planta
vanilla parecida; a identidade fica nos itens (sementes e produtos com textura própria).

| Planta | Era | Onde se obtém | Produtos principais |
|--------|-----|---------------|---------------------|
| Trigo, cenoura, batata, beterraba, cana, abóbora, melancia (vanilla) | 1 | vanilla | farinha, açúcar, etanol, ração |
| Linho | 1 | sementes silvestres em planícies (grama cortada com faca de sílex) | fibra de linho, sementes (óleo de linhaça) |
| Erva medicinal | 1 | sementes silvestres em florestas | pomada curativa, chás |
| Arroz | 2 | sementes em pântanos e margens de rio | arroz, palha |
| Uva | 5 | videira silvestre em florestas e bosques | vinho, vinagre, passas |
| Anil | 5 | selvas e savanas | corante azul |
| Milho | 7 | selvas | espiga, amido, etanol, ração |
| Algodão | 7 | savanas e desertos | fibra de algodão, óleo de semente |
| Seringueira (árvore) | 7 | muda que cai de folhas de selva | látex, madeira |
| Soja | 10 | planícies | óleo, farelo, biodiesel |
| Algas cultivadas | 17 | kelp vanilla em biorreator | biocombustível, fertilizante |
| Sementes editadas | 19 | engenharia genética de qualquer planta | rendimento maior, resistência |

| Era | Técnica agrícola |
|-----|------------------|
| 1 | Plantio manual, sementes silvestres, mó manual (farinha), composto |
| 2 | Arado de cobre (3 blocos por vez), arrozal alagado, salina |
| 3 | Foice de bronze (colhe 3×3 e replanta) |
| 4 | Arado de ferro, adubo de cal |
| 5 | Irrigação por aqueduto, moinho hidráulico de grãos, lagar de uva |
| 6 | Rotação de culturas (bônus por alternar plantas no mesmo solo), apicultura, moinho de vento |
| 7 | Plantas do Novo Mundo, torneira de resina e látex |
| 8 | Semeadora e colheitadeira a vapor, descaroçador de algodão, superfosfato |
| 9 | Bomba de irrigação elétrica, câmara frigorífica |
| 10 | Fertilizante NPK (amônia, fosfato, potássio), trator elétrico de área |
| 11 | Biocombustíveis, defensivos biológicos, câmara de fungos (penicilina) |
| 12 | Hidroponia com luz artificial |
| 15 | Sensores de solo e irrigação controlada por programas de lógica |
| 16 | Androides agricultores e drones de polinização |
| 17 | Fazenda vertical, algas |
| 19 | Edição genética de sementes |
| 20 | Fazenda orbital |
| 24 | Síntese de alimentos |

### 6.2 Alimentos

Comidas do Sapientia saciam mais que as do vanilla e algumas dão efeitos curtos, o que dá valor à
agricultura desde o início.

| Era | Alimentos |
|-----|-----------|
| 1 | Pão de farinha, mingau de ervas, chá medicinal |
| 2 | Arroz cozido, carne salgada |
| 3 | Queijo |
| 5 | Vinho (resistência curta), passas |
| 6 | Hidromel, pão de mel |
| 7 | Chocolate, pipoca, tortilha |
| 8 | Conservas em lata de estanho |
| 9 | Comida congelada (dura mais em baús frigoríficos) |
| 10 | Ração animal (acelera reprodução e crescimento de animais) |
| 11 | Refeição pronta |
| 15 | Suplemento nutricional |
| 24 | Alimento sintetizado |

### 6.3 Madeira

| Era | Técnica | Produtos |
|-----|---------|----------|
| 1 | Carvoaria (queima lenta de toras) | carvão vegetal ×1,5, cinza de madeira |
| 2 | Serra manual | 6 tábuas por tora |
| 5 | Serraria hidráulica | tábuas ×1,5, serragem |
| 7 | Torneira de resina (pinheiros) | resina, breu, barril alcatroado |
| 8 | Retorta de destilação seca, fábrica de papel | carvão, metanol, ácido acético, creosoto, madeira tratada, papel, papelão |
| 11 | Celulose | celuloide, raiom |
| 17 | Pellets de biomassa | combustível renovável |
| 19 | Nanocelulose | material estrutural leve |

Seringueira e pinheiros só dão látex e resina em árvores de verdade (troncos colocados por
jogadores não produzem).

### 6.4 Fibras e têxteis

| Era | Técnica | Usos |
|-----|---------|------|
| 1 | Fibra vegetal torcida | barbante, cesto |
| 2 | Fuso e tear manual (linho) | tecido de linho, sacos |
| 3 | Tear de pedal | tecido em volume, velas de barco |
| 6 | Velas de moinho, lã tratada | moinho de vento |
| 8 | Descaroçador e tear mecânico (algodão) | tecido de algodão, filtros, correias |
| 9 | Algodão e borracha | isolamento de cabos |
| 11 | Nylon, raiom | tecidos técnicos, cordas |
| 13 | Tecido revestido de chumbo | traje antirradiação |
| 19 | Fibra de nanotubos | cabos ultrarresistentes |

### 6.5 Água

A água passa por níveis de pureza, e cada era exige um nível maior.

| Era | Técnica | Produto |
|-----|---------|---------|
| 1 | Água vanilla | água |
| 2 | Salina (evaporação de água do mar) | sal marinho, salmoura |
| 5 | Aqueduto, cisterna | água transportada e armazenada |
| 8 | Bomba a vapor, condensador | água para caldeiras; reaproveitamento do vapor |
| 9 | Filtro de areia e carvão | água tratada |
| 10 | Destilador | água destilada |
| 12 | Deionizador | água ultrapura (fabricação de chips) |
| 13 | Destilação fracionada | água pesada |
| 17 | Dessalinizador | água doce a partir do mar |
| 18 | Separação isotópica | deutério |

### 6.6 Químicos

| Era | Processo | Produtos |
|-----|----------|----------|
| 1 | Lixívia de cinzas | potassa |
| 2 | Salina | sal |
| 4 | Forno de cal | cal virgem, argamassa |
| 5 | Fermentação, pozolana | vinagre, concreto romano, corantes |
| 6 | Alambique | álcool, ácido sulfúrico diluído, ácido nítrico, água-forte (separa ouro e prata) |
| 7 | Saboaria, vidraria | sabão, vidro óptico, tintas |
| 8 | Câmaras de chumbo, processo Leblanc, vulcanização, cimento | ácido sulfúrico, soda, borracha, cimento Portland |
| 9 | Cloro-álcali (eletrólise de salmoura), processo Bayer | cloro, soda cáustica, hidrogênio, alumina |
| 10 | Haber-Bosch, Ostwald | amônia, ácido nítrico, fertilizantes |
| 11 | Petroquímica, farmacêutica | plásticos, solventes, detergentes, aspirina, penicilina |
| 12 | Química de alta pureza | ácido fluorídrico, gases dopantes, fotorresiste |
| 13 | Fluoretação | hexafluoreto de urânio |
| 15 | Extração por solvente | separação de terras raras |
| 17 | Química verde | eletrólitos de bateria, amônia verde |
| 19 | Precursores de deposição | grafeno, nanotubos |

### 6.7 Petróleo

| Era | Técnica | Produtos |
|-----|---------|----------|
| 7 | Betume natural (afloramentos em desertos) | breu, impermeabilizante |
| 8 | Alcatrão do coque | creosoto, óleos leves |
| 11 | Pumpjack, refinaria, craqueamento | diesel, gasolina, lubrificante, etileno, plásticos, asfalto |
| 12 | Petroquímica fina | solventes, fotorresiste |
| 17 | Biocombustíveis substituem o petróleo | biodiesel, etanol, algas |

O petróleo fica em reservatórios finitos por chunk (sistema atual), consumidos pelos pumpjacks.

---

## 7. Eras detalhadas

Cada era tem um documento com todos os itens, receitas, máquinas e tarefas:
[`eras/README.md`](eras/README.md).

---

## 8. Conteúdo atual por era

| Era | Conteúdo que o plugin já tem |
|-----|------------------------------|
| 0 | Guia, Bancada Sapientia |
| 2–6 | Cobre, prata, estanho, chumbo, zinco (formas atuais); electrum, bronze, latão, aço de Damasco; pedestal |
| 8 | Caldeira, condensador; cano, bomba, tanque e dreno de fluidos; níquel |
| 9 | Gerador, cabo, capacitor, consumidor, console, chave inglesa, carcaça de máquina; macerador, lavador, forno elétrico, serra de bancada; cabo de itens, produtor, consumidor e filtro; alumínio; motor e bobina T1 |
| 10 | Cabo e capacitor MV, transformador LV→MV, carcaça MV; misturador, compressor, prensa, extrator, forno de indução; aço inox, nicromo, carcaça de aço inox; pedreira; prospector; buffer, divisor, câmara de filtros, transbordo, sensor comparador, esteira, válvula e sensor de nível |
| 11 | Pumpjack, refinaria, geradores a combustão e biogás, turbina a gás; craqueador, fermentador, destilador, biorreator; cano pressurizado, compressor de gás; sonda, extrator de gás; embalador e desembalador; lítio; circuito T1 |
| 12 | Silício, wafer; processador T1, componentes T2; cabo e capacitor HV, transformador MV→HV; eletrolisador, laminador, cortador a laser, reator químico; liquefator, separador de fases, coletor atmosférico |
| 14 | Titânio; RTG; transmissor e marcador GPS, mapa GPS |
| 15 | Componentes T3, RAM, HDD; programas de lógica |
| 16 | Os oito androides e os dezesseis upgrades; SSD |
| 17 | Geradora geotérmica, dessalinizador |

Ajustes necessários: os minérios brutos atuais são substituídos pelos fragmentos de mineral; a
pedreira, a sonda e os androides passam a agir no mundo real; a logística da era 10 deixa de pedir
circuito T3; caldeira e condensador viram a base da era 8; a chave inglesa muda de receita.

---

## 9. Desempenho e escala

### 9.1 Meta

O Sapientia deve continuar liso com **10 milhões de blocos ativos** no servidor: 100 mil por
jogador, com 100 jogadores online. **Bloco ativo** é qualquer bloco do Sapientia em funcionamento:
máquinas, cabos, canos, esteiras, tanques, geradores.

A infraestrutura precisa ser prudente sem limitar a jogabilidade: o jogador constrói bases grandes,
e o servidor aguenta porque cada bloco custa o mínimo possível e porque boas práticas são
aplicadas (limites por chunk e raio de atividade).

### 9.2 Onde o custo pode estar

| Tipo de bloco | Exemplos | Custo por tick |
|---------------|----------|----------------|
| Passivo | cabos, canos, tanques, carcaças | **zero**: só existem na topologia das redes, que muda apenas quando um bloco é colocado ou quebrado |
| De rede | geradores, capacitores, consumidores simples | somado por rede: o custo é por **rede**, não por bloco |
| Processador | máquinas com receita | **um evento por ciclo de receita** (a cada 5 a 20 s), agendado; nenhum custo entre eventos |
| Ocioso | máquina sem insumo, sem energia ou com saída cheia | **zero** até um evento acordá-la |
| Fora do raio de atividade | qualquer bloco longe dos jogadores | **zero** |

**Estimativa com 10 milhões de blocos ativos:** supondo 70 % passivos e 30 % processadores (3
milhões), com ciclos médios de 10 s (200 ticks), são cerca de 15 mil eventos de máquina por tick.
A 200 ns por evento, isso cabe em 3 ms. As redes (dezenas de milhares) são resolvidas em outras
threads a cada 10 ticks. A memória fica em torno de 4 bytes por bloco passivo e 48 bytes por
processador, cerca de 200 MB no total.

### 9.3 Metas

| Métrica | Meta |
|---------|------|
| Blocos ativos no servidor | 10 milhões (100 mil por jogador × 100 jogadores) |
| Orçamento do Sapientia na thread principal | configurável; padrão de 5 ms por tick (de 50 ms) |
| Pior tick do Sapientia | nunca acima de 10 ms |
| Custo de um bloco passivo, ocioso ou fora do raio | zero por tick |
| Carregar um chunk com 1.000 blocos do Sapientia | até 1 ms |
| Memória | até 4 bytes por bloco passivo e 48 bytes por processador |

Essas metas viram benchmarks automatizados, e uma mudança que as piore não entra.

### 9.4 Limites por chunk

Limites evitam concentrações extremas num só chunk, que são a principal causa de lag em plugins de
tecnologia. Os valores padrão são configuráveis e pensados para não atrapalhar bases normais:

| Limite | Padrão |
|--------|--------|
| Blocos do Sapientia por chunk (todos os tipos, inclusive cabos e canos) | 2.048 |
| Máquinas processadoras por chunk | 256 |
| Máquinas do mesmo tipo por chunk | 64 (cada era define limites menores para máquinas pesadas) |
| Multiblocos grandes por chunk | 1 a 4, conforme a máquina |
| Androides por chunk | 4 (já existe) |

Ao atingir um limite, o bloco não é colocado e o jogador recebe uma mensagem explicando qual limite
foi atingido. Um limite opcional por jogador existe, desligado por padrão.

### 9.5 Raio de atividade

- As máquinas só funcionam em chunks a até **4 chunks** de algum jogador (uma área de 9×9 chunks em
  volta de cada um). Assim, a base continua funcionando enquanto o jogador anda por ela ou fica por
  perto, mas não quando ele vai para longe.
- Fora do raio, os blocos **pausam** (custo zero), mesmo que o chunk continue carregado. Quando um
  jogador volta, retomam de onde pararam, sem recuperar o tempo perdido.
- Para evitar liga-desliga na borda, um chunk só pausa **30 segundos** depois de ficar sem
  jogadores por perto.
- Numa rede que atravessa a borda, só a parte dentro do raio funciona.
- Configuração: `performance.activity-radius: 4` e `performance.deactivate-delay-seconds: 30`.
- O Sapientia não tem carregadores de chunk.

### 9.6 Regras de arquitetura

1. **Agenda, não varredura.** Nada percorre todas as máquinas a cada tick; cada máquina ativa tem o
   próximo evento marcado numa fila por tempo.
2. **Máquina ociosa dorme** até um evento acordá-la (chega item, volta energia, saída esvazia).
3. **Orçamento com fatiamento.** Se o tick passar do orçamento, o restante fica para o seguinte; sob
   carga extrema as máquinas desaceleram, mas o servidor não trava. Redes perto de jogadores têm
   prioridade.
4. **Redes como entidades**, com totais agregados, recalculadas só quando mudam.
5. **Dados compactos:** arrays primitivos por chunk, ids numéricos, nada de objetos por bloco,
   `ItemStack` ou busca de mundo no caminho quente.
6. **Fora da thread principal quando for seguro:** redes, rotas, persistência e tabelas. Arquitetura
   compatível com a divisão por regiões do Folia no futuro.
7. **Persistência em lote**, em segundo plano, sem acesso a banco de dados no tick.
8. **Plantas usam o crescimento vanilla**; colheitadeiras reagem a eventos de crescimento em vez de
   varrer plantações; torneiras de resina e látex produzem por evento agendado.
9. **Mineração e eras em tempo constante:** mapa de bits para terreno virgem, tabelas de drop
   pré-compiladas, era do servidor e de cada item em memória.
10. **Nenhuma entidade por máquina;** visuais com entidades só perto de jogadores e com limite.
11. **Medir sempre:** benchmark de 10 milhões de blocos no CI e o comando `/sapientia perf` com o
    custo de cada subsistema.

### 9.7 O que muda no código atual

- Os quatro loops que percorrem todos os nós viram uma agenda única com orçamento.
- A logística passa a rodar por rede, só quando há trabalho.
- As máquinas ganham os estados ocupada, ociosa e dormindo, e passam a respeitar o raio de
  atividade.
- O estado das máquinas sai de mapas com chaves de objeto para arrays compactos por chunk.
- Limites por chunk em todos os blocos (hoje só os androides têm).

Essa fundação vem **antes** do conteúdo novo.

---

## 10. Balanceamento inicial

| Tema | Proposta |
|------|----------|
| Duração-alvo | Eras 1–4: cerca de 1 h cada · 5–8: 2 a 4 h · 9–12: 5 a 10 h · 13 em diante: 10 h ou mais |
| Chance de fragmento | Tabela da seção 4.2; ×3 dentro de um veio |
| Fortuna | +30 %, +60 %, +100 % de chance |
| Gerador LV | 4 SU/t; 1 carvão dura 80 s |
| Comidas do Sapientia | 1,5× a saciedade do equivalente vanilla |

Os valores de cada máquina estão no documento da era.

---

## 11. Proposta técnica (resumo)

- **API** (aditiva): `Era` (0 a 24); métodos padrão `SapientiaItem#era()` e `SapientiaBlock#era()`.
- **Progressão:** `ProgressionService` com a era do servidor (migração V010) e as descobertas por
  jogador.
- **Terreno virgem:** mapa de blocos colocados por seção de chunk, nos dados persistentes do chunk.
- **Mineração:** tabelas de drop configuráveis por bloco, profundidade, bioma e dimensão; veios pela
  semente do mundo.
- **Minerais, rejeitos e separação:** composição declarada por mineral; frações separáveis
  declaradas por método.
- **Plantas:** sementes e produtos do Sapientia sobre blocos de planta vanilla, identificados pelo
  mapa por chunk; drop de sementes silvestres por bioma.
- **Energia mecânica local** (eras 5–7) e **vapor** (era 8), antes da rede elétrica.
- **Fluidos:** água em níveis de pureza, vapor, salmoura, químicos e combustíveis sobre o sistema de
  fluidos atual.
- **Desempenho:** agenda única com orçamento, estados de máquina, raio de atividade, limites por
  chunk (seção 9).
- **Testes:** coerência de eras; nenhum item sem origem; blocos colocados nunca dão minério;
  benchmarks de escala.

### Ordem de entrega e versões

1. **Fundação de desempenho** (seção 9): versão **2.0.0**. **Fundação de progressão** (sistema de
   eras, comandos, bloqueios, guia, terreno virgem, fragmentos e plantas): versão **2.0.1**.
   **Era 0**: versão **2.0.2**.
2. **Eras em ordem**, uma por vez: a era N sai na versão **2.N.0**, e as correções dela seguem como
   2.N.1, 2.N.2 e assim por diante. Detalhes no `ROADMAP.md`.

---

## 12. Glossário

- **Bloco ativo:** qualquer bloco do Sapientia em funcionamento (máquina, cabo, cano, tanque).
- **Fragmento de mineral:** item que cai ao quebrar rocha virgem.
- **Rejeito:** o que sobra de um mineral depois do processamento; pode ser reprocessado.
- **Terreno virgem:** blocos gerados pelo mundo que nunca foram colocados, movidos ou formados.
- **Veio:** região onde um mineral é mais comum.
- **Raio de atividade:** distância, em chunks, dentro da qual as máquinas funcionam perto de um
  jogador.
- **Item-porta:** item que marca a conclusão de uma era.

---

## 13. Decisões em aberto

A versão 2.0.1 implementou as opções recomendadas das decisões 1 a 6 e 11, todas ajustáveis:
chunks antigos contam como naturais (`mining.legacy-chunks`), os minérios brutos viraram fragmentos
automaticamente, a era inicial é 1 (`progression.starting-era`), o admin pode baixar a era, itens de
eras bloqueadas ficam inertes, os veios já existem e as plantas usam o visual das plantas vanilla.

1. **Mundos antigos:** chunks gerados antes do plugin contam como naturais (recomendado)?
2. **Minérios brutos atuais e metais duplicados:** remover e converter automaticamente
   (recomendado) ou manter por compatibilidade?
3. **Era inicial padrão:** 1 (recomendado) ou 0.
4. **Rebaixar a era:** o admin pode voltar a uma era anterior?
5. **Itens de eras bloqueadas em posse dos jogadores:** ficam inertes (proposto) ou são removidos?
6. **Veios:** entram na primeira entrega da mineração ou depois?
7. **Energia mecânica e vapor:** sistemas novos (proposto) ou só máquinas a combustível nas eras
   5 a 8?
8. **Eras 19 a 24:** manter todas no plano ou reduzir?
9. **Limites e raio padrão:** 2.048 blocos, 256 processadores e 64 do mesmo tipo por chunk, raio
   de 4 chunks. Ajustar?
10. **Orçamento padrão por tick:** 5 ms (proposto) ou outro valor?
11. **Visual das plantas no mundo:** usar plantas vanilla parecidas (proposto, sem custo) ou
    investir em visuais próprios?
