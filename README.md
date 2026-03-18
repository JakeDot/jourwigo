# jourwigo
Your Java Urwigo!

## Build

- Maven: `mvn test` / `mvn package`
- Gradle: `./gradlew test` / `./gradlew build`

## Desktop GUI

- `java -jar jourwigo-<version>-jar-with-dependencies.jar --gui` launches a JavaFX desktop UI.
- **Run Cartridge** tab opens and runs `.gwc` cartridges with editable simulated GPS coordinates.
- **Create Cartridge** tab creates a Lua cartridge template (`.lua`) that can be compiled into `.gwc`.
- **Web Integration** tab starts the built-in web editor shell and loads it directly in JavaFX via WebView.

## Integrated web editor shell

- `java -jar jourwigo-<version>-jar-with-dependencies.jar --web [port]` starts a lightweight web interface.
- The integrated approach uses cgeo/cgeo Wherigo runtime classes for robust cartridge execution and Urwigo-style template generation for editor workflows.

## Inspiration and code sources

- Inspired by Urwigo.
- Code sources include cgeo, openwig, and kahlua.
