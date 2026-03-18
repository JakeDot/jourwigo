# jourwigo
Your Java Urwigo!

## Desktop GUI

- `java -jar jourwigo-<version>-jar-with-dependencies.jar --gui` launches a JavaFX desktop UI.
- **Run Cartridge** tab opens and runs `.gwc` cartridges with editable simulated GPS coordinates.
- **Create Cartridge** tab creates a Lua cartridge template (`.lua`) that can be compiled into `.gwc` using Urwigo-compatible tooling.

## Integrated web editor shell

- `java -jar jourwigo-<version>-jar-with-dependencies.jar --web [port]` starts a lightweight web interface.
- The integrated approach uses c:geo/c:geo Wherigo runtime classes for robust cartridge execution and Urwigo-style template generation for editor workflows.
