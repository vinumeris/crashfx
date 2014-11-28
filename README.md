CrashFX
-------

CrashFX is a small, simple library and web app to gather and analyse crash reports from JavaFX applications. It provides:

- A small client library that can will display a crash alert dialog with optional viewing of the stack trace, and which
  saves crash logs to disk for later background upload.
- A small web app that collects crash reports, inserts them into a database (any supported by Hibernate will do) and
  can draw a dashboard showing recent exceptions and a pie chart of which types are most common.

