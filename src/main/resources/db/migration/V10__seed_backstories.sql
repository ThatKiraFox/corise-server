-- V10: настоящие предыстории Corise (4 предыстории).
-- Временные предыстории V3 заменяются: совпавшие по названию обновляются,
-- остальные временные удаляются, если на них ничего не ссылается.

CREATE TEMP TABLE seed_backstory (
    name        VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_backstory (name, description) VALUES
('Университетский маг',
$t$Ваш персонаж был студентом в магическом университете. После его окончания персонаж не только получил важные знания, но и имеет связи с архимагами и сильными магами на месте своего обучения.$t$),
('Ненавистник Серафимов',
$t$Город или деревню, где жил ваш персонаж, разорили Серафимы, что напали и начали сжигать людей для нахождения чистой души. Ваш персонаж знает больше о Серафимах и намеревается им отомстить.$t$),
('Спасенный восстанием',
$t$Падение Патульякской Империи спасло вашего персонажа от наказания за проступок перед империей. Его отношения с поддерживающими Империю персонажами сложно назвать теплыми.$t$),
('Проповедник Великого Пантеона',
$t$Ваш персонаж какое-то время жил в церкви Великого Пантеона. Ваш персонаж знает много о религиях.$t$);

-- Вставка с обновлением: если предыстория с таким названием уже была (из V3), она приводится к настоящим данным
UPDATE backstory b
SET description = s.description
FROM seed_backstory s
WHERE b.name = s.name AND b.is_custom = FALSE;

INSERT INTO backstory (name, description)
SELECT s.name, s.description
FROM seed_backstory s
WHERE NOT EXISTS (
    SELECT 1 FROM backstory b WHERE b.name = s.name AND b.is_custom = FALSE
);

-- Удаление временных предысторий V3, которых нет в настоящих данных.
-- Если на предысторию что-то ссылается (например, персонаж), она остаётся, а в журнале появляется сообщение.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT id, name FROM backstory
        WHERE is_custom = FALSE
          AND name NOT IN (SELECT name FROM seed_backstory)
    LOOP
        BEGIN
            DELETE FROM backstory WHERE id = r.id;
        EXCEPTION WHEN foreign_key_violation OR restrict_violation THEN
            RAISE NOTICE 'Предыстория "%" оставлена: на нее есть ссылки', r.name;
        END;
    END LOOP;
END $$;
