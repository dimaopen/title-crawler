# Простой интернет crawler

Предоставляет endpoint для получения названий сайтов.

## Как запускать

```shell
sbt run
```

Сервер запустится на порту 8880.

## Endpoint

Сервер предоставляет один POST endpoint по пути /api/v1/tasks, который принимает JSON массив, содержащий URLы.

```
POST http://localhost:8880/api/v1/tasks
Content-Type: application/json
Accept: application/json

[
  "https://www.google.com",
  "https://www.yandex.com",
  "https://www.bing.com"
]
```

В ответ приходит массив JSON объектов, содержащий в себе заданный URL (поле `uri`) и название сайта (поле `title`).
```JSON
{
  "uri": "https://www.google.com",
  "title": "Google"
}
```
Если сервер не смог по какой-то причине извлечь название сайта, для данного URL возвращается ошибка
```json
 {
    "error": {
      "error_code": "CONNECTION_ERROR",
      "error_message": "Connection refused"
    },
    "uri": "https://unknown.com/some.html"
  }
```
## Описание работы краулера
Запросы к сайтам выполняются параллельно, при этом ответ парсится с помощью SAX-совместимой библиотеки [tasoup](https://mvnrepository.com/artifact/org.ccil.cowan.tagsoup). Это позволяет не загружать весь ответ в память, а искать
тег title в потоке.

## Что можно еще сделать
1. Тесты.
2. Добавить конфигурацию.
3. Имплементировать [паттерн для долго выполняющихся задач](https://restfulapi.net/rest-api-design-for-long-running-tasks/).