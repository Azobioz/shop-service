INSERT INTO products (name, description, price, stock, created_at, updated_at) VALUES
       ('Синяя машина', 'Для езды', 100000.99, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Фонарь А', 'Фонарь хорошего качества', 5000.0, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Плюшевый медведь Миша', 'Есть только красного цвета', 501.11, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Ораньжевая мышка"', 'Песпроводная', 4000.12, 81, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Металический стакан', 'В бонус чайные пакеты', 5000.99, 48, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Вентилятор', 'Гарантия 2 года', 14000.99, 91, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;