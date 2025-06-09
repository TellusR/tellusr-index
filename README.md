# SiSearchIndex

A lightweight Kotlin wrapper around Apache Lucene that makes it easy to define,
index and search domain models without writing Lucene-specific code.

This library is particularly useful when you need a full-text search capable datastore in your application, but want to
avoid the complexity and overhead of setting up and maintaining an external database system. By using embedded Lucene
through SiSearchIndex, you get powerful search capabilities while keeping your data local and your deployment simple.



> The library works with **Java 21** and **Kotlin 2.1**.

---

## Contents

1. [Main Concept](#main-concept)
2. [Installation](#installation)
3. [Core Concepts](#core-concepts)
4. [Getting Started](#getting-started)
5. [CRUD Operations](#crud-operations)
6. [Search Examples](#search-examples)
7. [Advanced Topics](#advanced-topics)
8. [Error Handling](#error-handling)
9. [Contributing](#contributing)
10. [License](#license)

---

## Main Concept

SiSearchIndex lets you describe a **Record** (row) through:

* `Fields` - enum describing the columns and their field types
* `Schema` - describes which group/path the index belongs to
* `Record` - data class holding the values
* `SearchIndex` - the Lucene index itself with a clear API

Everything is usually declared as inner types in an interface to give you a
self-documenting structure.

---

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/tellusr/framework")
        credentials {
            username = project.findProperty("gpr.user")?.toString() ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key")?.toString() ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("com.tellusr:tellusr-searchindex:0.9.1")
}
```

---

## Core Concepts

| Concept         | Description                                                                             |
|-----------------|-----------------------------------------------------------------------------------------|
| `SiField`       | Interface that an enum field implements. Defines primary name and type (`SiFieldType`). |
| `SiFieldType`   | Marker enum indicating semantics: `UniqueId`, `Keyword`, `ForeignKey`, `Auto` etc.      |
| `SiSchema`      | Connects `Fields` to a directory structure, holds analyzers and Doc/JSON conversion.    |
| `SiRecord`      | Row/Entity. Has `getValue`/`setValue` for reflection-free access from index handler.    |
| `SiSearchIndex` | High-level API for writing/updating/deleting records and running searches.              |

---

## Getting Started

Below we build a mini dataset for fine-tuning examples (abbreviated from
`DataBrickStore` in production):

```kotlin
package com.example.store

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import com.tellusr.searchindex.*
import org.apache.lucene.document.Document

interface TestStore {

    // 1. Define the fields
    enum class Fields : SiField {
        Id, Instruction, Context, Response, Category, Language, SourceId, MetaData, Vectorized;


        override fun primary(): String = name
        override fun fieldType(): Set<SiFieldType> = when (this) {
            Id -> setOf(SiFieldType.UniqueId)
            Category -> setOf(SiFieldType.Keyword)
            SourceId -> setOf(SiFieldType.ForeignKey)
            Vectorized -> setOf(SiFieldType.Vector)
            else -> setOf(SiFieldType.Auto)
        }
    }

    // 2. Schema - points to directory and converts between Document/JSON
    object Schema : SiSchema("tuning", Fields.entries) {
        override fun fromDoc(doc: Document): SiRecord = Record(doc)
        override fun fromJson(jsonObject: JsonObject): Record = json.decodeFromJsonElement<Record>(jsonObject)
    }

    // 3. Record - your domain model
    @Serializable
    class Record(
        var id: String,
        var instruction: String,
        var context: String?,
        var response: String?,
        var category: String?,
        var language: String?,
        var sourceId: String? = null,
        var json: JsonElement? = null,
        var vectorized: FloatArray? = null,
    ) : SiRecord {
        constructor(doc: Document) : this(
            id = doc.get(Fields.Id.primary()),
            instruction = doc.get(Fields.Instruction.primary()),
            context = doc.get(Fields.Context.primary()),
            response = doc.get(Fields.Response.primary()),
            category = doc.get(Fields.Category.primary()),
            language = doc.get(Fields.Language.primary()),
            sourceId = doc.get(Fields.SourceId.primary()),
            json = doc.get(Fields.MetaData.primary())?.let {
                SiRecord.jsonEncoder.parseToJsonElement(it)
            },
            vectorized = SiRecord.fromVectorString(doc.get(Fields.Vectorized.primary()))
        )

        override fun schema(): SiSchema = Schema

        override fun getValue(key: SiField): Any? = when (key as? Fields) {
            Fields.Id -> id
            Fields.Instruction -> instruction
            Fields.Context -> context
            Fields.Response -> response
            Fields.Category -> category
            Fields.Language -> language
            Fields.SourceId -> sourceId
            Fields.MetaData -> json
            Fields.Vectorized -> vectorized
            null -> null
        }

        override fun setValue(key: SiField, value: Any?) {
            when (key as? Fields) {
                Fields.Id -> (value as? String)?.let { id = it }
                Fields.Instruction -> (value as? String)?.let { instruction = it }
                Fields.Context -> context = (value as? String)
                Fields.Response -> response = (value as? String)
                Fields.Category -> category = (value as? String)
                Fields.Language -> language = (value as? String)
                Fields.SourceId -> sourceId = (value as? String)
                Fields.MetaData -> json = when (value) {
                    is JsonElement -> value
                    is String -> SiRecord.jsonEncoder.parseToJsonElement(value)
                    else -> null
                }
                Fields.Vectorized -> vectorized = value as FloatArray
                null -> {}
            }
        }
    }

    
    // 4. Finally - the index
    class SearchIndex(name: String) : SiSearchIndex<Record>(Schema, name)
}
```

Create an index and insert data:

```kotlin
val index = DataBrickStore.SearchIndex("default")

index.add(
    DataBrickStore.Record(
        id = "1",
        instruction = "Oversett til norsk",
        context = "Hello, world!",
        response = "Hei, verden!",
        category = "translation",
        language = "nb"
    )
)
```
---

## CRUD-operasjoner
```kotlin
// Create / Replace
index.add(record)              // auto-commit
index.add(listOf(r1, r2, r3))  // bulk

// Read 
val doc = index.getById("1")           // gets one
val list = index.getAll()              // gets all

// Update
index.update("1") { rec ->
    rec.response = "Hello, world!"
}

// Delete
index.remove("1")
index.clear()                   // clears the entire index
---

## Søkeeksempler
```kotlin
// Full-text search
val hits = index.search("instruction:translate AND response:Hello*")

// Free text with standard analyzer
val english = index.search("language:en")

// Pagination
val page2 = index.search("category:translation", page = 2, pageSize = 10)

`search()` returns a `List<Record>` and scores if you request them.

---

## Advanced Topics

| Topic            | Why                                                                      |
|------------------|--------------------------------------------------------------------------|
| Custom analyzer  | Override `Schema.defaultAnalyzer()` or `analyzer()`                      |
| Custom Directory | Pass in your own `createDirectory` lambda to `SiSearchIndex` constructor |
| Constraints      | Add `SiConstraint` to fields for validation before writing               |
| Thread safety    | All write operations are held behind a `Mutex`; searches are lock-free.  |

---

## Error Handling

All serious conditions throw `LuceneStoreException.*`.
The most common are:

* `SearchIndexError` - same index created twice
* `ConstraintViolation` - you broke a field rule (e.g. unique ID violation)

---

## Contributing

1. Fork the repo
2. Create branch `feature/<name>`
3. Write tests (`./gradlew test`)
4. Submit Pull Request

All suggestions are welcome!

---

## License

This project is licensed under the **Apache License 2.0**. See the `LICENSE` file
for details.


