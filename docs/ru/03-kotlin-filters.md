---
id: kotlin-filters
title: Фильтр мусорных мутаций Kotlin
sidebar_label: Kotlin-фильтры
---

# Фильтр мусорных мутаций Kotlin

![Module](https://img.shields.io/badge/module-mutaktor--pitest--filter-7F52FF)
![PIT SPI](https://img.shields.io/badge/PIT-MutationInterceptor%20SPI-orange)
![Default](https://img.shields.io/badge/включён%20по%20умолчанию-true-brightgreen)
![Lang](https://img.shields.io/badge/Lang-Русский-blue)

## Обзор

Компилятор Kotlin генерирует значительное количество байткода, не имеющего прямого эквивалента в исходном коде: null-check intrinsics, реализации методов data-классов, конечные автоматы корутин, bridge-классы для реализаций интерфейсных методов по умолчанию и таблицы диспетчеризации `when`-выражений. PIT не осведомлён о Kotlin — он инструментирует скомпилированный байткод напрямую — и будет генерировать большое количество мутаций в этом сгенерированном коде.

Такие мутации почти всегда **неуничтожимы**: ни один разумно написанный тест никогда не уничтожит мутацию в `Intrinsics.checkNotNullParameter`, bridge-методе `copy$default` или таблице диспетчеризации `$Continuation.invokeSuspend`. Они раздувают общее количество мутаций, снижают сообщаемый mutation score и впустую тратят процессорное время при анализе.

`KotlinJunkFilter` — это `MutationInterceptor` PIT, который идентифицирует и отбрасывает такие мутации до того, как PIT попытается создать и запустить соответствующих мутантов.

## Как работает MutationInterceptor SPI PIT

Конвейер перехватчиков PIT выполняется между генерацией мутаций и исполнением мутантов. Перехватчики типа `FILTER` удаляют мутации из набора, который PIT будет фактически тестировать.

```kroki-mermaid
flowchart TD
    A[PIT генерирует мутации-кандидаты\nдля класса] --> B[MutationInterceptorFactory\nобнаружена через META-INF/services]
    B --> C[KotlinJunkFilterFactory\ncreateInterceptor]
    C --> D[KotlinJunkFilter.intercept\nколлекция MutationDetails\nMutater]
    D --> E{isKotlinJunk?}
    E -- да --> F[Отброшена\nне выполняется]
    E -- нет --> G[Сохранена\nPIT тестирует мутанта]
```

Фабрика обнаруживается через стандартный механизм Java `ServiceLoader`. JAR фильтра поставляется с файлом `META-INF/services/org.pitest.mutationtest.build.MutationInterceptorFactory`, указывающим на `KotlinJunkFilterFactory`.

Фабрика регистрирует функцию как `KOTLIN_JUNK` с `withOnByDefault(true)`. Это означает, что фильтр активен всегда, когда JAR находится в classpath PIT, без необходимости указывать флаг функции PIT. Чтобы отключить его на уровне PIT, используйте `--features=-KOTLIN_JUNK` (или задайте `features = listOf("-KOTLIN_JUNK")` в DSL Mutaktor).

### Интерфейс MutationInterceptor

| Метод | Описание |
|---|---|
| `type()` | Возвращает `InterceptorType.FILTER` — мутации, возвращённые `intercept`, удаляются из конвейера |
| `begin(ClassTree)` | Вызывается один раз для каждого класса до обработки мутаций; `KotlinJunkFilter` не хранит состояния для каждого класса |
| `intercept(Collection<MutationDetails>, Mutater)` | Точка входа фильтрации; возвращает только мутации, которые должны быть сохранены |
| `end()` | Вызывается после обработки всех мутаций для класса; очистка не требуется |

## 5 паттернов фильтрации

### Паттерн 1: Null-check intrinsics Kotlin

**Что фильтруется:** Мутации, описание которых ссылается на null-проверочные методы `kotlin/jvm/internal/Intrinsics`.

**Происхождение в байткоде:** Каждый non-null параметр Kotlin генерирует вызов `Intrinsics.checkNotNullParameter` в начале тела функции. Компилятор Kotlin генерирует это как обычный вызов метода в байткоде.

**Почему это мусор:** PIT может мутировать саму null-проверку (например, удалить вызов). Полученный мутант будет уничтожен только тестом, передающим `null` для non-null параметра — что является ошибкой компиляции в Kotlin. Такой тест не может существовать.

**Эвристика обнаружения:** Описание мутации содержит любое из: `Intrinsics`, `checkNotNull`, `checkParameterIsNotNull`, `checkNotNullParameter`, `checkNotNullExpressionValue`.

```kotlin
// Исходный код (Kotlin)
fun process(name: String): String {
    return name.uppercase()
}

// Эквивалент в байткоде (что видит PIT)
public static String process(String name) {
    Intrinsics.checkNotNullParameter(name, "name");  // <-- PIT мутирует этот вызов
    return name.toUpperCase();
}
```

Мутация на вызов `checkNotNullParameter` фильтруется; мутация на `toUpperCase()` сохраняется.

### Паттерн 2: Сгенерированные методы data-классов

**Что фильтруется:** Мутации внутри методов `copy`, `copy$default`, `component1` — `componentN`, `toString`, `hashCode` и `equals`.

**Происхождение в байткоде:** Объявления `data class` Kotlin автоматически генерируют эти методы на основе свойств первичного конструктора.

**Почему это мусор:** Эти методы являются чисто механическими реализациями стандартного контракта. Тестирование того, что `data class User(val name: String)` корректно копирует `name` в своём сгенерированном методе `copy()`, не имеет никакой ценности. Логика тривиально корректна по конструкции.

**Эвристика обнаружения:** Имя метода мутации соответствует регулярному выражению `^(copy|copy\$default|component\d+|toString|hashCode|equals)$`.

```kotlin
// Исходный код
data class User(val id: Long, val name: String, val email: String)

// Компилятор генерирует copy(), component1(), component2(), component3(),
// toString(), hashCode(), equals() — все мутации в этих методах фильтруются.
```

**Что остаётся тестируемым:** Любой не-сгенерированный метод, добавленный в data-класс, по-прежнему мутируется в штатном режиме.

### Паттерн 3: Диспетчеризация конечного автомата корутины

**Что фильтруется:** Мутации в методах `invokeSuspend` внутри генерируемых компилятором классов-продолжений (имена классов, содержащие `$`).

**Происхождение в байткоде:** Каждая `suspend`-функция компилируется в конечный автомат. Компилятор Kotlin создаёт анонимный внутренний класс (например, `MyService$fetchUser$1`), реализующий `Continuation<T>`. Метод `invokeSuspend` содержит таблицу диспетчеризации состояний — большой блок `when`, переключающийся по точке возобновления корутины.

**Почему это мусор:** Структура конечного автомата является деталью реализации среды выполнения корутин. Мутации в таблице диспетчеризации не могут быть уничтожены тестами бизнес-логики; для этого потребовались бы тесты, специально зондирующие внутреннее состояние корутины.

**Эвристика обнаружения:** Имя метода — `invokeSuspend`, И имя класса содержит `$`.

```kotlin
// Исходный код
suspend fun fetchUser(id: Long): User {
    val data = repository.load(id)  // точка приостановки
    return User(data)
}

// Компилятор генерирует примерно:
// class MyService$fetchUser$1 : Continuation<Any?> {
//     var label = 0
//     override fun invokeSuspend(result: Any?): Any? {
//         when (label) {          // <-- PIT видит мутации здесь; все фильтруются
//             0 -> { ... }
//             1 -> { ... }
//         }
//     }
// }
```

### Паттерн 4: Bridge-классы DefaultImpls

**Что фильтруется:** Все мутации в классах, чьё полностью квалифицированное имя заканчивается на `$DefaultImpls`.

**Происхождение в байткоде:** Когда интерфейс Kotlin объявляет метод с телом по умолчанию, компилятор Kotlin генерирует статический внутренний класс `InterfaceName$DefaultImpls`, содержащий фактическую реализацию. JVM-классы, реализующие интерфейс, делегируют вызов `DefaultImpls`, если они не переопределяют метод.

**Почему это мусор:** `$DefaultImpls` — это шим совместимости JVM. Фактическая бизнес-логика находится в объявлении интерфейса, как видно в исходном коде Kotlin. Тестирование мутаций в bridge-классе дублирует усилия и создаёт неуничтожимых мутантов.

**Эвристика обнаружения:** `mutation.className.asJavaName().endsWith("\$DefaultImpls")`.

```kotlin
// Исходный код
interface Repository {
    fun findAll(): List<String> = emptyList()  // тело по умолчанию
}

// Компилятор генерирует:
// class Repository$DefaultImpls {
//     static List findAll(Repository $this) { return CollectionsKt.emptyList(); }
// }
// Все мутации внутри Repository$DefaultImpls фильтруются.
```

### Паттерн 5: Диспетчеризация when-выражения через hashCode

**Что фильтруется:** Мутации, описание которых одновременно упоминает `hashCode` и `equals`.

**Происхождение в байткоде:** `when`-выражения Kotlin, переключающиеся на значениях `String`, компилируются с использованием `hashCode()` для выбора корзины хеш-таблицы, за которым следует `equals()` для однозначного определения (идентично тому, как Java switch-on-String работает на уровне байткода).

**Почему это мусор:** Хеш-диспетчеризация — это паттерн, генерируемый компилятором. Мутация, инвертирующая проверку `equals` в диспетчеризации switch — в отличие от фактической бизнес-логики — фактически неуничтожима обычными тестами, поскольку само вычисление хеша не является объектом тестирования.

**Эвристика обнаружения:** Описание мутации содержит одновременно `hashCode` и `equals`.

```kotlin
// Исходный код
fun describe(status: String): String = when (status) {
    "active"   -> "User is active"
    "inactive" -> "User is inactive"
    else       -> "Unknown status"
}

// Скомпилированный байткод содержит диспетчеризацию hashCode()+equals().
// Мутации в логике диспетчеризации фильтруются; мутации в строковых
// литералах или путях возврата сохраняются.
```

## Сводная таблица фильтров

| # | Паттерн | Предикат | Причина |
|---|---|---|---|
| 1 | Null-check intrinsics | `description` содержит `Intrinsics` / `checkNotNull*` | `Intrinsics.checkNotNullParameter` для каждого non-null параметра |
| 2 | Сгенерированные методы data-классов | `method` соответствует `copy`, `componentN`, `toString`, `hashCode`, `equals` | Генерируемые компилятором реализации `data class` |
| 3 | Конечный автомат корутины | `method == "invokeSuspend"` И имя класса содержит `$` | `suspend`-функция, скомпилированная в конечный автомат-продолжение |
| 4 | Bridge DefaultImpls | Имя класса заканчивается на `$DefaultImpls` | Bridge-совместимость JVM для методов интерфейса по умолчанию |
| 5 | Hashcode-диспетчеризация when | `description` содержит одновременно `hashCode` и `equals` | `when(String)` скомпилирован в хеш-корзину + проверку равенства |

## Примеры отфильтрованных и сохранённых мутаций

### Пример: Null-проверка

| Мутация | Отфильтрована? | Причина |
|---|---|---|
| `removed call to kotlin/jvm/internal/Intrinsics.checkNotNullParameter` | Да | Паттерн 1 — неуничтожимый null-check intrinsic |
| `replaced return value with null` в `fun process(name: String): String` | Нет | Нормальная мутация в пользовательском коде |

### Пример: Data-класс

| Мутация | Отфильтрована? | Причина |
|---|---|---|
| `negated conditional in hashCode()` в `data class User` | Да | Паттерн 2 — метод, сгенерированный компилятором |
| `negated conditional in validate()` в `data class User` | Нет | Написанный пользователем метод, нормальная мутация |

### Пример: Корутины

| Мутация | Отфильтрована? | Причина |
|---|---|---|
| `changed conditional boundary in MyService$fetchUser$1.invokeSuspend` | Да | Паттерн 3 — конечный автомат корутины |
| `changed conditional boundary in MyService.fetchUser` | Нет | Бизнес-логика в самой функции |

## Отключение фильтра

Чтобы отключить фильтр глобально, задайте `kotlinFilters = false` в DSL Mutaktor. При этом JAR фильтра полностью удаляется из classpath PIT.

```kotlin
mutaktor {
    kotlinFilters = false
}
```

Чтобы отключить фильтр на уровне функции PIT, сохранив JAR в classpath (например, для использования других функций модуля фильтра в будущем):

```kotlin
mutaktor {
    features = listOf("-KOTLIN_JUNK")
}
```

## См. также

- [Архитектура плагина](./01-architecture.md)
- [Справочник по конфигурационному DSL](./02-configuration.md)
- [Документация PIT MutationInterceptor](https://pitest.org/javadoc/)
