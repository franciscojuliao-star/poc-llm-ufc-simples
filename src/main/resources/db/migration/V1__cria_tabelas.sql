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
    file_type         VARCHAR(10),
    content_editor    TEXT,
    content_generated TEXT,
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
