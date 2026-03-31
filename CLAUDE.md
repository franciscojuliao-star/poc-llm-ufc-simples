# poc-llm-ufc-simples — Briefing do Projeto

## O que é
PoC simplificada de uma plataforma LMS com IA embarcada.
Sem segurança, sem autenticação — foco total no fluxo de criação de conteúdo com IA.
Professor único seedado via Flyway. Sem login, sem token.

## Stack
Java 21 · Spring Boot 3.4 · PostgreSQL 16 · Flyway
Spring AI · Groq (llama-3.3-70b-versatile) · Apache PDFBox · Apache Tika
MapStruct · Lombok · SpringDoc OpenAPI · Maven

## Modelo de Dados
```
Course    (1) ──> (N) Module
Module    (1) ──> (N) Lesson
Module    (1) ──> (0..1) Quiz
Quiz      (1) ──> (N) Question
Question  (1) ──> (N) Alternative
```

## Estrutura de Pacotes
```
br.ufc.llm
  course    → controller · service · repository · domain · dto · exception
  module    → controller · service · repository · domain · dto · exception
  lesson    → controller · service · repository · domain · dto · exception
  quiz      → controller · service · repository · domain · dto · exception
  shared    → config · dto · exception
```

## Convenções de Código
- Idioma: inglês para entidades, campos, métodos e variáveis
- Pacote raiz: `br.ufc.llm`
- DTOs separados: `XxxRequest` (entrada) e `XxxResponse` (saída)
- Todas as entidades possuem `createdAt` e `updatedAt`
- Resposta padrão da API:
  ```json
  { "sucesso": true, "mensagem": "", "dados": {}, "timestamp": "" }
  ```
- Commits em português no imperativo: `"Adiciona endpoint de criação de curso"`

## Método de Trabalho

### TDD — Test Driven Development
- Testes escritos ANTES da implementação
- Ordem: escreve o teste → roda (falha) → implementa → roda (passa)
- Quando a IA errar: descreva o erro, não corrija manualmente

### Método Akita
- Nunca pule etapas — implemente na ordem definida
- Uma responsabilidade por classe
- Não antecipe funcionalidades futuras
- Código simples e direto; complexidade só quando necessária
- Ao encontrar um problema: pare, entenda a causa raiz, resolva na origem

## Comandos Disponíveis
```
/arquitetura            → entidades, tabelas SQL, endpoints REST
/regras-negocio         → regras de negócio do sistema
/requisitos-funcionais  → requisitos funcionais da PoC
```
