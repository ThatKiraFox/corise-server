-- V1: начальная схема CoriseDND.
-- Правила удаления:
--   CASCADE   - строки, принадлежащие персонажу/родителю (связующие таблицы, личные данные).
--   RESTRICT  - справочники (раса, класс, предмет...): нельзя удалить, пока на них ссылаются.
--   SET NULL  - необязательные ссылки (надетая броня/оружие, пререквизит узла).

-- ---------- Пользователи ----------
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_email_format CHECK (email ~* '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'),
    CONSTRAINT users_username_len CHECK (char_length(username) >= 3)
);
CREATE UNIQUE INDEX users_email_lower_uq ON users (lower(email));

-- ---------- Справочники ----------
CREATE TABLE race (
    id            BIGSERIAL PRIMARY KEY,
    out_name      VARCHAR(100) NOT NULL,
    in_name       VARCHAR(100) NOT NULL,
    short_desc    TEXT NOT NULL DEFAULT '',
    appearance    TEXT NOT NULL DEFAULT '',
    lore          TEXT NOT NULL DEFAULT '',
    relationships TEXT NOT NULL DEFAULT '',
    status        VARCHAR(50)  NOT NULL DEFAULT '',
    archetype     VARCHAR(50)  NOT NULL DEFAULT '',
    lifespan      INT,
    is_custom     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT race_names_not_blank CHECK (btrim(out_name) <> '' AND btrim(in_name) <> ''),
    CONSTRAINT race_lifespan_positive CHECK (lifespan IS NULL OR lifespan > 0)
);

CREATE TABLE heritage_nodes (
    id                   BIGSERIAL PRIMARY KEY,
    race_id              BIGINT NOT NULL REFERENCES race(id) ON DELETE CASCADE,
    branch_name          VARCHAR(100) NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    level_required       INT NOT NULL DEFAULT 1,
    description          TEXT NOT NULL DEFAULT '',
    prerequisite_node_id BIGINT REFERENCES heritage_nodes(id) ON DELETE SET NULL,
    is_custom            BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT heritage_level_positive CHECK (level_required >= 1),
    CONSTRAINT heritage_not_self_prereq CHECK (prerequisite_node_id IS NULL OR prerequisite_node_id <> id)
);

CREATE TABLE effect (
    id          BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(30)  NOT NULL,
    source_id   BIGINT       NOT NULL,
    effect_type VARCHAR(50)  NOT NULL,
    target_key  VARCHAR(100) NOT NULL,
    value       INT,
    CONSTRAINT effect_source_type_valid CHECK (source_type IN
        ('race', 'heritage_node', 'class', 'specialization', 'item', 'spell', 'backstory'))
);

