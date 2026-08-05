# SpotlessFormatPlugin

SpotlessFormatPlugin is an IntelliJ IDEA plugin that integrates [Spotless](https://github.com/diffplug/spotless) logic to format files using Eclipse Formatter XML and Import Order files. It provides an automated on-save action to keep your codebase consistent with your formatting rules.

## Overview

This plugin bridges the gap between Spotless's powerful formatting capabilities and the IntelliJ IDEA environment. It allows developers to enforce consistent formatting across their team by utilizing shared Eclipse XML formatter configurations and Java import order files, or by using a generic Spotless configuration.

## Features

- **Flexible Configuration**: Use either specific Eclipse Formatter files or a generic Spotless configuration file.
- **Eclipse Formatter Integration**: Use your existing Eclipse XML formatter configuration via Spotless logic.
- **Import Ordering**: Apply custom import order rules (Java support).
- **Format on Save**: Automatically format changed files when saving.
- **Project-specific Configuration**: Settings are stored per project.
- **Customizable Extensions**: Configure which file types should be formatted (e.g., `java,xml`).

## Requirements

- **IntelliJ IDEA**: 2025.3.5 or newer.
- **JDK**: 17 or 21 (as required by the specified IntelliJ version).
- **Operating System**: macOS, Windows, or Linux.

## Setup & Installation

### Using Pre-built Plugin
1. Download the plugin distribution (ZIP).
2. In IntelliJ IDEA, go to `Settings` > `Plugins`.
3. Click the gear icon and select `Install Plugin from Disk...`.
4. Select the ZIP file and restart the IDE.

### From Source
1. Clone the repository:
   ```bash
   git clone https://github.com/kroeppelt/SpotlessFormatPlugin.git
   ```
2. Open the project in IntelliJ IDEA.
3. Gradle will sync automatically.

## Configuration

Navigate to **Settings** > **Other Settings** > **Spotless Formatter** (or search for "Spotless Formatter" in settings).

### Legacy Mode (Eclipse Formatter)
- **Formatter XML Path**: Absolute path to your Eclipse formatter XML file.
- **Import Order Path**: Absolute path to your `.importorder` file (Java only).

### Generic Spotless Mode
- **Use generic Spotless configuration file**: Enable this to use a custom Spotless configuration.
- **Spotless Config**: Path to your `spotless.gradle`, `spotless.xml` or other supported configuration file.

### General Settings
- **Supported Extensions**: Comma-separated list of file extensions that should be formatted (e.g., `java,xml`).
- **Execute on Save**: Enable this to trigger formatting automatically when files are saved.

## Development & Scripts

This project uses **Kotlin** and **Gradle (Kotlin DSL)** with the [IntelliJ Platform Gradle Plugin](https://github.com/JetBrains/intellij-platform-gradle-plugin).

### Useful Gradle Tasks

- `./gradlew runIde`: Runs a development instance of IntelliJ IDEA with the plugin installed.
- `./gradlew test`: Executes unit tests.
- `./gradlew verifyPlugin`: Validates the plugin configuration and checks for compatibility.
- `./gradlew buildPlugin`: Assembles the plugin distribution ZIP (found in `build/distributions`).
- `./gradlew publishPlugin`: Uploads the plugin to JetBrains Marketplace (requires `JETBRAINS_TOKEN`).

### Predefined Run Configurations

The `.run` directory contains predefined configurations for IntelliJ IDEA:
- **Run Plugin**: Executes `:runIde`.
- **Run Tests**: Executes `:test`.
- **Run Verifications**: Executes `:verifyPlugin`.

## Project Structure

```text
.
├── .run/                   # Predefined Run/Debug Configurations
├── gradle/
│   ├── wrapper/            # Gradle Wrapper
│   └── libs.versions.toml  # Version catalog
├── src/
│   ├── main/
│   │   ├── kotlin/         # Plugin source code (Kotlin)
│   │   │   └── de/spotlessformatplugin/
│   │   │       ├── listeners/   # Entry point: SpotlessSaveListener (on-save)
│   │   │       ├── services/    # Business logic (SpotlessRunner)
│   │   │       └── settings/    # Entry point: SpotlessFormatConfigurable (Settings UI)
│   │   └── resources/
│   │       └── META-INF/   # Plugin descriptors (plugin.xml) and icons
│   ├── test/
│   │   └── kotlin/         # Unit tests (JUnit 5 and JUnit 4)
├── build.gradle.kts        # Main build configuration (Gradle Kotlin DSL)
├── settings.gradle.kts      # Gradle settings
├── gradle.properties       # Gradle properties
├── CHANGELOG.md            # Project changelog
├── LICENSE                 # License file
└── README.md               # This file
```

## Environment Variables

- `JETBRAINS_TOKEN`: Required for the `publishPlugin` task to authenticate with JetBrains Marketplace.

## Testing

Tests are located in `src/test/kotlin`. The project uses **JUnit 5** with support for **JUnit 4**. Run them using:

```bash
./gradlew test
```

## TODOs / Unknowns

- [ ] Add support for more Spotless formatters (e.g., Prettier, Google Java Format).
- [ ] Implement a progress indicator during formatting if it takes significant time.
- [ ] Add more comprehensive integration tests using the IntelliJ Platform test framework.
- [ ] TODO: Investigate behavior with large monorepos and multiple configuration files.
- [ ] TODO: Determine compatibility with older IntelliJ IDEA versions (pre-2025.3.5).

## License

Copyright (c) 2026 kroeppelt. Distributed under the Apache 2.0 License. See `LICENSE` for details.

---

[docs]: https://plugins.jetbrains.com/docs/intellij
[jb:forum]: https://platform.jetbrains.com/
[gh:intellij-platform-gradle-plugin]: https://github.com/JetBrains/intellij-platform-gradle-plugin
