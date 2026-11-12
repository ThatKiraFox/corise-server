-- V2: заметки (UC-13) и владелец пользовательского контента (UC-14, UC-15).

CREATE TABLE note (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    character_id BIGINT REFERENCES character(id)          ON DELETE SET NULL,
    title        VARCHAR(200) NOT NULL,
    body         TEXT NOT NULL DEFAULT '',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT note_title_not_blank CHECK (btrim(title) <> '')
);
CREATE INDEX idx_note_user_updated ON note (user_id, updated_at DESC);

-- Владелец есть только у пользовательского контента основных сущностей.
-- При удалении пользователя контент остаётся (SET NULL), чтобы не ломать чужих персонажей.
ALTER TABLE race  ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE class ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE spell ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE item  ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE race  ADD CONSTRAINT race_owner_only_custom  CHECK (is_custom OR created_by IS NULL);
ALTER TABLE class ADD CONSTRAINT class_owner_only_custom CHECK (is_custom OR created_by IS NULL);
ALTER TABLE spell ADD CONSTRAINT spell_owner_only_custom CHECK (is_custom OR created_by IS NULL);
ALTER TABLE item  ADD CONSTRAINT item_owner_only_custom  CHECK (is_custom OR created_by IS NULL);
