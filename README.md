<p align="center">
  <img src="src/main/resources/META-INF/SpotlessFormatPlguin_1280x640.png" alt="SpotlessFormatPlugin Banner" width="600"/>
</p>

<p align="center">
  <a href="https://plugins.jetbrains.com/plugin/34407-spotless-formatter"><img src="https://img.shields.io/jetbrains/plugin/v/34092" alt="Version"/></a>
  <a href="https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/gradle.yml"><img src="https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/gradle.yml/badge.svg" alt="Build Status"/></a>
  <a href="https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/qodana_code_quality.yml"><img src="https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/qodana_code_quality.yml/badge.svg" alt="Qodana Code Quality"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License"/></a>
</p>

# Spotless Formatter Plugin for IntelliJ IDEA

**SpotlessFormatPlugin** brings [Spotless](https://github.com/diffplug/spotless) formatting capabilities directly into IntelliJ IDEA. It empowers developers and teams to enforce consistent code formatting across projects using **Eclipse XML formatters**, **Google Java Format**, **Prettier**, or custom **Spotless configuration files** (such as `spotless.gradle`, `.importorder`, or Eclipse XML settings).

---

## ✨ Features

- 🛠️ **Multiple Formatter Engines**:
  - **Eclipse Formatter**: Format Java source files using shared Eclipse XML formatter profiles and `.importorder` files with embedded Eclipse JDT core formatting.
  - **Google Java Format**: Format Java source code according to the Google Java Style guide, with configurable version support.
  - **Prettier**: Format JavaScript, TypeScript, JSON, YAML, HTML, CSS, and other files using local or global Prettier installations (with support for project `node_modules`, `npx`, NVM, Volta, asdf, nodenv, and Homebrew).
  - **Generic Spotless Configuration**: Use generic Spotless configuration files (such as `spotless.gradle`, `.order`, or `.importorder`) with automatic whitespace trimming, line-ending standardization, and import reordering.
- 🔍 **Hierarchical Configuration Resolution**: Automatically resolves configuration files by searching upwards from the file being formatted through its parent directories (ideal for multi-module and monorepo projects).
- 💾 **Format on Save**: Automatically reformat modified files upon document saving.
- ⚙️ **Configurable File Extensions**: Define custom comma-separated file extensions that trigger formatting (default: `java,xml,js,ts,json`).
- 📁 **Per-Project Configuration**: Settings are saved per project (`.idea/spotless-format-settings.xml`) for seamless team sharing.
- 🚨 **Real-Time Validation & Notifications**: Live validation in the settings UI with clear balloon notifications for errors or missing configurations.

---

## 📋 Requirements

- **IntelliJ IDEA**: 2025.3 or newer (tested up to 2026.2+).
- **JDK**: 17 or 21+ (as required by the IDE).
- **Node.js & Prettier** *(Optional)*: Required only when using the Prettier formatter engine.
- **Operating System**: macOS, Linux, or Windows.

---

## 🚀 Installation

### From JetBrains Marketplace
1. In IntelliJ IDEA, open **Settings** (`Cmd+,` on macOS / `Ctrl+Alt+S` on Windows & Linux) > **Plugins**.
2. Select the **Marketplace** tab and search for **Spotless Formatter**.
3. Click **Install** and restart the IDE if prompted.

### From Pre-built Plugin ZIP
1. Download the latest release `.zip` from the [Releases](https://github.com/dunuld/SpotlessFormatPlugin/releases) page.
2. Open **Settings** > **Plugins**.
3. Click the gear icon (⚙️) and choose **Install Plugin from Disk...**.
4. Select the downloaded ZIP file and restart the IDE.

### From Source
1. Clone the repository:
   ```bash
   git clone https://github.com/dunuld/SpotlessFormatPlugin.git
   ```
2. Open the project in IntelliJ IDEA.
3. Gradle will sync automatically and download required dependencies.

---

## ⚙️ Configuration

Open **Settings** (`Cmd+,` / `Ctrl+Alt+S`) > **Spotless Formatter** (or search for *"Spotless Formatter"*).

```
Spotless Formatter Settings
├── [ ] Use generic Spotless configuration file
│   └── Spotless Config: (path to spotless.gradle, .importorder, etc.)
│
├── Formatter Type: [ Eclipse | Prettier | Google Java Format ]
│   ├── Eclipse:
│   │   ├── Formatter XML: (path to eclipse-formatter.xml)
│   │   └── Import Order File: (path to custom.importorder)
│   ├── Prettier:
│   │   └── Prettier Config: (optional path to .prettierrc or prettier.config.js)
│   └── Google Java Format:
│       └── Version: (e.g., 1.17.0)
│
├── Supported Extensions: java,xml,js,ts,json
└── [x] Execute Spotless on save for changed files
```

### 1. Generic Spotless Mode
- **Use generic Spotless configuration file**: Enable this option to resolve formatting settings from a custom Spotless configuration file.
- **Spotless Config**: Specify a relative or absolute path (e.g., `spotless.gradle`, `spotless.xml`, `.importorder`).
  - *Hierarchical Search*: If a relative path is provided, the plugin searches starting from the folder containing the active file upwards to parent directories.

### 2. Dedicated Formatter Engines
When generic mode is unchecked, choose one of the dedicated formatters:

#### Eclipse Formatter
- **Formatter XML**: Path to your Eclipse code formatter XML export file.
- **Import Order File**: Path to your Eclipse-compatible `.importorder` file.

#### Prettier
- **Prettier Config**: *(Optional)* Path to a `.prettierrc`, `.prettierrc.json`, or `prettier.config.js` configuration file.
- **Auto-Detection**: Searches for `prettier` binaries in:
  - Local `node_modules/.bin/prettier`
  - System `PATH`
  - Node version managers (`~/.nvm`, `~/.volta`, `~/.asdf`, `~/.nodenv`) and Homebrew paths
  - `npx prettier` fallback

#### Google Java Format
- **Version**: Google Java Format version string (e.g., `1.17.0`, `1.22.0`). Applies to `.java` files.

### 3. General Settings
- **Supported Extensions**: Comma-separated list of file extensions to format (e.g., `java,xml,js,ts,json,kt`).
- **Execute Spotless on save for changed files**: When checked, formats eligible files on document save.

---

## 🛠️ Development

This project is built with **Kotlin** and **Gradle (Kotlin DSL)** using the [IntelliJ Platform Gradle Plugin (2.x)](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html).

### Useful Gradle Tasks

```bash
# Launch a development instance of IntelliJ IDEA with the plugin loaded
./gradlew runIde

# Run all unit and integration tests
./gradlew test

# Validate plugin descriptor, dependencies, and binary compatibility
./gradlew verifyPlugin

# Build the distributable plugin ZIP archive (outputs to build/distributions/)
./gradlew buildPlugin

# Publish the plugin to JetBrains Marketplace (requires JETBRAINS_TOKEN)
./gradlew publishPlugin
```

### Predefined Run Configurations

Pre-configured run targets located under `.run/`:
- **Run Plugin**: Launches the plugin in a sandbox IDE (`runIde`).
- **Run Tests**: Executes test suite (`test`).
- **Run Verifications**: Executes plugin verifications (`verifyPlugin`).

---

## 📁 Project Structure

```text
.
├── .run/                                # Predefined Run/Debug configurations
├── gradle/
│   ├── wrapper/                         # Gradle Wrapper files
│   └── libs.versions.toml               # Version catalog
├── src/
│   ├── main/
│   │   ├── kotlin/de/spotlessformatplugin/
│   │   │   ├── listeners/               # Save listeners (SpotlessSaveListener)
│   │   │   ├── services/                # Core formatting services & logic
│   │   │   │   ├── formatters/          # Engine implementations:
│   │   │   │   │   ├── EclipseFormatter.kt
│   │   │   │   │   ├── GoogleJavaFormatFormatter.kt
│   │   │   │   │   ├── PrettierFormatter.kt
│   │   │   │   │   └── SpotlessConfigFormatter.kt
│   │   │   │   ├── DocumentTextService.kt
│   │   │   │   ├── SpotlessConfigResolver.kt
│   │   │   │   ├── SpotlessNotifier.kt
│   │   │   │   ├── SpotlessRunner.kt
│   │   │   │   └── SpotlessSettingsValidator.kt
│   │   │   └── settings/                # Settings UI and Persistent State Component
│   │   │       ├── SpotlessFormatConfigurable.kt
│   │   │       └── SpotlessFormatSettings.kt
│   │   └── resources/
│   │       └── META-INF/                # plugin.xml & plugin assets (icons, banners)
│   └── test/
│       └── kotlin/de/spotlessformatplugin/
│           ├── services/                # Service & Formatter unit tests
│           └── settings/                # Settings tests
├── build.gradle.kts                     # Gradle build script
├── settings.gradle.kts                  # Gradle project settings
├── gradle.properties                    # Project properties & caching config
├── CHANGELOG.md                         # Release history & notes
├── LICENSE                              # Apache 2.0 License
└── README.md                            # Documentation
```

---

## 🧪 Testing

Tests are written using **JUnit 5** and **JUnit 4** (via the vintage engine) alongside the IntelliJ Platform test framework:

```bash
./gradlew test
```

---

## 📄 License

Distributed under the Apache 2.0 License. See [LICENSE](LICENSE) for details.
