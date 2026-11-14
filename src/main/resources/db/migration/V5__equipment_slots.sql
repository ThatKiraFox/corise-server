-- V5: слот щита у персонажа и тип хвата оружия.
-- Щит нельзя использовать с двуручным или древковым оружием: правило проверяется в сервисе
-- (оно затрагивает две таблицы, обычным CHECK его не выразить).

ALTER TABLE character
    ADD COLUMN equipped_shield_id BIGINT REFERENCES armour(id) ON DELETE SET NULL;

ALTER TABLE weapon
    ADD COLUMN grip VARCHAR(20) NOT NULL DEFAULT 'one_handed';

ALTER TABLE weapon
    ADD CONSTRAINT weapon_grip_valid CHECK (grip IN ('one_handed', 'two_handed', 'polearm'));
