# Memory Map (MMAP) — TextMate bundle

Syntax highlighting for `.mmap` files (MIDI device memory layouts).

The same bundle works in **VS Code** and **IntelliJ IDEA** (through the
*TextMate Bundles* plugin, bundled and enabled by default).

## Contents

| File | Purpose |
|---|---|
| `package.json` | VS Code manifest: `mmap` language, `.mmap` extension, grammar |
| `syntaxes/mmap.tmLanguage.json` | the TextMate grammar |
| `language-configuration.json` | `#` comments, brackets, auto-closing |
| `README.md` | this document |

---

# VS Code

## Prerequisites

- Recent VS Code.
- Node.js only for the VSIX option (`npx`).

## Option 1 — install from the folder (recommended)

1. `Ctrl+Shift+P`
2. **Developer: Install Extension from Location...**
3. Select `D:\github-checkout\Audio-Workshops\tools\textmate-mmap`
4. Reload the window if VS Code asks.

## Option 2 — junction (to edit the grammar without reinstalling)

```powershell
New-Item -ItemType Junction `
  -Path "$env:USERPROFILE\.vscode\extensions\hypercube-software.mmap-0.1.0" `
  -Target "D:\github-checkout\Audio-Workshops\tools\textmate-mmap"
```

Then restart VS Code.

- Remove the target folder first if it already exists.
- A Windows junction does not require admin rights (unlike a symlink).
- After each change to `syntaxes/mmap.tmLanguage.json`:
  `Ctrl+Shift+P` → **Developer: Reload Window**.

## Option 3 — VSIX

```powershell
cd D:\github-checkout\Audio-Workshops\tools\textmate-mmap
npx --yes @vscode/vsce package --allow-missing-repository

& "$env:LOCALAPPDATA\Programs\Microsoft VS Code\bin\code.cmd" `
  --install-extension .\mmap-0.1.0.vsix
```

The `code` CLI may not be in `PATH`; the full path above works for a standard
user installation. Otherwise, in VS Code:
`Extensions → ... → Install from VSIX...`.

## Verify

Open `DS-330.mmap`. The status bar should show **Memory Map**.

Otherwise:

- `Ctrl+K M` → **Memory Map**, or
- in `settings.json`:

```json
"files.associations": { "*.mmap": "mmap" }
```

## Inspect scopes

`Ctrl+Shift+P` → **Developer: Inspect Editor Tokens and Scopes**
(formerly *Inspect TM Scopes*), then hover a token.

Expected examples:

```
PatchSection  -> constant.other.type-reference.mmap
Section       -> meta.tag.array-index.mmap
byte          -> storage.type.primitive.mmap
4             -> constant.numeric.array-size.mmap
# comment     -> comment.line.number-sign.mmap
```

## VS Code troubleshooting

| Symptom | Cause / fix |
|---|---|
| No highlighting | the file is not in `Memory Map` mode → `Ctrl+K M` or `files.associations` |
| No highlighting after editing the grammar | VS Code caches grammars → **Developer: Reload Window** |
| Colors differ from IntelliJ | expected: each theme maps TextMate scopes differently |
| Extension not listed | check that `package.json` is at the root of the installed folder |

---

# IntelliJ IDEA

## Installation

1. `Ctrl+Alt+S` → **Editor → TextMate Bundles**
2. `+` → select `D:\github-checkout\Audio-Workshops\tools\textmate-mmap`
3. Make sure the bundle checkbox is selected
4. **Apply**

The bundle is read **in place**: IntelliJ stores only the path, not a copy.
On Windows it is stored in:

```
%APPDATA%\JetBrains\<Product><Version>\options\textmate.xml
```

If the folder is moved or renamed, remove and re-add the bundle.

## Extension mapping

`.mmap` is normally associated automatically through
`package.json → contributes.languages[0].extensions`.

Otherwise: `Settings → Editor → File Types`, search **Memory Map**, add `*.mmap`.

## Reload after a change

IntelliJ caches grammars. After editing `mmap.tmLanguage.json`:

- **Restart the IDE** (simplest), or
- uncheck the bundle → **Apply** → check it again → **Apply**.

Note: removing and re-adding the same path and applying **once** does not
reload anything, because the final state is identical to the initial state and
`isModified()` returns `false`.

## IntelliJ troubleshooting

| Symptom | Cause / fix |
|---|---|
| No highlighting | bundle unchecked, or `.mmap` not associated → File Types |
| Changes ignored | restart, or perform the **two** Applies (uncheck then check) |
| A custom type is not colored | the theme does not define **Constant** → see below |
| Field names are not colored | `variable.other.member` → **Local variable**, often undefined in default schemes |

---

# Colors and scopes

Colors come from the theme, not from the bundle. Scopes used:

| Element | Scope | IntelliJ attribute |
|---|---|---|
| Native types (`byte`, `short`, `int`, `long`, `String`) | `storage.type.primitive` | Keyword |
| Custom types (`PatchSection`, `Switch`, `ToneParams`) | `constant.other.type-reference` | Constant |
| Declared names (struct, enum, zone, model) | `entity.name.function.*` | Function declaration |
| Addresses, sizes, counters | `constant.numeric.*` | Number |
| Enum values | `string.unquoted.enum-value` | String |
| Dimension labels (`Part`, `Section`, `Note`, `block`) | `meta.tag.array-index` | Metadata |
| `BYTES` / `NIBBLES` | `keyword.other.format` | Keyword |
| Field names | `variable.other.member` | Local variable (often undefined) |
| Comments | `comment.line` / `comment.documentation` | Line / Doc comment |
| Separators `# ----` | `markup.heading` | underline |

The default IntelliJ schemes do not define `DEFAULT_CLASS_NAME`,
`DEFAULT_LOCAL_VARIABLE`, `DEFAULT_PARAMETER` or `DEFAULT_PREDEFINED_SYMBOL`.
That is why the bundle avoids those scopes and uses `Constant`,
`Function declaration`, `Metadata`, etc., which are actually colored.

To change a color:

- **IntelliJ**: `Settings → Editor → Color Scheme → Language Defaults`
  (e.g. *Identifiers → Local variable* for field names).
- **VS Code**: depends on the active theme; the `.mmap` scope can be targeted
  specifically by a TextMate theme, otherwise the base scopes apply.
