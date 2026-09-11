![Version](https://img.shields.io/jetbrains/plugin/v/:34092)
![Build](https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/gradle.yml/badge.svg)
![Build](https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/qodana_code_quality.yml/badge.svg)

# SpotlessFormatPlugin

## Overview

**SpotlessFormatPlugin** brings Spotless formatting capabilities directly into the IntelliJ IDEA environment. It enables developers and teams to enforce consistent code styles across their projects using **Eclipse XML formatters**, **Google Java Format**, **Prettier**, or a generic **Spotless configuration** (e.g., `spotless.gradle`, `.importorder`).

## Features

- **Multiple Formatter Engines**:
  - **Eclipse Formatter**: Apply shared Eclipse XML formatter profiles and `.importorder` rules for Java and XML files.
  - **Google Java Format**: Format Java source code seamlessly with Google Java Format (configurable version).
  - **Prettier**: Format JavaScript, TypeScript, JSON, and other web/data files using local or global Prettier installations (supporting project `node_modules`, `npx`, NVM, Volta, asdf, nodenv, and Homebrew).
  - **Generic Spotless Configuration**: Utilize generic Spotless configuration files (e.g., `spotless.gradle`, `.order`, `.importorder`) with automatic whitespace trimming, line-ending standardization, and import reordering.
- **Hierarchical Config Search**: Automatically discovers configuration files by searching upwards from the file being formatted through its parent directories.
- **Format on Save**: Automatically reformat modified files upon document saving.
- **Configurable Supported Extensions**: Easily define which file extensions trigger formatting (default: `java,xml,js,ts,json`).
- **Project-Specific Settings**: Settings are saved per project (`spotless-format-settings.xml`).
- **Validation & Error Reporting**: Real-time validation in the settings UI and informative notifications if configuration files or executables are missing.

## Requirements

- **IntelliJ IDEA**: 2025.3 or newer (tested up to 2026.2+).
- **JDK**: 17 or 21+ (as required by the specified IntelliJ IDEA version).
- **Node.js & Prettier** *(Optional)*: Required only when using the Prettier formatter engine.
- **Operating System**: macOS, Windows, or Linux.

## Setup & Installation

### Using Pre-built Plugin
1. Download the plugin distribution (`.zip`).
2. In IntelliJ IDEA, open **Settings** (`Cmd+,` on macOS / `Ctrl+Alt+S` on Windows & Linux) > **Plugins**.
3. Click the gear icon (⚙️) and select **Install Plugin from Disk...**.
4. Select the downloaded ZIP file and restart the IDE.

### From Source
1. Clone the repository:
   ```bash
   git clone https://github.com/kroeppelt/SpotlessFormatPlugin.git
   ```
2. Open the project in IntelliJ IDEA.
3. Gradle will sync automatically and download dependencies.

## Configuration

Navigate to **Settings** > **Spotless Formatter** (or search for *"Spotless Formatter"* in settings).

### 1. Generic Spotless Mode
- **Use generic Spotless configuration file**: Enable this checkbox to use a custom Spotless configuration instead of individual formatter engines.
- **Spotless Config**: Relative or absolute path to your configuration file (e.g., `spotless.gradle`, `spotless.xml`, `.importorder`).
  - **Hierarchical Search**: When a relative path is specified, the plugin searches starting from the folder containing the file being formatted and traverses upwards through parent directories.

### 2. Dedicated Formatter Engines
When generic mode is disabled, select one of the following engines:

#### Eclipse Formatter
- **Formatter XML**: Path to your Eclipse formatter XML configuration file.
- **Import Order File**: Path to your Java `.importorder` file.

#### Prettier
- **Prettier Config**: Optional path to a `.prettierrc` or `prettier.config.js` configuration file.
- Automatically detects Prettier from:
  - Local project `node_modules/.bin/prettier`
  - System `PATH`
  - Node version managers (`~/.nvm`, `~/.volta`, `~/.asdf`, `~/.nodenv`) and Homebrew paths
  - `npx prettier` fallback

#### Google Java Format
- **Version**: Google Java Format version (e.g., `1.17.0`). Applies to `.java` files.

### 3. General Settings
- **Supported Extensions**: Comma-separated list of file extensions to format (e.g., `java,xml,js,ts,json`).
- **Execute Spotless on save for changed files**: When checked, formats supported files automatically when saving.

## Development & Scripts

This project is built using **Kotlin** and **Gradle (Kotlin DSL)** with the [IntelliJ Platform Gradle Plugin](https://github.com/JetBrains/intellij-platform-gradle-plugin).

### Useful Gradle Tasks

- `./gradlew runIde`: Launches a development instance of IntelliJ IDEA with the plugin active.
- `./gradlew test`: Executes all unit and integration tests.
- `./gradlew verifyPlugin`: Validates plugin descriptor, dependencies, and binary compatibility.
- `./gradlew buildPlugin`: Builds the distribution ZIP artifact (located under `build/distributions`).
- `./gradlew publishPlugin`: Publishes the plugin to JetBrains Marketplace (requires `JETBRAINS_TOKEN`).

### Predefined Run Configurations

Preconfigured run targets in `.run`:
- **Run Plugin**: Executes `:runIde`.
- **Run Tests**: Executes `:test`.
- **Run Verifications**: Executes `:verifyPlugin`.

## Project Structure

```text
.
├── .run/                                # Predefined Run/Debug configurations
├── gradle/
│   ├── wrapper/                         # Gradle Wrapper files
│   └── libs.versions.toml               # Version catalog
├── src/
│   ├── main/
│   │   ├── kotlin/de/spotlessformatplugin/
│   │   │   ├── listeners/               # Event listeners (SpotlessSaveListener)
│   │   │   ├── services/                # Core logic & services
│   │   │   │   ├── formatters/          # Formatter implementations:
│   │   │   │   │   ├── EclipseFormatter.kt
│   │   │   │   │   ├── GoogleJavaFormatFormatter.kt
│   │   │   │   │   ├── PrettierFormatter.kt
│   │   │   │   │   └── SpotlessConfigFormatter.kt
│   │   │   │   ├── DocumentTextService.kt
│   │   │   │   ├── SpotlessConfigResolver.kt
│   │   │   │   ├── SpotlessNotifier.kt
│   │   │   │   ├── SpotlessRunner.kt
│   │   │   │   └── SpotlessSettingsValidator.kt
│   │   │   └── settings/                # Settings UI and Persistent State
│   │   │       ├── SpotlessFormatConfigurable.kt
│   │   │       └── SpotlessFormatSettings.kt
│   │   └── resources/
│   │       └── META-INF/                # plugin.xml & pluginIcon.svg
│   └── test/
│       └── kotlin/de/spotlessformatplugin/
│           ├── services/                # Service & Formatter unit tests
│           └── settings/                # Settings tests
├── build.gradle.kts                     # Gradle build configuration
├── settings.gradle.kts                  # Gradle settings
├── gradle.properties                    # Project properties
├── CHANGELOG.md                         # Changelog
├── LICENSE                              # Apache 2.0 License
└── README.md                            # Documentation
```

## Testing

Tests are written using **JUnit 5** and **JUnit 4** (vintage engine) alongside the IntelliJ Platform test framework:

```bash
./gradlew test
```

## License

Copyright © 2026 kroeppelt. Distributed under the Apache 2.0 License. See `LICENSE` for details.

---

[docs]: https://plugins.jetbrains.com/docs/intellij
[jb:forum]: https://platform.jetbrains.com/
[gh:intellij-platform-gradle-plugin]: https://github.com/JetBrains/intellij-platform-gradle-plugin
