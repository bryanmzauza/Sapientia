# Era 18 — Fusão

**Inspiração:** futuro próximo (2040–2080) · **Versão:** 2.18.x · **Status:** planejada

## Resumo

O plasma confinado por bobinas supercondutoras funde deutério e trítio. Os supercondutores exigem
nióbio, estanho, titânio e terras raras, resfriados com hélio líquido. É a primeira fonte de energia
do futuro, e abre a rede LuV.

## Desbloqueio

- Requer a era 17 e a Célula solar avançada.
- **Item-porta:** Bobina supercondutora.

## Energia: LuV

Transformador IV→LuV, cabos supercondutores e capacitores LuV.

## Materiais

| Item | Id | Tipo | Como é produzido | O que faz |
|------|----|------|------------------|-----------|
| Hélio | `helium` | gás | Extrator de gás em bolsões encontrados pela sonda (a partir desta era). | Liquefeito → hélio líquido. |
| Hélio líquido | `liquid_helium` | fluido | Liquefator criogênico. | Resfria supercondutores. |
| Liquefator criogênico | `cryo_liquefier` | máquina LuV | Bancada: 1 carcaça IV, 2 liquefatores, 1 processador T3. | Hélio → hélio líquido. |
| Fio de nióbio-titânio | `nbti_wire` | material | Forno de indução: nióbio + titânio; extrator. | Bobinas supercondutoras. |
| Fio de nióbio-estanho | `nb3sn_wire` | material | Forno de indução: 3 nióbio + 1 estanho; extrator. | Bobinas de campo alto. |
| Fita supercondutora | `hts_tape` | material | Reator químico: ítrio + terras raras + cobre (óxido) sobre fita de hastelloy. | Cabos supercondutores. |
| Bobina supercondutora | `superconducting_coil` | componente | Bancada: 8 fios de nióbio-titânio ou nióbio-estanho, 1 criostato. | Reator de fusão, aceleradores. Item-porta. |
| Criostato | `cryostat` | componente | Bancada: 4 placas de aço inox, 1 tanque de hélio líquido, 1 circuito T3. | Mantém componentes frios. |
| Carboneto de tungstênio | `tungsten_carbide_ingot` | material | Forno a arco: tungstênio + grafite. | Divertor do reator; ferramentas extremas. |
| Deutério | `deuterium` | gás | Eletrolisador com água pesada. | Combustível de fusão. |
| Trítio | `tritium` | gás | Manto reprodutor: lítio irradiado no reator de fusão. | Combustível de fusão. |

## Reator

| Item | Id | Como é produzido | O que faz |
|------|----|------------------|-----------|
| Reator de fusão | `fusion_reactor_controller` | Multibloco toroidal 7×7×7: bobinas supercondutoras, primeira parede de berílio, divertor de carboneto de tungstênio, manto de lítio + controlador (2 processadores T3, 1 computador). | Deutério + trítio → SU (LuV) e nêutrons que produzem trítio no manto. Precisa de ignição (RTG ou banco de baterias carregado). |
| Cabo supercondutor | `superconducting_cable` | Bancada: 3 fitas supercondutoras + 1 criostato → 8. | Rede LuV. |
| Transformador IV→LuV, capacitor LuV | `transformer_iv_luv`, `capacitor_luv` | Bancada: bobinas supercondutoras e capacitores IV. | Rede LuV. |

## Máquinas

| Máquina | Consumo | Limite por chunk |
|---------|---------|------------------|
| Reator de fusão | produz até 16.384 SU/t | 1 |
| Liquefator criogênico | 1.024 SU/t | 2 |

## Tarefas

- [ ] E18.1 Hélio e hélio líquido; liquefator criogênico; criostato.
- [ ] E18.2 Fios e fitas supercondutores; bobina supercondutora.
- [ ] E18.3 Carboneto de tungstênio; deutério; trítio.
- [ ] E18.4 Reator de fusão (multibloco, ignição, manto reprodutor).
- [ ] E18.5 Rede LuV.
- [ ] E18.6 Texturas.
