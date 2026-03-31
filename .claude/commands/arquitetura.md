# Arquitetura — poc-llm-ufc-simples

## Entidades e Relacionamentos

```
Course   (1) ──> (N) Module      [course_id FK, ON DELETE CASCADE]
Module   (1) ──> (N) Lesson      [module_id FK, ON DELETE CASCADE]
Module   (1) ──> (0..1) Quiz     [module_id FK UNIQUE, ON DELETE CASCADE]
Quiz     (1) ──> (N) Question    [quiz_id FK, ON DELETE CASCADE]
Question (1) ──> (N) Alternative [question_id FK, ON DELETE CASCADE]
```

## Tabelas do Banco (PostgreSQL)

Schema gerado pelas migrations Flyway (V1 → V3). Rodar o projeto aplica automaticamente.

```sql
-- V1__cria_tabelas.sql
CREATE TABLE courses (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    category    VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE modules (
    id         BIGSERIAL   PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    order_num  INT         NOT NULL,
    course_id  BIGINT      NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE lessons (
    id                BIGSERIAL    PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    order_num         INT          NOT NULL,
    file_path         VARCHAR(500),
    file_type         VARCHAR(10),              -- somente PDF
    content_editor    TEXT,                     -- texto digitado ou confirmado via IA
    content_generated TEXT,                     -- coluna reservada (pendente vive em memória)
    module_id         BIGINT       NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE quizzes (
    id         BIGSERIAL PRIMARY KEY,
    module_id  BIGINT    NOT NULL UNIQUE REFERENCES modules(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE questions (
    id         BIGSERIAL PRIMARY KEY,
    statement  TEXT      NOT NULL,
    points     INT       NOT NULL DEFAULT 1,
    order_num  INT       NOT NULL,
    quiz_id    BIGINT    NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE alternatives (
    id          BIGSERIAL PRIMARY KEY,
    text        TEXT      NOT NULL,
    correct     BOOLEAN   NOT NULL DEFAULT FALSE,
    question_id BIGINT    NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- V2__adiciona_config_quiz.sql
ALTER TABLE quizzes
    ADD COLUMN show_wrong_answers   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN show_correct_answers BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN show_points          BOOLEAN NOT NULL DEFAULT FALSE;

-- V3__adiciona_imagem_capa.sql
ALTER TABLE courses ADD COLUMN image_path VARCHAR(500);
ALTER TABLE modules ADD COLUMN image_path VARCHAR(500);
```

## Endpoints REST

Apenas GET e POST — sem PATCH, PUT ou DELETE.

### Cursos
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/courses` | Criar curso (multipart: dados + imagem opcional) |
| GET | `/courses` | Listar cursos |
| GET | `/courses/{id}` | Detalhar curso |

### Módulos
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/courses/{id}/modules` | Criar módulo (multipart: dados + imagem opcional) |
| GET | `/courses/{id}/modules` | Listar módulos do curso |
| GET | `/modules/{id}` | Detalhar módulo |

### Aulas
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/modules/{id}/lessons` | Criar aula (multipart: dados + arquivo PDF opcional) |
| GET | `/modules/{id}/lessons` | Listar aulas do módulo |
| GET | `/lessons/{id}` | Detalhar aula |

### IA — Conteúdo da Aula
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/lessons/{id}/gerar-conteudo` | IA gera conteúdo (fica em memória, não salva) |
| GET | `/lessons/{id}/conteudo-pendente` | Ver conteúdo gerado aguardando decisão |
| POST | `/lessons/{id}/confirmar-conteudo` | Salva conteúdo em `content_editor` no banco |
| POST | `/lessons/{id}/regerar-conteudo` | Gera de novo (sobrescreve pendente em memória) |

### Quiz — Manual
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/modules/{id}/quiz` | Criar quiz com perguntas e alternativas no body |
| GET | `/modules/{id}/quiz` | Ver quiz salvo |
| POST | `/quiz/{id}` | Configurar exibição (gabarito, erros, pontuação) |
| POST | `/quiz/{id}/questions` | Adicionar pergunta (mín. 2 alternativas, exatamente 1 correta) |
| GET | `/quiz/{id}/questions` | Listar perguntas |
| POST | `/questions/{id}/alternatives` | Adicionar alternativa |
| GET | `/questions/{id}/alternatives` | Listar alternativas |

### Quiz — IA
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/modules/{id}/quiz/gerar?quantidade=5` | IA gera quiz com base nas aulas (fica em memória) |
| GET | `/modules/{id}/quiz/pendente` | Ver quiz gerado aguardando decisão |
| POST | `/modules/{id}/quiz/confirmar` | Salva quiz no banco (perguntas + alternativas + corretas) |
| POST | `/modules/{id}/quiz/regerar?quantidade=5` | Gera de novo (sobrescreve pendente em memória) |
