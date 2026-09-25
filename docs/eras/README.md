# Eras do Sapientia

O desenvolvimento do Sapientia é feito **por eras**, e as versões seguem as eras: a 2.0.0 traz as
fundações e a era 0, e a era N sai na linha 2.N.x (2.N.0 lança a era; 2.N.1 em diante são
correções). Cada arquivo desta pasta descreve uma era por
completo: o que ela libera, cada item e bloco, como é produzido, o que faz, as máquinas com seus
números e a lista de tarefas de implementação.

As regras gerais (sistema de eras, mineração, minerais, metais, desempenho) estão em
[`../jogabilidade.md`](../jogabilidade.md). Em caso de conflito, este é o documento de cada era que
vale para os detalhes da era, e o `jogabilidade.md` vale para as regras gerais.

## Convenções

- **Bancada** é a Bancada Sapientia. A bancada do Minecraft é chamada de **bancada vanilla**.
- **Ferramenta de bancada:** ferramentas como o martelo podem ser usadas como ingrediente numa
  receita da Bancada; elas voltam para o inventário com desgaste, em vez de serem consumidas.
- **Ids** estão em inglês (`clay_furnace`), no formato usado pelo código (`sapientia:<id>`).
- **Limite por chunk** é o padrão configurável de quantos blocos daquele tipo cabem num chunk
  (seção 9 do `jogabilidade.md`).
- **Energia:** UM = unidade mecânica (eras 5 a 8); SU = unidade elétrica do Sapientia (era 9 em
  diante); mB = milibaldes (fluidos e vapor).
- **Fluidos como itens:** baldes e garrafas de um fluido podem ser usados como ingrediente em
  receitas de bancada e fornos; máquinas com tanque recebem o fluido por cano.
- **Tarefas** de cada era usam o código `E<era>.<número>` (por exemplo, `E3.2`), também usado em
  commits (`Refs: E3.2`).

## Regras gerais de receita

Valem para todos os metais, a partir da era em que o metal é extraído, e não são repetidas nos
documentos das eras:

| Produto | Receita | A partir de |
|---------|---------|-------------|
| Minério triturado | 1 fragmento + martelo (Bancada), ou pilão, martelo a vapor, macerador | era 2 |
| Placa | 2 lingotes + martelo (Bancada) → 1; prensa de placas (era 10) → 1 por lingote | era 2 |
| Vara | 1 lingote + martelo (Bancada) → 2; torno (era 7), serra de bancada (era 9) | era 2 |
| Engrenagem | 4 placas em volta de 1 vara (Bancada) | era 3 |
| Parafuso | 1 vara no torno → 4 | era 7 |
| Fio | 1 lingote na fieira (era 9) → 2; extrator (era 10) → 4 | era 9 |
| Bloco | 9 lingotes (Bancada) e o inverso | era 2 |
| Mistura de liga | pós ou triturados na proporção da liga (Bancada) | era 2 |


## Índice

| Era | Versão | Nome | Documento |
|-----|--------|------|-----------|
| 0 | 2.0.x | Chegada | [era-00-chegada.md](era-00-chegada.md) |
| 1 | 2.1.x | Idade da Pedra | [era-01-pedra.md](era-01-pedra.md) |
| 2 | 2.2.x | Idade do Cobre | [era-02-cobre.md](era-02-cobre.md) |
| 3 | 2.3.x | Idade do Bronze | [era-03-bronze.md](era-03-bronze.md) |
| 4 | 2.4.x | Idade do Ferro | [era-04-ferro.md](era-04-ferro.md) |
| 5 | 2.5.x | Antiguidade Clássica | [era-05-antiguidade.md](era-05-antiguidade.md) |
| 6 | 2.6.x | Idade Média | [era-06-idade-media.md](era-06-idade-media.md) |
| 7 | 2.7.x | Renascença e Navegações | [era-07-renascenca.md](era-07-renascenca.md) |
| 8 | 2.8.x | Revolução Industrial | [era-08-revolucao-industrial.md](era-08-revolucao-industrial.md) |
| 9 | 2.9.x | Eletricidade | [era-09-eletricidade.md](era-09-eletricidade.md) |
| 10 | 2.10.x | Era do Aço | [era-10-aco.md](era-10-aco.md) |
| 11 | 2.11.x | Petróleo e Química | [era-11-petroleo-quimica.md](era-11-petroleo-quimica.md) |
| 12 | 2.12.x | Eletrônica | [era-12-eletronica.md](era-12-eletronica.md) |
| 13 | 2.13.x | Era Atômica | [era-13-atomica.md](era-13-atomica.md) |
| 14 | 2.14.x | Era Espacial | [era-14-espacial.md](era-14-espacial.md) |
| 15 | 2.15.x | Era da Informação | [era-15-informacao.md](era-15-informacao.md) |
| 16 | 2.16.x | Robótica | [era-16-robotica.md](era-16-robotica.md) |
| 17 | 2.17.x | Energia Renovável | [era-17-renovavel.md](era-17-renovavel.md) |
| 18 | 2.18.x | Fusão | [era-18-fusao.md](era-18-fusao.md) |
| 19 | 2.19.x | Nanotecnologia | [era-19-nanotecnologia.md](era-19-nanotecnologia.md) |
| 20 | 2.20.x | Era Orbital | [era-20-orbital.md](era-20-orbital.md) |
| 21 | 2.21.x | Era Quântica | [era-21-quantica.md](era-21-quantica.md) |
| 22 | 2.22.x | Antimatéria | [era-22-antimateria.md](era-22-antimateria.md) |
| 23 | 2.23.x | Era Interestelar | [era-23-interestelar.md](era-23-interestelar.md) |
| 24 | 2.24.x | Singularidade | [era-24-singularidade.md](era-24-singularidade.md) |