CREATE TABLE language (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL DEFAULT '',
    is_custom   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE instrument (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL DEFAULT '',
    is_custom   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE culture (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL DEFAULT '',
    is_custom   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE culture_languages (
    id          BIGSERIAL PRIMARY KEY,
    culture_id  BIGINT NOT NULL REFERENCES culture(id)  ON DELETE CASCADE,
    language_id BIGINT NOT NULL REFERENCES language(id) ON DELETE RESTRICT,
    CONSTRAINT culture_languages_uq UNIQUE (culture_id, language_id)
);

CREATE TABLE religion (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL DEFAULT '',
    is_custom   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE backstory (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL DEFAULT '',
    is_custom   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE backstory_instruments (
    id            BIGSERIAL PRIMARY KEY,
    backstory_id  BIGINT NOT NULL REFERENCES backstory(id)  ON DELETE CASCADE,
    instrument_id BIGINT NOT NULL REFERENCES instrument(id) ON DELETE RESTRICT,
    CONSTRAINT backstory_instruments_uq UNIQUE (backstory_id, instrument_id)
);

CREATE TABLE lore_entity (
    id        BIGSERIAL PRIMARY KEY,
    category  VARCHAR(50)  NOT NULL,
    title     VARCHAR(200) NOT NULL,
    body      TEXT NOT NULL,
    is_custom BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE attribute (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE skill (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  TEXT NOT NULL DEFAULT '',
    attribute_id BIGINT NOT NULL REFERENCES attribute(id) ON DELETE RESTRICT
);

CREATE TABLE component (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    requirement TEXT NOT NULL DEFAULT '',
    cost        TEXT,
    is_custom   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE spell (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    time        VARCHAR(50)  NOT NULL DEFAULT '',
    type        VARCHAR(50)  NOT NULL DEFAULT '',
    distance    INT NOT NULL DEFAULT 0,
    effect      TEXT NOT NULL DEFAULT '',
    cost        INT NOT NULL DEFAULT 0,
    description TEXT NOT NULL DEFAULT '',
    is_custom   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT spell_distance_nonneg CHECK (distance >= 0),
    CONSTRAINT spell_cost_nonneg CHECK (cost >= 0)
);

CREATE TABLE spell_components (
    id           BIGSERIAL PRIMARY KEY,
    spell_id     BIGINT NOT NULL REFERENCES spell(id)     ON DELETE CASCADE,
    component_id BIGINT NOT NULL REFERENCES component(id) ON DELETE RESTRICT,
    CONSTRAINT spell_components_uq UNIQUE (spell_id, component_id)
);

-- ---------- Предметы ----------
CREATE TABLE item (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    weight      INT NOT NULL DEFAULT 0,
    value       INT NOT NULL DEFAULT 0,
    is_custom   BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT NOT NULL DEFAULT '',
    use_type    VARCHAR(50),
    CONSTRAINT item_weight_nonneg CHECK (weight >= 0),
    CONSTRAINT item_value_nonneg CHECK (value >= 0)
);

CREATE TABLE armour (
    id                BIGSERIAL PRIMARY KEY,
    damage_mitigation INT NOT NULL DEFAULT 0,
    requirements      TEXT NOT NULL DEFAULT '',
    armour_type       VARCHAR(50) NOT NULL,
    disadvantages     TEXT NOT NULL DEFAULT '',
    resistance        TEXT NOT NULL DEFAULT '',
    vulnerability     TEXT NOT NULL DEFAULT '',
    item_id           BIGINT NOT NULL UNIQUE REFERENCES item(id) ON DELETE CASCADE,
    CONSTRAINT armour_mitigation_nonneg CHECK (damage_mitigation >= 0)
);

CREATE TABLE weapon (
    id           BIGSERIAL PRIMARY KEY,
    damage       VARCHAR(50) NOT NULL,
    damage_type  VARCHAR(50) NOT NULL,
    weapon_skill BIGINT REFERENCES skill(id) ON DELETE SET NULL,
    item_id      BIGINT NOT NULL UNIQUE REFERENCES item(id) ON DELETE CASCADE
);

-- ---------- Классы ----------
CREATE TABLE class (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL UNIQUE,
    social_status  INT NOT NULL DEFAULT 0,
    description    TEXT NOT NULL DEFAULT '',
    primary_skills TEXT NOT NULL DEFAULT '',
    is_custom      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE specialization (
    id                     BIGSERIAL PRIMARY KEY,
    class_id               BIGINT NOT NULL REFERENCES class(id) ON DELETE CASCADE,
    name                   VARCHAR(100) NOT NULL,
    description            TEXT NOT NULL DEFAULT '',
    social_status_override INT,
    focused_skill          VARCHAR(100),
    is_custom              BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT specialization_uq UNIQUE (class_id, name)
);

CREATE TABLE specialization_task (
    id                BIGSERIAL PRIMARY KEY,
    specialization_id BIGINT NOT NULL REFERENCES specialization(id) ON DELETE CASCADE,
    name              VARCHAR(100) NOT NULL,
    description       TEXT NOT NULL DEFAULT '',
    goal              TEXT NOT NULL DEFAULT '',
    is_custom         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE progression (
    id                BIGSERIAL PRIMARY KEY,
    class_id          BIGINT NOT NULL REFERENCES class(id) ON DELETE CASCADE,
    specialization_id BIGINT REFERENCES specialization(id) ON DELETE CASCADE,
    lvl_to_get        INT NOT NULL,
    description       TEXT NOT NULL DEFAULT '',
    is_custom         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT progression_level_positive CHECK (lvl_to_get >= 1)
);

-- ---------- Персонаж ----------
CREATE TABLE character (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    race_id             BIGINT NOT NULL REFERENCES race(id)     ON DELETE RESTRICT,
    culture_id          BIGINT REFERENCES culture(id)           ON DELETE RESTRICT,
    religion_id         BIGINT REFERENCES religion(id)          ON DELETE RESTRICT,
    backstory_id        BIGINT REFERENCES backstory(id)         ON DELETE RESTRICT,
    level               INT NOT NULL DEFAULT 1,
    exp                 INT NOT NULL DEFAULT 0,
    hp_cur              INT NOT NULL DEFAULT 0,
    ml_cur              INT NOT NULL DEFAULT 0,
    equipped_armour_id  BIGINT REFERENCES armour(id)            ON DELETE SET NULL,
    equipped_weapon_id  BIGINT REFERENCES weapon(id)            ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT character_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT character_level_positive CHECK (level >= 1),
    CONSTRAINT character_exp_nonneg CHECK (exp >= 0),
    CONSTRAINT character_hp_nonneg CHECK (hp_cur >= 0),
    CONSTRAINT character_ml_nonneg CHECK (ml_cur >= 0)
);

CREATE TABLE personality (
    id                 BIGSERIAL PRIMARY KEY,
    character_id       BIGINT NOT NULL UNIQUE REFERENCES character(id) ON DELETE CASCADE,
    biography          TEXT NOT NULL DEFAULT '',
    goal               TEXT NOT NULL DEFAULT '',
    unique_traits      TEXT NOT NULL DEFAULT '',
    personality_traits TEXT NOT NULL DEFAULT '',
    personal_values    TEXT NOT NULL DEFAULT '',
    personal_evils     TEXT NOT NULL DEFAULT ''
);

CREATE TABLE character_heritage_nodes (
    id               BIGSERIAL PRIMARY KEY,
    character_id     BIGINT NOT NULL REFERENCES character(id)      ON DELETE CASCADE,
    heritage_node_id BIGINT NOT NULL REFERENCES heritage_nodes(id) ON DELETE RESTRICT,
    CONSTRAINT character_heritage_nodes_uq UNIQUE (character_id, heritage_node_id)
);

CREATE TABLE character_languages (
    id           BIGSERIAL PRIMARY KEY,
    character_id BIGINT NOT NULL REFERENCES character(id) ON DELETE CASCADE,
    language_id  BIGINT NOT NULL REFERENCES language(id)  ON DELETE RESTRICT,
    CONSTRAINT character_languages_uq UNIQUE (character_id, language_id)
);

CREATE TABLE character_instruments (
    id            BIGSERIAL PRIMARY KEY,
    character_id  BIGINT NOT NULL REFERENCES character(id)  ON DELETE CASCADE,
    instrument_id BIGINT NOT NULL REFERENCES instrument(id) ON DELETE RESTRICT,
    CONSTRAINT character_instruments_uq UNIQUE (character_id, instrument_id)
);

CREATE TABLE character_spells (
    id           BIGSERIAL PRIMARY KEY,
    character_id BIGINT NOT NULL REFERENCES character(id) ON DELETE CASCADE,
    spell_id     BIGINT NOT NULL REFERENCES spell(id)     ON DELETE RESTRICT,
    CONSTRAINT character_spells_uq UNIQUE (character_id, spell_id)
);

CREATE TABLE character_skills (
    id                BIGSERIAL PRIMARY KEY,
    character_id      BIGINT NOT NULL REFERENCES character(id) ON DELETE CASCADE,
    skill_id          BIGINT NOT NULL REFERENCES skill(id)     ON DELETE RESTRICT,
    proficiency_level INT NOT NULL DEFAULT 0,
    CONSTRAINT character_skills_uq UNIQUE (character_id, skill_id),
    CONSTRAINT character_skills_level_nonneg CHECK (proficiency_level >= 0)
);

CREATE TABLE character_attribute (
    id           BIGSERIAL PRIMARY KEY,
    character_id BIGINT NOT NULL REFERENCES character(id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES attribute(id) ON DELETE RESTRICT,
    value        INT NOT NULL DEFAULT 0,
    CONSTRAINT character_attribute_uq UNIQUE (character_id, attribute_id),
    CONSTRAINT character_attribute_value_nonneg CHECK (value >= 0)
);

CREATE TABLE character_items (
    id           BIGSERIAL PRIMARY KEY,
    character_id BIGINT NOT NULL REFERENCES character(id) ON DELETE CASCADE,
    item_id      BIGINT NOT NULL REFERENCES item(id)      ON DELETE RESTRICT,
    amount       INT NOT NULL DEFAULT 1,
    CONSTRAINT character_items_uq UNIQUE (character_id, item_id),
    CONSTRAINT character_items_amount_positive CHECK (amount > 0)
);

CREATE TABLE character_classes (
    id                            BIGSERIAL PRIMARY KEY,
    character_id                  BIGINT NOT NULL REFERENCES character(id)       ON DELETE CASCADE,
    class_id                      BIGINT NOT NULL REFERENCES class(id)           ON DELETE RESTRICT,
    specialization_id             BIGINT REFERENCES specialization(id)           ON DELETE RESTRICT,
    class_level                   INT NOT NULL DEFAULT 1,
    specialization_task_completed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT character_classes_uq UNIQUE (character_id, class_id),
    CONSTRAINT character_classes_level_positive CHECK (class_level >= 1),
    CONSTRAINT character_classes_task_needs_spec
        CHECK (NOT specialization_task_completed OR specialization_id IS NOT NULL)
);

-- ---------- Индексы ----------
-- Postgres не индексирует внешние ключи сам; индексы ниже - только под реальные запросы.
-- Список персонажей пользователя с поиском/сортировкой по имени:
CREATE INDEX idx_character_user_name ON character (user_id, name);
-- Обратный поиск "у кого есть это заклинание" и join со справочником:
CREATE INDEX idx_character_spells_spell ON character_spells (spell_id);
-- Фильтры справочников в мастере создания:
CREATE INDEX idx_spell_type ON spell (type);
CREATE INDEX idx_item_category ON item (category);
-- Join "узлы наследия расы" и "прогрессия класса по уровню":
CREATE INDEX idx_heritage_nodes_race ON heritage_nodes (race_id);
CREATE INDEX idx_progression_class_lvl ON progression (class_id, lvl_to_get);
-- Полиморфная ссылка эффектов:
CREATE INDEX idx_effect_source ON effect (source_type, source_id);
