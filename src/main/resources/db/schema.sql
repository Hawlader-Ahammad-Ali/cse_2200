-- =============================================================================
-- MindMap — Database Schema v1
-- SQLite. Executed once on first launch by SchemaInitializer.
-- =============================================================================

PRAGMA foreign_keys = ON;

-- =============================================================================
-- SCHEMA VERSION (for future migrations)
-- =============================================================================
CREATE TABLE schema_version (
                                version     INTEGER PRIMARY KEY,
                                applied_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

-- =============================================================================
-- USERS
-- =============================================================================
CREATE TABLE users (
                       id              INTEGER PRIMARY KEY AUTOINCREMENT,
                       username        TEXT NOT NULL UNIQUE,
                       email           TEXT NOT NULL UNIQUE,
                       password_hash   TEXT NOT NULL,
                       salt            TEXT NOT NULL,
                       display_name    TEXT,
                       avatar_path     TEXT,
                       created_at      TEXT NOT NULL DEFAULT (datetime('now')),
                       updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
                       last_login      TEXT
);

CREATE TABLE user_preferences (
                                  user_id              INTEGER PRIMARY KEY,
                                  theme                TEXT NOT NULL DEFAULT 'LIGHT',
                                  language             TEXT NOT NULL DEFAULT 'en',
                                  daily_review_target  INTEGER DEFAULT 20,
                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================================
-- TAGS
-- =============================================================================
CREATE TABLE tags (
                      id          INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id     INTEGER NOT NULL,
                      name        TEXT NOT NULL,
                      color       TEXT DEFAULT '#7E57C2',
                      created_at  TEXT NOT NULL DEFAULT (datetime('now')),
                      UNIQUE (user_id, name),
                      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_tags_user ON tags(user_id);

-- =============================================================================
-- SOURCES
-- =============================================================================
CREATE TABLE sources (
                         id              INTEGER PRIMARY KEY AUTOINCREMENT,
                         user_id         INTEGER NOT NULL,
                         title           TEXT NOT NULL,
                         source_type     TEXT NOT NULL,
                         author_creator  TEXT,
                         description     TEXT,
                         url             TEXT,
                         cover_image     TEXT,
                         date_added      TEXT NOT NULL DEFAULT (datetime('now')),
                         date_consumed   TEXT,
                         rating          INTEGER,
                         status          TEXT NOT NULL DEFAULT 'PLANNED',
                         notes           TEXT,
                         created_at      TEXT NOT NULL DEFAULT (datetime('now')),
                         updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                         CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','ABANDONED')),
                         CHECK (rating IS NULL OR (rating BETWEEN 1 AND 5))
);
CREATE INDEX idx_sources_user   ON sources(user_id);
CREATE INDEX idx_sources_type   ON sources(source_type);
CREATE INDEX idx_sources_status ON sources(status);
CREATE INDEX idx_sources_added  ON sources(date_added);

CREATE TABLE source_tags (
                             source_id   INTEGER NOT NULL,
                             tag_id      INTEGER NOT NULL,
                             PRIMARY KEY (source_id, tag_id),
                             FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE,
                             FOREIGN KEY (tag_id)    REFERENCES tags(id)    ON DELETE CASCADE
);

-- =============================================================================
-- KNOWLEDGE ITEMS
-- =============================================================================
CREATE TABLE knowledge_items (
                                 id              INTEGER PRIMARY KEY AUTOINCREMENT,
                                 user_id         INTEGER NOT NULL,
                                 title           TEXT NOT NULL,
                                 item_type       TEXT NOT NULL DEFAULT 'CONCEPT',
                                 description     TEXT,
                                 personal_note   TEXT,
                                 category        TEXT,
                                 difficulty      TEXT DEFAULT 'MEDIUM',
                                 importance      TEXT DEFAULT 'MEDIUM',
                                 confidence      INTEGER DEFAULT 3,
                                 origin          TEXT NOT NULL DEFAULT 'USER_CREATED',
                                 date_learned    TEXT,
                                 created_at      TEXT NOT NULL DEFAULT (datetime('now')),
                                 updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
                                 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                 CHECK (item_type IN ('CONCEPT','FACT','SKILL','PRINCIPLE','IDEA','LESSON','INSIGHT','QUESTION')),
                                 CHECK (difficulty IN ('EASY','MEDIUM','HARD')),
                                 CHECK (importance IN ('LOW','MEDIUM','HIGH')),
                                 CHECK (confidence BETWEEN 1 AND 5),
                                 CHECK (origin IN ('AI_SUGGESTED','USER_CONFIRMED','USER_CREATED'))
);
CREATE INDEX idx_ki_user     ON knowledge_items(user_id);
CREATE INDEX idx_ki_category ON knowledge_items(category);
CREATE INDEX idx_ki_type     ON knowledge_items(item_type);
CREATE INDEX idx_ki_origin   ON knowledge_items(origin);

CREATE TABLE knowledge_tags (
                                knowledge_item_id INTEGER NOT NULL,
                                tag_id            INTEGER NOT NULL,
                                PRIMARY KEY (knowledge_item_id, tag_id),
                                FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE,
                                FOREIGN KEY (tag_id)            REFERENCES tags(id)            ON DELETE CASCADE
);

-- =============================================================================
-- SOURCE <-> KNOWLEDGE LINK (powers "Where did I learn this?")
-- =============================================================================
CREATE TABLE source_knowledge (
                                  source_id          INTEGER NOT NULL,
                                  knowledge_item_id  INTEGER NOT NULL,
                                  link_type          TEXT NOT NULL DEFAULT 'LEARNED_FROM',
                                  created_at         TEXT NOT NULL DEFAULT (datetime('now')),
                                  PRIMARY KEY (source_id, knowledge_item_id),
                                  FOREIGN KEY (source_id)         REFERENCES sources(id)         ON DELETE CASCADE,
                                  FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE,
                                  CHECK (link_type IN ('LEARNED_FROM','MENTIONED_IN','RELATED_TO'))
);
CREATE INDEX idx_sk_knowledge ON source_knowledge(knowledge_item_id);

-- =============================================================================
-- KNOWLEDGE GRAPH EDGES
-- =============================================================================
CREATE TABLE knowledge_connections (
                                       id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                                       user_id             INTEGER NOT NULL,
                                       source_item_id      INTEGER NOT NULL,
                                       target_item_id      INTEGER NOT NULL,
                                       relationship_type   TEXT NOT NULL,
                                       notes               TEXT,
                                       weight              REAL DEFAULT 1.0,
                                       created_at          TEXT NOT NULL DEFAULT (datetime('now')),
                                       UNIQUE (source_item_id, target_item_id, relationship_type),
                                       CHECK (source_item_id <> target_item_id),
                                       CHECK (relationship_type IN (
                                                                    'RELATED_TO','PREREQUISITE_OF','PART_OF','CAUSES',
                                                                    'CONTRASTS_WITH','EXAMPLE_OF','LEARNED_FROM'
                                           )),
                                       FOREIGN KEY (user_id)        REFERENCES users(id)           ON DELETE CASCADE,
                                       FOREIGN KEY (source_item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE,
                                       FOREIGN KEY (target_item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE
);
CREATE INDEX idx_kc_source ON knowledge_connections(source_item_id);
CREATE INDEX idx_kc_target ON knowledge_connections(target_item_id);

-- =============================================================================
-- FLASHCARDS (must come before reviews)
-- =============================================================================
CREATE TABLE flashcards (
                            id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                            user_id            INTEGER NOT NULL,
                            knowledge_item_id  INTEGER,
                            question           TEXT NOT NULL,
                            answer             TEXT NOT NULL,
                            origin             TEXT NOT NULL DEFAULT 'USER_CREATED',
                            created_at         TEXT NOT NULL DEFAULT (datetime('now')),
                            updated_at         TEXT NOT NULL DEFAULT (datetime('now')),
                            FOREIGN KEY (user_id)           REFERENCES users(id)           ON DELETE CASCADE,
                            FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_items(id) ON DELETE SET NULL,
                            CHECK (origin IN ('AI_SUGGESTED','USER_CONFIRMED','USER_CREATED'))
);
CREATE INDEX idx_flashcards_user ON flashcards(user_id);

-- =============================================================================
-- SPACED REPETITION (SM-2)
-- =============================================================================
CREATE TABLE reviews (
                         id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                         user_id            INTEGER NOT NULL,
                         knowledge_item_id  INTEGER,
                         flashcard_id       INTEGER,
                         ease_factor        REAL NOT NULL DEFAULT 2.5,
                         interval_days      INTEGER NOT NULL DEFAULT 0,
                         repetitions        INTEGER NOT NULL DEFAULT 0,
                         next_review_date   TEXT NOT NULL,
                         last_review_date   TEXT,
                         quality_last       INTEGER,
                         created_at         TEXT NOT NULL DEFAULT (datetime('now')),
                         updated_at         TEXT NOT NULL DEFAULT (datetime('now')),
                         CHECK (
                             (knowledge_item_id IS NOT NULL AND flashcard_id IS NULL) OR
                             (knowledge_item_id IS NULL     AND flashcard_id IS NOT NULL)
                             ),
                         CHECK (quality_last IS NULL OR (quality_last BETWEEN 0 AND 5)),
                         FOREIGN KEY (user_id)           REFERENCES users(id)           ON DELETE CASCADE,
                         FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_items(id) ON DELETE CASCADE,
                         FOREIGN KEY (flashcard_id)      REFERENCES flashcards(id)      ON DELETE CASCADE
);
CREATE INDEX idx_reviews_due ON reviews(user_id, next_review_date);

CREATE TABLE review_logs (
                             id          INTEGER PRIMARY KEY AUTOINCREMENT,
                             review_id   INTEGER NOT NULL,
                             quality     INTEGER NOT NULL,
                             reviewed_at TEXT NOT NULL DEFAULT (datetime('now')),
                             CHECK (quality BETWEEN 0 AND 5),
                             FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
);
CREATE INDEX idx_review_logs_review ON review_logs(review_id);

-- =============================================================================
-- QUIZ
-- =============================================================================
CREATE TABLE quiz_questions (
                                id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                                user_id            INTEGER NOT NULL,
                                knowledge_item_id  INTEGER,
                                question_type      TEXT NOT NULL,
                                question_text      TEXT NOT NULL,
                                options_json       TEXT,
                                correct_answer     TEXT NOT NULL,
                                explanation        TEXT,
                                origin             TEXT NOT NULL DEFAULT 'USER_CREATED',
                                created_at         TEXT NOT NULL DEFAULT (datetime('now')),
                                FOREIGN KEY (user_id)           REFERENCES users(id)           ON DELETE CASCADE,
                                FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_items(id) ON DELETE SET NULL,
                                CHECK (question_type IN ('MCQ','TRUE_FALSE','SHORT')),
                                CHECK (origin IN ('AI_SUGGESTED','USER_CONFIRMED','USER_CREATED'))
);
CREATE INDEX idx_qq_user ON quiz_questions(user_id);

CREATE TABLE quiz_attempts (
                               id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                               user_id            INTEGER NOT NULL,
                               quiz_question_id   INTEGER NOT NULL,
                               user_answer        TEXT,
                               is_correct         INTEGER NOT NULL,
                               attempted_at       TEXT NOT NULL DEFAULT (datetime('now')),
                               FOREIGN KEY (user_id)          REFERENCES users(id)          ON DELETE CASCADE,
                               FOREIGN KEY (quiz_question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE,
                               CHECK (is_correct IN (0,1))
);
CREATE INDEX idx_qa_user ON quiz_attempts(user_id, attempted_at);

-- =============================================================================
-- STUDY SESSIONS (streak & analytics)
-- =============================================================================
CREATE TABLE study_sessions (
                                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                                user_id         INTEGER NOT NULL,
                                session_type    TEXT NOT NULL,
                                start_time      TEXT NOT NULL DEFAULT (datetime('now')),
                                end_time        TEXT,
                                items_reviewed  INTEGER DEFAULT 0,
                                correct_count   INTEGER DEFAULT 0,
                                notes           TEXT,
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                CHECK (session_type IN ('REVIEW','QUIZ','EXPLORE'))
);
CREATE INDEX idx_sessions_user ON study_sessions(user_id, start_time);

-- =============================================================================
-- GOALS
-- =============================================================================
CREATE TABLE goals (
                       id             INTEGER PRIMARY KEY AUTOINCREMENT,
                       user_id        INTEGER NOT NULL,
                       title          TEXT NOT NULL,
                       description    TEXT,
                       target_count   INTEGER,
                       current_count  INTEGER DEFAULT 0,
                       deadline       TEXT,
                       status         TEXT NOT NULL DEFAULT 'ACTIVE',
                       created_at     TEXT NOT NULL DEFAULT (datetime('now')),
                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                       CHECK (status IN ('ACTIVE','DONE','ABANDONED'))
);
CREATE INDEX idx_goals_user ON goals(user_id);

-- =============================================================================
-- ATTACHMENTS
-- =============================================================================
CREATE TABLE attachments (
                             id           INTEGER PRIMARY KEY AUTOINCREMENT,
                             user_id      INTEGER NOT NULL,
                             source_id    INTEGER,
                             file_path    TEXT NOT NULL,
                             file_type    TEXT,
                             file_size    INTEGER,
                             created_at   TEXT NOT NULL DEFAULT (datetime('now')),
                             FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
                             FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE
);
CREATE INDEX idx_attachments_source ON attachments(source_id);

-- =============================================================================
-- AI SUGGESTIONS (raw staging — user must approve before it becomes knowledge)
-- =============================================================================
CREATE TABLE ai_suggestions (
                                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                                user_id      INTEGER NOT NULL,
                                source_id    INTEGER NOT NULL,
                                payload_json TEXT NOT NULL,
                                status       TEXT NOT NULL DEFAULT 'PENDING',
                                created_at   TEXT NOT NULL DEFAULT (datetime('now')),
                                FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
                                FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE,
                                CHECK (status IN ('PENDING','ACCEPTED','PARTIAL','REJECTED'))
);
CREATE INDEX idx_ai_sugg_user   ON ai_suggestions(user_id);
CREATE INDEX idx_ai_sugg_source ON ai_suggestions(source_id);

-- =============================================================================
-- RECORD SCHEMA VERSION
-- =============================================================================
INSERT INTO schema_version (version) VALUES (1);