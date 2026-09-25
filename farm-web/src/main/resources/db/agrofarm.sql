-- =====================================================================
--  Информационная система «Сельхозферма»
--  Скрипт создания базы данных PostgreSQL 14+
--  Создаёт таблицы, ограничения целостности, индексы, триггеры,
--  представление и справочные данные (статьи учёта, агроном).
-- =====================================================================

-- CREATE DATABASE "Agro_Farm" ENCODING 'UTF8';
-- \c "Agro_Farm"

DROP VIEW  IF EXISTS v_unit_results;
DROP TABLE IF EXISTS recommendations CASCADE;
DROP TABLE IF EXISTS budget_limits   CASCADE;
DROP TABLE IF EXISTS operations      CASCADE;
DROP TABLE IF EXISTS categories      CASCADE;
DROP TABLE IF EXISTS farm_units      CASCADE;
DROP TABLE IF EXISTS agronomists     CASCADE;
DROP TABLE IF EXISTS users           CASCADE;

-- ---------------------------------------------------------------------
-- Фермеры (пользователи мобильного приложения)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id       BIGSERIAL    PRIMARY KEY,
    first_name    VARCHAR(30)  NOT NULL,
    second_name   VARCHAR(30),
    last_name     VARCHAR(30)  NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    role          VARCHAR(10)  NOT NULL DEFAULT 'user',
    CONSTRAINT chk_users_email CHECK (email ~* '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'),
    CONSTRAINT chk_users_role  CHECK (role IN ('user', 'admin'))
);

-- ---------------------------------------------------------------------
-- Агрономы-аналитики (пользователи веб-панели)
-- ---------------------------------------------------------------------
CREATE TABLE agronomists (
    agronomist_id BIGSERIAL    PRIMARY KEY,
    first_name    VARCHAR(30)  NOT NULL,
    second_name   VARCHAR(30),
    last_name     VARCHAR(30)  NOT NULL,
    work_email    VARCHAR(100) NOT NULL UNIQUE,
    phone_number  VARCHAR(11),
    login         VARCHAR(30)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_agro_phone CHECK (phone_number IS NULL OR phone_number ~ '^[0-9]{11}$')
);

-- ---------------------------------------------------------------------
-- Участки фермы: поле, животноводческий блок, теплица, склад
-- ---------------------------------------------------------------------
CREATE TABLE farm_units (
    unit_id     BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    unit_name   VARCHAR(60)   NOT NULL,
    unit_type   VARCHAR(15)   NOT NULL,
    area_ha     NUMERIC(10,2) NOT NULL DEFAULT 0,
    head_count  INTEGER       NOT NULL DEFAULT 0,
    description VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT chk_unit_type  CHECK (unit_type IN ('field', 'livestock', 'greenhouse', 'storage')),
    CONSTRAINT chk_unit_area  CHECK (area_ha >= 0),
    CONSTRAINT chk_unit_heads CHECK (head_count >= 0),
    CONSTRAINT uq_unit_name   UNIQUE (user_id, unit_name)
);

-- ---------------------------------------------------------------------
-- Статьи доходов и расходов (глобальные и пользовательские)
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    category_id BIGSERIAL   PRIMARY KEY,
    cat_name    VARCHAR(60) NOT NULL,
    cat_type    VARCHAR(10) NOT NULL,
    is_global   BOOLEAN     NOT NULL DEFAULT FALSE,
    user_id     BIGINT      REFERENCES users (user_id) ON DELETE CASCADE,
    icon        VARCHAR(30) NOT NULL DEFAULT 'other',
    CONSTRAINT chk_cat_type  CHECK (cat_type IN ('income', 'expense')),
    CONSTRAINT chk_cat_owner CHECK ((is_global AND user_id IS NULL) OR (NOT is_global AND user_id IS NOT NULL))
);

