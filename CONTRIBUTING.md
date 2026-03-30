# Contributing to Defender of Egril

Thank you for your interest in contributing to Defender of Egril! This document provides guidelines and information to help you contribute effectively.

## Table of Contents

- [How to Report Issues](#how-to-report-issues)
- [How to Request Features](#how-to-request-features)
- [How to Contribute Code](#how-to-contribute-code)
- [How to Contribute Assets](#how-to-contribute-assets)
- [Testing Requirements](#testing-requirements)
- [Use of AI Tools](#use-of-ai-tools)
- [Credits](#credits)
- [Documentation Overview](#documentation-overview)

---

## How to Report Issues

If you find a bug or experience unexpected behavior, please [open a GitHub Issue](https://github.com/qvest-digital/defender-of-egril-fork/issues).

When reporting a bug, please include:
- A clear, descriptive title
- Steps to reproduce the problem
- Expected behavior vs. actual behavior
- Screenshots or logs if applicable
- Platform (desktop, Android, web/WASM) and version

**Before opening a new issue**, please search the existing issues to avoid duplicates.

---

## How to Request Features

Have an idea for a new feature or improvement? [Open a GitHub Issue](https://github.com/qvest-digital/defender-of-egril-fork/issues) with the label `enhancement`.

Please describe:
- What the feature would do
- Why it would be useful
- Any ideas for how it might be implemented

---

## How to Contribute Code

Be aware that all code that is merged into the project must be licensed under the [GNU Affero General Public License v3 (AGPL-3.0)](LICENSE). By submitting a pull request, you agree that your contribution will be licensed under AGPL-3.0.

All code contributions should be made through **Pull Requests**.

### Steps

1. **Fork** the repository on GitHub.
2. **Create a branch** for your change (`git checkout -b my-feature-branch`).
3. **Make your changes** (see guidelines below).
4. **Test your changes** (see [Testing Requirements](#testing-requirements)).
5. **Open a Pull Request** against the `main` branch, with a clear description of what you changed and why.

### Guidelines

- Keep your pull requests **focused and small** — don't try to change the world in one PR. Smaller, targeted changes are easier to review and merge.
- Follow the existing code style and conventions described in [docs/root/DEVELOPMENT.md](docs/root/DEVELOPMENT.md).
- All user-facing strings must be added to all language files (see [docs/implementation/LOCALIZATION_IMPLEMENTATION.md](docs/implementation/LOCALIZATION_IMPLEMENTATION.md)).
- Do **not** add Unicode emoji characters directly to code or string resources — use icon components instead (see the `ui/icon/` directory).
- Ensure your changes don't break existing behavior.
- run the test suite before submitting to catch any regressions (see below).

Be aware that all code that is merged into the project must be licensed under the [GNU Affero General Public License v3 (AGPL-3.0)](LICENSE). By submitting a pull request, you agree that your contribution will be licensed under AGPL-3.0.

---

## How to Contribute Assets

If you want to contribute images, sounds, or other assets:

1. Add the asset file(s) to the appropriate directory in the repository.
   (in the subdirectories of composeApp/src/commonMain/composeResources)
2. **Update the `README.md` in the same directory** where you added the file(s), listing:
   - The filename
   - The URL/source of the asset
   - The name of the creator
   - The license of the asset
3. **Check license compatibility**: All contributed assets must be compatible with the [GNU Affero General Public License v3 (AGPL-3.0)](LICENSE) used by this project. Common compatible licenses include CC0, CC-BY, and CC-BY-SA. If you are unsure, ask in the issue or PR.
4. Submit the asset and `README.md` update together in your Pull Request.

---

## Testing Requirements

All code contributions must be covered by automated tests where possible.

- **Game logic** (towers, enemies, combat, pathfinding, etc.) must be covered by unit tests in `composeApp/src/commonTest/`.
- **UI changes** that cannot be fully covered by automated tests must include a written description of how to manually test the change. Add this to your Pull Request description.
- Run the existing test suite before submitting to ensure you haven't introduced regressions:

```bash
# Run common tests
./gradlew :composeApp:cleanTestDebugUnitTest :composeApp:testDebugUnitTest

# Run translation coverage test (required for any UI string changes)
./gradlew :composeApp:testDebugUnitTest --tests "de.egril.defender.ui.TranslationCoverageTest"
```

See [docs/guides/TESTING_GUIDE.md](docs/guides/TESTING_GUIDE.md) for more details on testing.

---

## Use of AI Tools

The use of AI coding assistants (e.g., GitHub Copilot, ChatGPT, etc.) is **allowed** and can help with productivity. 
**However**:

- **Don't change the world in one PR.** AI tools can generate large amounts of code quickly — please keep contributions focused and incremental.
- You are responsible for reviewing and understanding any AI-generated code before submitting it.
- Make sure AI-generated code still meets all the testing, style, and quality requirements above.

---

## Credits

Contributors are encouraged to **add themselves to the final credits** of the game!

The credits are maintained in [`composeApp/src/commonMain/kotlin/de/egril/defender/ui/FinalCreditsData.kt`](composeApp/src/commonMain/kotlin/de/egril/defender/ui/FinalCreditsData.kt).

- Plaase add an entry to the `contributors` list with a short description of your contribution.

---

## Documentation Overview

The following documents may help you understand the project before contributing:

| Document | Description |
|----------|-------------|
| [README.md](README.md) | Project overview and quick start |
| [INSTALL.md](INSTALL.md) | Installation instructions for all platforms |
| [docs/root/DEVELOPMENT.md](docs/root/DEVELOPMENT.md) | Architecture, code conventions, and development guide |
| [docs/root/GAMEPLAY.md](docs/root/GAMEPLAY.md) | Game mechanics and rules |
| [docs/guides/TESTING_GUIDE.md](docs/guides/TESTING_GUIDE.md) | Manual testing procedures |
| [docs/guides/LEVEL_EDITOR.md](docs/guides/LEVEL_EDITOR.md) | Level editor guide (desktop/web only) |
| [docs/implementation/LOCALIZATION_IMPLEMENTATION.md](docs/implementation/LOCALIZATION_IMPLEMENTATION.md) | Localization system and adding new languages |
| [docs/implementation/SAVE_LOAD_IMPLEMENTATION.md](docs/implementation/SAVE_LOAD_IMPLEMENTATION.md) | Save/load system architecture |
| [docs/guides/WEB_WASM_GUIDE.md](docs/guides/WEB_WASM_GUIDE.md) | Web/WASM platform guide |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Community standards and expectations |
