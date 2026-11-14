-- V4: владелец пользовательского контента для всех таблиц с is_custom
-- (раса, класс, заклинание и предмет получили его в V2).
-- Правило видимости (в приложении): стандартный контент (is_custom = false) виден всем,
-- пользовательский - только автору.

ALTER TABLE culture         ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE religion        ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE backstory       ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE language        ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE instrument      ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE heritage_nodes  ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE lore_entity     ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE component       ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE specialization  ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE specialization_task ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE progression     ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE culture         ADD CONSTRAINT culture_owner_only_custom         CHECK (is_custom OR created_by IS NULL);
ALTER TABLE religion        ADD CONSTRAINT religion_owner_only_custom        CHECK (is_custom OR created_by IS NULL);
ALTER TABLE backstory       ADD CONSTRAINT backstory_owner_only_custom       CHECK (is_custom OR created_by IS NULL);
ALTER TABLE language        ADD CONSTRAINT language_owner_only_custom        CHECK (is_custom OR created_by IS NULL);
ALTER TABLE instrument      ADD CONSTRAINT instrument_owner_only_custom      CHECK (is_custom OR created_by IS NULL);
ALTER TABLE heritage_nodes  ADD CONSTRAINT heritage_nodes_owner_only_custom  CHECK (is_custom OR created_by IS NULL);
ALTER TABLE lore_entity     ADD CONSTRAINT lore_entity_owner_only_custom     CHECK (is_custom OR created_by IS NULL);
ALTER TABLE component       ADD CONSTRAINT component_owner_only_custom       CHECK (is_custom OR created_by IS NULL);
ALTER TABLE specialization  ADD CONSTRAINT specialization_owner_only_custom  CHECK (is_custom OR created_by IS NULL);
ALTER TABLE specialization_task ADD CONSTRAINT specialization_task_owner_only_custom CHECK (is_custom OR created_by IS NULL);
ALTER TABLE progression     ADD CONSTRAINT progression_owner_only_custom     CHECK (is_custom OR created_by IS NULL);

-- Уникальность имён: раньше была глобальной (UNIQUE name). Теперь имя уникально
-- среди стандартного контента и отдельно среди контента каждого автора, чтобы
-- два пользователя могли создать "Изгнанник", а чужой контент не выдавал себя конфликтом.
ALTER TABLE language   DROP CONSTRAINT language_name_key;
ALTER TABLE instrument DROP CONSTRAINT instrument_name_key;
ALTER TABLE culture    DROP CONSTRAINT culture_name_key;
ALTER TABLE religion   DROP CONSTRAINT religion_name_key;
ALTER TABLE backstory  DROP CONSTRAINT backstory_name_key;
ALTER TABLE class      DROP CONSTRAINT class_name_key;

CREATE UNIQUE INDEX language_name_std_uq   ON language   (name) WHERE NOT is_custom;
CREATE UNIQUE INDEX instrument_name_std_uq ON instrument (name) WHERE NOT is_custom;
CREATE UNIQUE INDEX culture_name_std_uq    ON culture    (name) WHERE NOT is_custom;
CREATE UNIQUE INDEX religion_name_std_uq   ON religion   (name) WHERE NOT is_custom;
CREATE UNIQUE INDEX backstory_name_std_uq  ON backstory  (name) WHERE NOT is_custom;
CREATE UNIQUE INDEX class_name_std_uq      ON class      (name) WHERE NOT is_custom;

CREATE UNIQUE INDEX language_name_owner_uq   ON language   (created_by, name) WHERE is_custom;
CREATE UNIQUE INDEX instrument_name_owner_uq ON instrument (created_by, name) WHERE is_custom;
CREATE UNIQUE INDEX culture_name_owner_uq    ON culture    (created_by, name) WHERE is_custom;
CREATE UNIQUE INDEX religion_name_owner_uq   ON religion   (created_by, name) WHERE is_custom;
CREATE UNIQUE INDEX backstory_name_owner_uq  ON backstory  (created_by, name) WHERE is_custom;
CREATE UNIQUE INDEX class_name_owner_uq      ON class      (created_by, name) WHERE is_custom;
