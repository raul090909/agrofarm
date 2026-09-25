-- Тесты контроля целостности данных БД «Сельхозферма».
-- Каждый тест выполняется в подтранзакции; ожидаемый результат — ошибка ограничения.
\set ON_ERROR_STOP off
\pset pager off
BEGIN;
INSERT INTO users (first_name, last_name, email, password_hash) VALUES ('Иван','Петров','ivan@test.ru','h');
INSERT INTO users (first_name, last_name, email, password_hash) VALUES ('Пётр','Сидоров','petr@test.ru','h');
INSERT INTO farm_units (user_id, unit_name, unit_type, area_ha) SELECT user_id,'Поле №1','field',12.5 FROM users WHERE email='ivan@test.ru';
INSERT INTO categories (cat_name, cat_type, is_global, user_id) SELECT 'Личная статья','expense',FALSE,user_id FROM users WHERE email='petr@test.ru';
COMMIT;

\echo T01 Повторный email (UNIQUE)
INSERT INTO users (first_name, last_name, email, password_hash) VALUES ('Аня','Ко','ivan@test.ru','h');
\echo T02 Некорректный email (CHECK)
INSERT INTO users (first_name, last_name, email, password_hash) VALUES ('Аня','Ко','not-an-email','h');
\echo T03 Недопустимый тип участка (CHECK)
INSERT INTO farm_units (user_id, unit_name, unit_type) SELECT user_id,'X','garage' FROM users WHERE email='ivan@test.ru';
\echo T04 Отрицательная площадь (CHECK)
INSERT INTO farm_units (user_id, unit_name, unit_type, area_ha) SELECT user_id,'Y','field',-1 FROM users WHERE email='ivan@test.ru';
\echo T05 Дубликат названия участка у одного фермера (UNIQUE)
INSERT INTO farm_units (user_id, unit_name, unit_type) SELECT user_id,'Поле №1','field' FROM users WHERE email='ivan@test.ru';
\echo T06 Нулевая сумма операции (CHECK)
INSERT INTO operations (unit_id, category_id, op_type, amount, operation_date) SELECT (SELECT unit_id FROM farm_units LIMIT 1),(SELECT category_id FROM categories WHERE cat_name='Удобрения'),'expense',0,current_date;
\echo T07 Тип операции не совпадает с типом статьи (TRIGGER)
INSERT INTO operations (unit_id, category_id, op_type, amount, operation_date) SELECT (SELECT unit_id FROM farm_units LIMIT 1),(SELECT category_id FROM categories WHERE cat_name='Продажа зерна'),'expense',100,current_date;
\echo T08 Чужая пользовательская статья (TRIGGER)
INSERT INTO operations (unit_id, category_id, op_type, amount, operation_date) SELECT (SELECT unit_id FROM farm_units LIMIT 1),(SELECT category_id FROM categories WHERE cat_name='Личная статья'),'expense',100,current_date;
\echo T09 Количество без единицы измерения (CHECK)
INSERT INTO operations (unit_id, category_id, op_type, amount, quantity, operation_date) SELECT (SELECT unit_id FROM farm_units LIMIT 1),(SELECT category_id FROM categories WHERE cat_name='Удобрения'),'expense',100,50,current_date;
\echo T10 Лимит на статью дохода (TRIGGER)
INSERT INTO budget_limits (user_id, category_id, amount, time_period) SELECT (SELECT user_id FROM users WHERE email='ivan@test.ru'),(SELECT category_id FROM categories WHERE cat_name='Продажа зерна'),1000,'month';
\echo T11 Недопустимый период лимита (CHECK)
INSERT INTO budget_limits (user_id, category_id, amount, time_period) SELECT (SELECT user_id FROM users WHERE email='ivan@test.ru'),(SELECT category_id FROM categories WHERE cat_name='Удобрения'),1000,'year';
\echo T12 Пустая рекомендация (CHECK)
INSERT INTO recommendations (user_id, rec_message) SELECT user_id,'   ' FROM users WHERE email='ivan@test.ru';
\echo T13 Глобальная статья с владельцем (CHECK)
INSERT INTO categories (cat_name, cat_type, is_global, user_id) SELECT 'Z','expense',TRUE,user_id FROM users WHERE email='ivan@test.ru';
\echo T14 Удаление используемой статьи (RESTRICT)
INSERT INTO operations (unit_id, category_id, op_type, amount, operation_date) SELECT (SELECT unit_id FROM farm_units LIMIT 1),(SELECT category_id FROM categories WHERE cat_name='Удобрения'),'expense',500,current_date;
DELETE FROM categories WHERE cat_name='Удобрения';
\echo T15 Каскадное удаление фермера удаляет участки и операции (ожидается 0)
DELETE FROM users WHERE email='ivan@test.ru';
SELECT (SELECT count(*) FROM farm_units) + (SELECT count(*) FROM operations) AS remaining_rows;
\echo T16 Корректная операция принимается (ожидается INSERT 0 1)
BEGIN;
INSERT INTO farm_units (user_id, unit_name, unit_type) SELECT user_id,'Ферма','livestock' FROM users WHERE email='petr@test.ru';
INSERT INTO operations (unit_id, category_id, op_type, amount, quantity, quantity_unit, operation_date) SELECT (SELECT unit_id FROM farm_units LIMIT 1),(SELECT category_id FROM categories WHERE cat_name='Продажа молока'),'income',5400.50,300,'л',current_date;
ROLLBACK;
