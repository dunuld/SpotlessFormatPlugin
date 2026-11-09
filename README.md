<p>
  <img src=".github/images/SpotlessFormatPlugin_1280x640.png" alt="SpotlessFormatPlugin Banner" width="600"/>
</p>

<p>
  <a href="https://plugins.jetbrains.com/plugin/34407-spotless-formatter"><img src="https://img.shields.io/jetbrains/plugin/v/34407" alt="Version"/></a>
  <a href="https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/gradle.yml"><img src="https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/gradle.yml/badge.svg" alt="Build Status"/></a>
  <a href="https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/qodana_code_quality.yml"><img src="https://github.com/dunuld/SpotlessFormatPlugin/actions/workflows/qodana_code_quality.yml/badge.svg" alt="Qodana Code Quality"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License"/></a>
</p>

# Spotless Formatter Plugin for IntelliJ IDEA

**SpotlessFormatPlugin** brings [Spotless](https://github.com/diffplug/spotless)-style formatting directly into IntelliJ IDEA. It helps teams enforce consistent formatting with **Eclipse XML formatters**, **Google Java Format**, **Prettier**, or the **generic Spotless steps** (trailing whitespace, final newline, import order).

> ℹ️ The plugin runs the formatters itself. It does **not** read or execute Spotless build configuration (`spotless { ... }` in Gradle or Maven).

---

## ✨ Features

- 🛠️ **Multiple Formatter Engines**:
  - **Eclipse Formatter**: Formats Java files using shared Eclipse JDT formatter XML profiles and `.importorder` files with the embedded Eclipse JDT core formatter. Named profiles in the XML file are detected automatically. The Java source level is taken from the file's module. Other file types only get trailing-whitespace and final-newline fixes.
  - **Google Java Format**: Formats Java files according to the Google Java Style Guide using the bundled google-java-format. Optionally removes unused imports and sorts imports, like the google-java-format CLI does by default.
  - **Prettier**: Formats JavaScript, TypeScript, JSON, YAML, HTML, CSS and more with a project-local or global Prettier installation (`node_modules`, `PATH`, NVM, Volta, asdf, nodenv, Homebrew, or `npx --no-install`).
  - **Generic Spotless Steps**: Trims trailing whitespace, ensures a final newline and, for Java files, applies an optional import order file.
- 🔍 **Hierarchical Import Order Lookup**: In the generic mode, a relative import order path is searched upwards from the formatted file, up to the project root. This works well for multi-module projects.
- 💾 **Actions on Save**: Formats files when you save them explicitly, integrated in **Settings > Tools > Actions on Save**. Formatting runs in the background, only for files of the current project, and the formatted content is what gets saved.
- ⌨️ **Reformat Code Action Override**: Optionally replaces IntelliJ's **Reformat Code** action (`Cmd+Alt+L` on macOS / `Ctrl+Alt+L` on Windows/Linux). When every selected file has a configured extension, those files are formatted with Spotless. Directories and other files keep the standard behavior.
- ✍️ **Safe Editing**: Only changed lines are replaced, so the caret, folding and undo history are preserved. If you keep typing while a slow formatter runs, the result is discarded instead of overwriting your changes.
- ⚙️ **Configurable File Extensions**: Define the comma-separated file extensions that are formatted (default: `java,xml,js,ts,json`).
- 📁 **Per-Project, Shareable Configuration**: Settings are saved per project (`.idea/spotless-format-settings.xml`). Relative paths and `$PROJECT_DIR$` are resolved against the project root, so the settings work on every machine.
- 🔒 **Trusted Projects Only for Prettier**: Prettier executes code from the project (local binaries, `prettier.config.js`, plugins), so it never runs in projects that are not trusted.
- 🚨 **Validation & Notifications**: Live validation in the settings UI. Error notifications are on by default. Success notifications are optional.

---

## 📋 Requirements

- **IntelliJ IDEA**: 2025.3 (build 253) or newer. Compatibility with newer IDE versions is checked by the IntelliJ Plugin Verifier in CI.
- **JDK** *(development only)*: 21. The IDE itself runs on its bundled JetBrains Runtime.
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
├── [x] Execute Spotless on save for changed files
├── [ ] Use Spotless when running IntelliJ's reformat code action
├── Mode: [ Formatter | Generic Spotless steps ]
│
├── (Mode: Formatter)
│   ├── Formatter Selection:
│   │   └── Formatter type: [ Eclipse | Prettier | Google Java Format ]
│   ├── Eclipse:
│   │   ├── Formatter XML: (path to eclipse-formatter.xml)
│   │   ├── Formatter profile: [ Default | CustomProfile ]
│   │   └── Import order file: (path to custom.importorder)
│   ├── Prettier:
│   │   └── Prettier config: (optional path to .prettierrc or prettier.config.js)
│   └── Google Java Format:
│       └── [x] Remove unused imports and sort imports
│
├── (Mode: Generic Spotless steps)
│   └── Import order file: (optional, e.g. config/java.importorder)
│
├── Supported extensions: java,xml,js,ts,json
├── [x] Show error notifications
└── [ ] Show a notification after each successful formatting
```

All paths can be absolute, relative to the project root, or use `$PROJECT_DIR$`.

### 1. General Controls
- **Execute Spotless on save for changed files**: When enabled, project files matching the supported extensions are formatted when you save them explicitly (for example with `Cmd+S` / `Ctrl+S`).
  > 💡 **Actions on Save**: You can also toggle this option directly under **Settings > Tools > Actions on Save > "Format changed files with Spotless"**.
- **Use Spotless when running IntelliJ's reformat code action**: When enabled, pressing `Cmd+Alt+L` (macOS) / `Ctrl+Alt+L` (Windows/Linux) formats the selected files with Spotless instead of the standard IDE formatter. This only applies when every selected file has a supported extension; otherwise the IDE formatter runs.
- **Supported Extensions**: Comma-separated list of file extensions to format (e.g., `java,xml,js,ts,json,kt`).
- **Show error notifications**: Balloon notifications for configuration problems and formatting errors (on by default).
- **Show a notification after each successful formatting**: Off by default, so saving does not produce a balloon every time.

### 2. Configuration Modes

#### Mode: Formatter
Select and configure a specific formatting engine:

- **Eclipse Formatter**:
  - **Formatter XML**: Path to your Eclipse code formatter XML export file.
  - **Formatter Profile**: Select a specific named profile defined inside the XML file.
  - **Import Order File**: Path to your Eclipse-compatible `.importorder` file.
- **Prettier**:
  - **Prettier Config**: *(Optional)* Path to a `.prettierrc`, `.prettierrc.json`, or `prettier.config.js` configuration file.
  - **Auto-Detection**: Searches for `prettier` binaries in:
    - Local `node_modules/.bin/prettier`
    - System `PATH`
    - Node version managers (`~/.nvm`, `~/.volta`, `~/.asdf`, `~/.nodenv`) and Homebrew paths
    - `npx --no-install prettier` fallback (never downloads packages)
  - **Trusted projects only**: Prettier is not executed in untrusted projects.
- **Google Java Format**:
  - Uses the google-java-format version bundled with the plugin (shown in the settings). Applies to `.java` files.
  - **Remove unused imports and sort imports**: Enabled by default, matching the google-java-format CLI.

#### Mode: Generic Spotless Steps
Trims trailing whitespace and ensures a final newline for all supported files.
- **Import order file** *(optional)*: An Eclipse-style import order file (e.g. `java.importorder`) applied to Java files.
  - *Hierarchical Search*: A relative path is searched upwards from the folder of the formatted file, but never above the project root. If nothing is found there, the path is resolved against the project root.
  - Spotless build scripts such as `spotless.gradle` are **not** evaluated. If such a file is configured, only the whitespace steps are applied and a warning is shown.

---

## 🛠️ Development

This project is built with **Kotlin** and **Gradle (Kotlin DSL)** using the [IntelliJ Platform Gradle Plugin (2.x)](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html).

### Useful Gradle Tasks

```bash
# Launch a development instance of IntelliJ IDEA with the plugin loaded
./gradlew runIde

# Run all unit and integration tests
./gradlew test

# Check binary compatibility with the recommended IDE versions (IntelliJ Plugin Verifier)
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
│   │   │   ├── SpotlessBundle.kt        # Localized UI and notification texts
│   │   │   ├── actions/                 # Reformat Code action wrapper and installer
│   │   │   ├── actionsonsave/           # Actions on Save integration (SpotlessActionOnSave, info provider)
│   │   │   ├── services/                # Core formatting services & logic
│   │   │   │   ├── formatters/          # Engine implementations:
│   │   │   │   │   ├── EclipseFormatter.kt
│   │   │   │   │   ├── GoogleJavaFormatFormatter.kt
│   │   │   │   │   ├── PrettierFormatter.kt
│   │   │   │   │   └── SpotlessConfigFormatter.kt
│   │   │   │   ├── DocumentUpdater.kt   # Applies results as minimal line edits
│   │   │   │   ├── JdkCompilerAccess.kt # Grants google-java-format access to jdk.compiler
│   │   │   │   ├── SpotlessConfigResolver.kt
│   │   │   │   ├── SpotlessNotifier.kt
│   │   │   │   ├── SpotlessPaths.kt     # Project-relative path resolution
│   │   │   │   ├── SpotlessRunner.kt    # Snapshot -> format in background -> apply if unchanged
│   │   │   │   └── SpotlessSettingsValidator.kt
│   │   │   └── settings/                # Settings UI and Persistent State Component
│   │   │       ├── SpotlessFormatConfigurable.kt
│   │   │       └── SpotlessFormatSettings.kt
│   │   └── resources/
│   │       ├── messages/                # I18n resource bundles (SpotlessBundle.properties)
│   │       └── META-INF/                # plugin.xml & plugin icon
│   └── test/
│       └── kotlin/de/spotlessformatplugin/
│           ├── actions/                 # Reformat action tests
│           ├── actionsonsave/           # Actions on save tests
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
