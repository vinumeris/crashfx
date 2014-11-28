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


How to run the web app
----------------------

The web app is based on the [Ninja Framework](http://www.ninjaframework.org), and written in Kotlin. Despite that it
is all Maven based and has a standalone mode. It is set up for a PostgreSQL backend but the persistence.xml file could
 be tweaked to use any other database backend supported by Hiberate.

Grab the code, then put your database connection details into the `web/src/main/java/conf/application.conf` file. You
 should then run `mvn ninja:run` from the web directory which will boot up the app in standalone mode, and generate
 a random application secret for you in the application.conf file as well. This is used for cookie signing so please
 don't screw this step up!

Finally you can run `mvn package` to get a WAR in the target directory that can be deployed to any Java app server.
Or, just use standalone mode.