-- ---------------------------------------------------------------------
-- Хозяйственные операции (доходы и расходы по участкам)
-- ---------------------------------------------------------------------
CREATE TABLE operations (
    operation_id   BIGSERIAL     PRIMARY KEY,
    unit_id        BIGINT        NOT NULL REFERENCES farm_units (unit_id) ON DELETE CASCADE,
    category_id    BIGINT        NOT NULL REFERENCES categories (category_id) ON DELETE RESTRICT,
    op_type        VARCHAR(10)   NOT NULL,
    amount         NUMERIC(12,2) NOT NULL,
    quantity       NUMERIC(12,3),
    quantity_unit  VARCHAR(10),
    description    VARCHAR(255),
    operation_date DATE          NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT chk_op_type     CHECK (op_type IN ('income', 'expense')),
    CONSTRAINT chk_op_amount   CHECK (amount > 0),
    CONSTRAINT chk_op_quantity CHECK (quantity IS NULL OR quantity > 0),
    CONSTRAINT chk_op_qty_pair CHECK ((quantity IS NULL) = (quantity_unit IS NULL)),
    CONSTRAINT chk_op_qty_unit CHECK (quantity_unit IS NULL OR quantity_unit IN ('кг', 'ц', 'т', 'л', 'шт', 'гол'))
);

-- ---------------------------------------------------------------------
-- Лимиты расходов по статьям
-- ---------------------------------------------------------------------
CREATE TABLE budget_limits (
    limit_id    BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    category_id BIGINT        NOT NULL REFERENCES categories (category_id) ON DELETE CASCADE,
    amount      NUMERIC(12,2) NOT NULL,
    time_period VARCHAR(8)    NOT NULL,
    CONSTRAINT chk_limit_amount CHECK (amount > 0),
    CONSTRAINT chk_limit_period CHECK (time_period IN ('month', 'quarter')),
    CONSTRAINT uq_limit UNIQUE (user_id, category_id, time_period)
);

-- ---------------------------------------------------------------------
-- Рекомендации агронома (и автоматические уведомления системы)
-- ---------------------------------------------------------------------
CREATE TABLE recommendations (
    rec_id        BIGSERIAL     PRIMARY KEY,
    user_id       BIGINT        NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    agronomist_id BIGINT        REFERENCES agronomists (agronomist_id) ON DELETE SET NULL,
    topic         VARCHAR(12)   NOT NULL DEFAULT 'general',
    rec_message   VARCHAR(1000) NOT NULL,
    is_read       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_auto       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT chk_rec_topic   CHECK (topic IN ('general', 'crops', 'livestock', 'finance')),
    CONSTRAINT chk_rec_message CHECK (length(btrim(rec_message)) > 0)
);

-- ---------------------------------------------------------------------
-- Индексы
-- ---------------------------------------------------------------------
CREATE INDEX idx_units_user         ON farm_units (user_id);
CREATE INDEX idx_operations_unit    ON operations (unit_id, operation_date DESC);
CREATE INDEX idx_operations_date    ON operations (operation_date);
CREATE INDEX idx_operations_cat     ON operations (category_id);
CREATE INDEX idx_categories_user    ON categories (user_id);
CREATE INDEX idx_limits_user        ON budget_limits (user_id);
CREATE INDEX idx_recs_user          ON recommendations (user_id, created_at DESC);

-- ---------------------------------------------------------------------
-- Триггер: тип операции должен совпадать с типом статьи
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_check_operation_category() RETURNS trigger AS $$
DECLARE
    v_cat_type   VARCHAR(10);
    v_cat_owner  BIGINT;
    v_cat_global BOOLEAN;
    v_unit_owner BIGINT;
BEGIN
    SELECT cat_type, user_id, is_global INTO v_cat_type, v_cat_owner, v_cat_global
      FROM categories WHERE category_id = NEW.category_id;
    SELECT user_id INTO v_unit_owner FROM farm_units WHERE unit_id = NEW.unit_id;

    IF v_cat_type <> NEW.op_type THEN
        RAISE EXCEPTION 'Тип операции (%) не совпадает с типом статьи (%)', NEW.op_type, v_cat_type
            USING ERRCODE = 'check_violation';
    END IF;
    IF NOT v_cat_global AND v_cat_owner <> v_unit_owner THEN
        RAISE EXCEPTION 'Статья принадлежит другому пользователю'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_operations_check
    BEFORE INSERT OR UPDATE ON operations
    FOR EACH ROW EXECUTE FUNCTION fn_check_operation_category();

