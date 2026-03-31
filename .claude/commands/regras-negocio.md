# Regras de Negócio — poc-llm-ufc-simples

- **RN01** – Professor único no sistema, seedado via Flyway; sem cadastro ou autenticação
- **RN02** – Pergunta deve ter mínimo de 2 alternativas e exatamente 1 marcada como correta
- **RN03** – Arquivos aceitos nas aulas: PDF (`application/pdf`) e vídeo (`video/*`)
- **RN04** – Conteúdo gerado pela IA não é persistido automaticamente; requer confirmação explícita
- **RN05** – Quiz gerado pela IA não é salvo automaticamente; requer confirmação explícita
- **RN06** – Ao regerar conteúdo ou quiz, o resultado anterior é sobrescrito em memória (não no banco)
- **RN07** – Professor pode criar quiz manualmente ou via IA — os dois caminhos são válidos
- **RN08** – Uma aula pode ter conteúdo manual (`content_editor`) e arquivo coexistindo
- **RN09** – Um módulo pode ter no máximo uma prova (1:1)
- **RN10** – Se não houver conteúdo legível nas aulas do módulo, a geração de quiz via IA retorna 422
