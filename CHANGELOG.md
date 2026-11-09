<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# SpotlessFormatPlugin Changelog

## [Unreleased]

### Fixed

- Format on save now runs as an IntelliJ *Action on Save*: the formatted content is what gets saved, the file is no longer left modified, and only files of the current project are formatted (previously every open project formatted every saved file with its own settings).
- Formatting results are discarded if the document changed while a formatter was running, so nothing typed in the meantime is lost.
- Results are applied as minimal line edits instead of replacing the whole document, which preserves the caret, folding and undo history.
- Google Java Format works in the IDE: the required `jdk.compiler` packages are opened at runtime (previously it failed with an `IllegalAccessError`).
- Relative paths and `$PROJECT_DIR$` in the settings are resolved against the project root instead of the IDE's working directory.
- The Eclipse formatter uses the Java language level of the file's module instead of a fixed Java 17 level, and always emits `\n` line separators.
- The Reformat Code action formats all selected files and restores the original action when the plugin is unloaded.
- The formatter settings can be edited when only the Reformat Code override is enabled.
- Relative import order paths are no longer searched above the project root.

### Changed

- Google Java Format always uses the bundled version (shown in the settings). The ineffective version field has been removed. A new option removes unused imports and sorts imports (enabled by default).
- The "Spotless" mode has been renamed to "Generic Spotless steps" and documented accurately: it trims trailing whitespace, ensures a final newline and applies an optional import order file. Spotless build scripts are not evaluated, and a warning is shown if one is configured.
- Success notifications are off by default (new option). Error notifications have specific titles.
- UI and notification texts have been moved to the resource bundle.
- Requires IntelliJ IDEA 2025.3 (build 253) or newer. The project now builds with a Java 21 toolchain.

### Removed

- Prettier support. Projects that had Prettier selected fall back to the Eclipse formatter. The default supported extensions are now `java,xml`.

### Build

- Fixed the release asset upload (wrong file name pattern). The release workflow fails if the tag does not match the project version.
- CI uses JDK 21 and runs the IntelliJ Plugin Verifier. GitHub Actions are pinned to commit SHAs and updated by Dependabot.
- Change notes on JetBrains Marketplace are generated from this changelog.
- Removed banner images from the plugin JAR and dropped the stale `.iml` file.
