json-serialization
==================


This library provides simple and effective method of parsing JSON into Java Maps and Lists.
You can use the `JsonParser` class to parse a json string into either `List<Object>` or `Map<String, Object>`.
The `JsonSerialization` class can then serialize a `List<Object>` or `Map<String, Object>` into a json string.

Note that this library operates on 5 basic types:
- Numbers are represented by either `Integer`, `Double`, or `Long` by the `JsonParser` class,
  though `JsonSerialization` will accept any class that implements `Number`.
- Strings are represented by the java `String` class.
- JSON objects are represented by the `Map<String, Object>` class in `JsonParser`,
  though `JsonSerialization` will accept any `Map<?, ?>`, providing the map values are valid types
- JSON arrays are represented by the `List<Object>` class in `JsonParser`,
  though `JsonSerialization` will accept any `Iterable<?>` class, providing the provided values are valid types.
- `null` is used to represent literal `null` values in JSON objects/arrays. The `JsonParser` class will produce Lists
  and Maps which contain null values if the json string has an unquoted `null` value. The `JsonSerialization` class will
  accept null values, and they will translate to literal unquoted `null` values in the produced string.

`JsonParser` and `JsonSerialization` will only produce/accept those 5 types of values.
No other type may be used with this library.

json-serialization is built to target Java 1.6 or greater.

json-serialization is based on the JSON-java project, though it has been thoroughly rebuilt into a simpler and lighter library.
