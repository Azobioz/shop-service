
# shop-service

Этот репозиторий был создан с целью, чтобы заркыть предмет "Кроссплатформенная разработка на языке Kotlin" на 100 балов

## Что можно сделать с этим сервисом

### Если обычный пользователь:
- Регистрироваться и логиниться
- Создать заказ
- Смотреть историю покупок
- Просмотривать каталог товаров
- Отменять заказы


### Если администратор:
- Управлятьтоварами (по CRUD)
- Получать доступ ко всем функциям API
- Получать статистики по заказам


## Что я использовал при разработке этого сайта

- **Kotlin**
- **Ktor**
- **PostgreSQL**
- **JWT**
- **JUnit & MockK**
- **Exposed ORM**
- **Redis**
- **Apache Kafka**
- **Docker & Docker Compose**




## Структура проекта

```
src/
├── main/
│   ├── kotlin/com/example/
│   │   ├── config/         # Конфигурация приложения
│   │   ├── data/
│   │   │   ├── repository/ # Слой работы с данными
│   │   │   └── table/     # Схемы таблиц БД
|   |   |── kafka           # Продюсер и консюмер
│   │   ├── module/         # Сущности
│   │   ├── routes/         # Маршруты
│   │   ├── service/        # Бизнес-логика
│   │   └── Application.kt  # Точка входа
│   └── resources/
│       ├── application.conf
│       ├── logback.xml
│       ├── db/
│       │   └── migration/  # Flyway миграции
│       └── openapi/        # Swagger документация
└── test/
    ├── e2e/                # E2E тесты
    ├── integration/        # Интеграционные тесты
    └── unit/            # Unit тесты
```

## Инструкция

### Что нужно для запуска сервиса

- JDK 21
- Docker и Docker Compose
- Gradle или использовать wrapper

### Локальный запуск с Docker Compose

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd shop-service
```

2. Запустите все сервисы:
```bash
docker compose up -d
```

Что это даст:
- PostgreSQL (порт 5433) 
- Redis (порт 6379)
- Kafka + Zookeeper (порт 9092)
- Приложение (порт 8080)

3. После этого сервис будет доступен по адресу: `http://localhost:8080`


### Администратор

При первом запуске сервиса создаётся администратор с такими данными:
- **Email:** `admin@shop.com`
- **Пароль:** `admin123`


### Для разработки запустите локально

1. Инфраструктуру:
```bash
docker compose up -d postgres redis zookeeper kafka
```

2. Сервис:
```bash
./gradlew run
```

## API Документация

Swagger UI доступен по адресу: `http://localhost:8080/swagger`

### Эндпоинты

#### Аутентификация
- `POST /auth/register` - Регистрация
- `POST /auth/login` - Вход

#### Заказы (для этого требуется авторизация)
- `POST /orders` - Создать 
- `GET /orders` - История 
- `DELETE /orders/{id}` - Отменить 

#### Товары
- `GET /products` - Список товаров
- `GET /products/{id}` - Конкретный товар

#### Админ (требуется иметь роль ADMIN)
- `POST /admin/products` - Добавить 
- `PUT /admin/products/{id}` - Обновить 
- `DELETE /admin/products/{id}` - Удалить 
- `GET /admin/stats/orders` - Статистика

## Тестирование

Проект состоит из таких тестов:
- **E2E тесты** - тестирование API эндпоинтов
- **Unit тесты** - тестирование сервисов и бизнес-логики
- **Integration тесты** - тестирование репозиториев с TestContainers

Для запуска всех тестов нужно прописать:
```bash
./gradlew test
```


## Функционал сервиса

### Асинхронность
При создании/отмене заказа событие отправляется в Kafka. Consumer обрабатывает события и:
- Логирует действия
- Отправляет уведомления (заглушка)

### Кэширование
Товары кэшируются в Redis с TTL 5 минут. Кэш инвалидируется при обновлении/удалении товара.

### База данных и миграции
Проект использует Flyway для управления схемой базы данных:
- **V1__Initial_schema.sql** - создает таблицы, индексы и внешние ключи
- **V2__data.sql** - начальные данные 
- Все изменения схемы версионируются и воспроизводимы
- Миграции автоматически применяются при запуске приложения

### Аудит
Все важные действия (создание/отмена заказов) логируются в таблицу `audit_logs`.

### Безопасность
- Есть роли пользователей (USER и ADMIN)
- JWT токены для аутентификации
- Защита админских эндпоинтов

## Конфигурация

Настройки находятся в `src/main/resources/application.conf`:

```hocon
ktor {
    deployment {
        port = 8080
        port = ${?PORT}
    }
    application {
        modules = [ com.example.ApplicationKt.module ]
    }
}

jwt {
    secret = "my-secret-key"
    issuer = "http://localhost:8080"
    audience = "http://localhost:8080/api"
    realm = "Access to API"
}

database {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5433/shop_db"
    user = "postgres"
    password = "postgres"
    maxPoolSize = 10
}

redis {
    host = "localhost"
    port = 6379
}

kafka {
    bootstrapServers = "localhost:9092"
    topic = "order-events"
}

admin {
    email = "admin@shop.com"
    password = "admin123"
}
```

## Про разработку

### Миграции базы данных

Для создания новой миграции нужно:

1. Создать файл `src/main/resources/db/migration/V{N}__{Description}.sql`
2. Написать SQL для изменения схемы
3. Перезапустить сервис - миграция применится автоматически

Например:
```sql
-- V3__Add_user_phone.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

## CI/CD

GitHub Actions workflow:
- Собирает проект с кэшированием зависимостей
- Запускуает все тесты (Unit + Integration + E2E)
- Создает Docker образ
- Публикует HTML отчеты тестов

### Для просмотра результатов тестов

После каждого push в `main` или `dev`, происходит это:

1. **GitHub Actions** → выбирает нужный workflow run
2. **Artifacts** → скачивает `test-report-html.zip`
3. Распаковывает и открывает `index.html` в браузере



### Про просмотр логов

```bash
docker compose logs -f app
```

### Для остановки сервисов

```bash
docker compose down
```
