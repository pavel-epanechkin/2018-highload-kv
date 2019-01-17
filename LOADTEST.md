## Результаты нагрузочного тестирования реализации второго этапа:

##### PUT 3/3
https://overload.yandex.net/148602

##### GET 3/3
https://overload.yandex.net/149648

##### PUT + GET 3/3
https://overload.yandex.net/149650

## Результаты нагрузочного тестирования после оптимизаций:

##### PUT 3/3
https://overload.yandex.net/149653

##### GET 3/3
https://overload.yandex.net/149964

##### PUT + GET 3 3
https://overload.yandex.net/149967

## Профилирование и оптимизация:

Для профилирования были использованы **jvisualvm** (так как изначально пытался нагружать хранилище на Windows,
но результаты оптимизаций были не заметны из-за низкого rps), а также **async-profiler** (затем перешел c Windows на Ubuntu).

Основные оптимизации:

- Кеширование записей при чтении и записи (направлено на уменьшение задержек при GET-запросах);
- Исполнение одного из запросов репликации в потоке обработчика, вместо его исполнения в отдельном потоке и перевода основного потока в состояние ожидания результатов;
- Параллельная обработка результатов запросов репликации.

**Результаты оптимизации:**

- Пропускную способность хранилища для PUT-запросов удалось увеличить в среднем на 9%. Данный результат можно считать вполне приемлемым, так как если посмотреть на [результаты профилирования для PUT после оптимизации](./results/put_after.svg),
можно увидеть, что почти все процессорное время уходит на выполнение методов one-nio и встраиваемой бд nitrite, что говорит о сильной ограниченности пространства для дальнейших оптимизаций обработки PUT-запросов.
- Пропускная способность хранилища для GET-запросов была увеличена в среднем на 16,5%.
- Пропускная способность хранилища для смеси PUT- и GET-запросов была увеличена в среднем на 11,1%.