-- ---------------------------------------------------------------------
-- Триггер: лимиты можно задавать только на статьи расходов
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_check_limit_category() RETURNS trigger AS $$
DECLARE
    v_cat_type VARCHAR(10);
BEGIN
    SELECT cat_type INTO v_cat_type FROM categories WHERE category_id = NEW.category_id;
    IF v_cat_type <> 'expense' THEN
        RAISE EXCEPTION 'Лимит можно установить только на статью расходов'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_limits_check
    BEFORE INSERT OR UPDATE ON budget_limits
    FOR EACH ROW EXECUTE FUNCTION fn_check_limit_category();

-- ---------------------------------------------------------------------
-- Представление: финансовый результат по участкам
-- ---------------------------------------------------------------------
CREATE VIEW v_unit_results AS
SELECT u.unit_id,
       u.user_id,
       u.unit_name,
       u.unit_type,
       COALESCE(SUM(o.amount) FILTER (WHERE o.op_type = 'income'),  0) AS income,
       COALESCE(SUM(o.amount) FILTER (WHERE o.op_type = 'expense'), 0) AS expense,
       COALESCE(SUM(o.amount) FILTER (WHERE o.op_type = 'income'),  0)
     - COALESCE(SUM(o.amount) FILTER (WHERE o.op_type = 'expense'), 0) AS profit
  FROM farm_units u
  LEFT JOIN operations o ON o.unit_id = u.unit_id
 GROUP BY u.unit_id, u.user_id, u.unit_name, u.unit_type;

-- ---------------------------------------------------------------------
-- Справочные данные: глобальные статьи учёта
-- ---------------------------------------------------------------------
INSERT INTO categories (cat_name, cat_type, is_global, user_id, icon) VALUES
    ('Семена и посадочный материал', 'expense', TRUE, NULL, 'seeds'),
    ('Удобрения',                    'expense', TRUE, NULL, 'fertilizer'),
    ('Средства защиты растений',     'expense', TRUE, NULL, 'protection'),
    ('Корма',                        'expense', TRUE, NULL, 'feed'),
    ('Ветеринария',                  'expense', TRUE, NULL, 'vet'),
    ('Топливо и ГСМ',                'expense', TRUE, NULL, 'fuel'),
    ('Техника и ремонт',             'expense', TRUE, NULL, 'machinery'),
    ('Зарплата работников',          'expense', TRUE, NULL, 'salary'),
    ('Аренда земли',                 'expense', TRUE, NULL, 'rent'),
    ('Электроэнергия и вода',        'expense', TRUE, NULL, 'energy'),
    ('Прочие расходы',               'expense', TRUE, NULL, 'other'),
    ('Продажа зерна',                'income',  TRUE, NULL, 'grain'),
    ('Продажа овощей',               'income',  TRUE, NULL, 'vegetables'),
    ('Продажа молока',               'income',  TRUE, NULL, 'milk'),
    ('Продажа мяса',                 'income',  TRUE, NULL, 'meat'),
    ('Субсидии и гранты',            'income',  TRUE, NULL, 'subsidy'),
    ('Прочие доходы',                'income',  TRUE, NULL, 'other');

-- ---------------------------------------------------------------------
-- Агроном по умолчанию (логин: agronom, пароль: agro123, хэш BCrypt)
-- ---------------------------------------------------------------------
INSERT INTO agronomists (first_name, second_name, last_name, work_email, phone_number, login, password_hash)
VALUES ('Ирина', 'Петровна', 'Соколова', 'agronom@agrofarm.local', '79001234567', 'agronom',
        '$2b$12$KoLK4HOfCbGBu6qRtPFYCuYV4E4yf5uV13gRWfjC4eQabvZ5jsJdC');
