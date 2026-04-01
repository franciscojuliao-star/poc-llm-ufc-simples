# PoC LLM UFC Simples

Prova de conceito de uma plataforma LMS (Learning Management System) com IA embarcada.
O foco é o fluxo de criação de conteúdo assistida por inteligência artificial — sem autenticação, sem segurança, professor único seedado via Flyway.

Desenvolvido por **Aglayrton Julião** com **Desenvolvimento Assistido por IA**.

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.4 |
| Banco de Dados | PostgreSQL 16 |
| Migrations | Flyway |
| IA | Spring AI + Groq (llama-3.3-70b-versatile) |
| Leitura de PDF | Apache PDFBox 3.0 |
| Detecção de tipo | Apache Tika 2.9 |
| Mapeamento | MapStruct |
| Boilerplate | Lombok |
| Documentação | SpringDoc OpenAPI (Swagger) |
| Build | Maven |

---

## Modelo de Dados

```
Course    (1) ──> (N) Module
Module    (1) ──> (N) Lesson
Module    (1) ──> (0..1) Quiz
Quiz      (1) ──> (N) Question
Question  (1) ──> (N) Alternative
```

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- PostgreSQL 16
- Conta no [Groq](https://console.groq.com) com uma API Key (`gsk_...`)

---

## Como Rodar

### 1. Criar o banco de dados e o usuário

Conecte-se ao PostgreSQL com um usuário que tenha permissão de criação:

```sql
CREATE USER poc_user WITH PASSWORD 'poc123';
CREATE DATABASE poc_llm_simples OWNER poc_user;
```

### 2. Configurar variáveis de ambiente

O projeto lê as configurações via variáveis de ambiente. Os valores padrão estão em `src/main/resources/application.yml`.

Crie um arquivo `.env` na raiz (não versionado) ou exporte as variáveis no terminal:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/poc_llm_simples
export DB_USERNAME=poc_user
export DB_PASSWORD=poc123
export GROQ_API_KEY=gsk_sua_chave_aqui
export UPLOAD_DIR=uploads
```

### 3. Rodar a aplicação

```bash
GROQ_API_KEY=gsk_sua_chave_aqui mvn spring-boot:run
```

O Flyway executará as migrations automaticamente ao iniciar.
A aplicação sobe em: `http://localhost:8080`

### 4. Swagger UI

Documentação interativa disponível em:

```
http://localhost:8080/swagger-ui.html
```

---

## Testando com Postman

O arquivo `poc-llm-ufc-simples.postman_collection.json` na raiz do projeto contém todos os endpoints organizados por domínio.

**Como importar:**
1. Abra o Postman
2. Clique em **Import**
3. Selecione o arquivo `poc-llm-ufc-simples.postman_collection.json`

**Pastas disponíveis na collection:**

| Pasta | Descrição |
|---|---|
| Cursos | CRUD de cursos, com suporte a imagem de capa |
| Módulos | CRUD de módulos vinculados a um curso |
| Aulas | CRUD de aulas com suporte a texto e upload de PDF |
| IA — Conteúdo da Aula | Geração, revisão e confirmação de conteúdo via IA |
| Quiz — Manual | Criação manual de quiz, perguntas e alternativas |
| Quiz — IA | Geração, revisão e confirmação de quiz via IA |

---

## Fluxo Completo da Aplicação

### 1. Criar o Curso

```
POST /courses
Content-Type: multipart/form-data

dados: { "title": "...", "category": "...", "description": "..." }
imagem: (opcional)
```

### 2. Criar o Módulo

```
POST /courses/{courseId}/modules
Content-Type: multipart/form-data

dados: { "name": "..." }
imagem: (opcional)
```

### 3. Criar as Aulas

Aula com conteúdo em texto:

```
POST /modules/{moduleId}/lessons
Content-Type: multipart/form-data

dados: { "name": "...", "contentEditor": "Texto da aula..." }
```

Aula com PDF (a IA extrai o texto automaticamente):

```
POST /modules/{moduleId}/lessons
Content-Type: multipart/form-data

dados: { "name": "..." }
arquivo: arquivo.pdf
```

### 4. Gerar Conteúdo com IA

A IA usa o `contentEditor` ou o texto extraído do PDF como base para gerar conteúdo HTML estruturado.

```
POST /lessons/{id}/gerar-conteudo     → gera e mantém pendente
GET  /lessons/{id}/conteudo-pendente  → visualiza o conteúdo gerado
POST /lessons/{id}/confirmar-conteudo → salva no banco
POST /lessons/{id}/regerar-conteudo   → regera se não gostar
```

### 5. Gerar Quiz com IA

A IA usa o conteúdo das aulas do módulo para gerar perguntas de múltipla escolha.

```
POST /modules/{moduleId}/quiz/gerar?quantidade=5  → gera e mantém pendente
GET  /modules/{moduleId}/quiz/pendente             → visualiza o quiz gerado
POST /modules/{moduleId}/quiz/confirmar            → salva no banco
POST /modules/{moduleId}/quiz/regerar?quantidade=5 → regera se não gostar
```

### 6. Quiz Manual (opcional)

É possível criar quiz manualmente, adicionando perguntas e alternativas individualmente:

```
POST /modules/{moduleId}/quiz
POST /quiz/{quizId}/questions
POST /questions/{questionId}/alternatives
```

---

## Resposta Padrão da API

Todas as respostas seguem o padrão:

```json
{
  "sucesso": true,
  "mensagem": "Mensagem descritiva",
  "dados": {},
  "timestamp": "2026-04-01T10:00:00"
}
```

---

## Rodando os Testes

```bash
mvn test
```

55 testes unitários cobrindo todos os serviços: `CourseService`, `ModuleService`, `LessonService`, `LessonAiService`, `QuizService` e `QuizAiService`.

---

## Estrutura de Pacotes

```
br.ufc.llm
  course    → controller · service · repository · domain · dto · exception
  module    → controller · service · repository · domain · dto · exception
  lesson    → controller · service · repository · domain · dto · exception
  quiz      → controller · service · repository · domain · dto · exception
  shared    → config · dto · exception
```
