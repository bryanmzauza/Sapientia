# Era 0 — Chegada

**Versão:** 2.0.2 · **Status:** planejada · **Sempre liberada**

## Resumo

A porta de entrada. O jogador recebe o guia, entende como o Sapientia funciona e constrói a Bancada
Sapientia, onde todas as receitas do plugin são feitas. Não há minerais nem máquinas nesta era.

## Desbloqueio

- Liberada desde o início, em qualquer servidor.
- **Item-porta:** Bancada Sapientia.

## Itens e blocos

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Guia Sapientia | `guide` | item | Entregue no primeiro login (`give-guide-on-join`). Bancada vanilla, sem forma: 1 livro + 1 sílex. | Abre o guia: era do servidor, próximo objetivo, categorias e receitas desbloqueadas. |
| Bancada Sapientia | `workbench` | bloco | Bancada vanilla: 1 mesa de trabalho no centro, 4 pedregulhos nos cantos, 2 tábuas (em cima e embaixo) e 2 sílex (nas laterais). | Grade 3×3 com as receitas do Sapientia desbloqueadas pelo jogador nas eras liberadas. Aceita ferramentas de bancada. |

## Primeira experiência

1. No primeiro login, o jogador recebe o guia e uma mensagem curta explicando o que ele é.
2. A primeira página do guia mostra: a era atual do servidor, o objetivo seguinte ("Construa a
   Bancada Sapientia") e as categorias.
3. Ao colocar a Bancada pela primeira vez, o guia avança o objetivo para a era 1 (ou para a era mais
   alta liberada).

## Tarefas

- [ ] E0.1 Registrar as receitas vanilla do guia e da Bancada.
- [ ] E0.2 Entregar o guia no primeiro login (configurável).
- [ ] E0.3 Primeira página do guia com era do servidor e próximo objetivo.
- [ ] E0.4 A Bancada mostra só receitas desbloqueadas e aceita ferramentas de bancada.
