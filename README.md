CrashFX
-------

CrashFX is a small, simple library and web app to gather and analyse crash reports from JavaFX applications. It provides:

- A small client library that can will display a crash alert dialog with optional viewing of the stack trace, and which
  saves crash logs to disk for later background upload.
- A small web app that collects crash reports, inserts them into a database (any supported by Hibernate will do) and
  can draw a dashboard showing recent exceptions and a pie chart of which types are most common.

CrashFX is Apache 2.0 licensed. You can use it in commercial apps without restriction. If you would like commercial
support or to use the vinumeris.com crash collector and dashboard, please [contact us](mailto:contact@vinumeris.com).

Maven coordinates
-----------------

| Group ID            | Artifact ID    | Version |
| :-----------------: | :------------: | :-----: |
| com.vinumeris       | crashfx-client | 1.0     |

How to integrate
----------------

Near the start of your application, call

```
CrashFX.setup("MyApp v1.0", Paths.get("path/to/working/dir"), URI.create("https://www.example.com/path/to/crashfx/upload");
```



