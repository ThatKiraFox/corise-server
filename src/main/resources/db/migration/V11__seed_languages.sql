-- V11: языки Corise и языки культур.
-- Язык культуры персонаж знает по умолчанию (при создании записывается автоматически).

CREATE TEMP TABLE seed_culture_language (
    language_name VARCHAR(100) NOT NULL,
    culture_name  VARCHAR(100) NOT NULL
) ON COMMIT DROP;

INSERT INTO seed_culture_language (language_name, culture_name) VALUES
('Имперский Патульякский', 'Амерэг''Ли'),
('Имперский Патульякский', 'Увартион''Ли'),
('Наурский', 'Науры'),
('Кодировка Создателя', 'Последователи Общего Разума'),
('Фоксфордский Диалект', 'Срах''Ари'),
('Умэйский Шадонарский', 'Чари'),
('Язык жестов', 'Энотнас'),
('Туртойский', 'Римур'),
('Древне Уманский', 'Холинцы');

-- Если название культуры не совпало с V8, миграция остановится, а не пропустит запись молча
DO $$
DECLARE
missing TEXT;
BEGIN
SELECT string_agg(s.culture_name, ', ') INTO missing
FROM seed_culture_language s
WHERE NOT EXISTS (
    SELECT 1 FROM culture c WHERE c.name = s.culture_name AND c.is_custom = FALSE
);
IF missing IS NOT NULL THEN
        RAISE EXCEPTION 'Культуры не найдены: %', missing;
END IF;
END $$;

INSERT INTO language (name)
SELECT DISTINCT s.language_name
FROM seed_culture_language s
WHERE NOT EXISTS (
    SELECT 1 FROM language l WHERE l.name = s.language_name AND l.is_custom = FALSE
);

INSERT INTO culture_languages (culture_id, language_id)
SELECT c.id, l.id
FROM seed_culture_language s
         JOIN culture c  ON c.name = s.culture_name  AND c.is_custom = FALSE
         JOIN language l ON l.name = s.language_name AND l.is_custom = FALSE
    ON CONFLICT (culture_id, language_id) DO NOTHING;