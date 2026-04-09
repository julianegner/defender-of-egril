# Impressum Visual Guide

## Overview
This document describes the visual appearance of the impressum feature when enabled on the WASM platform.

## Main Menu Screen (with impressum enabled)

```
┌─────────────────────────────────────────────────────────────────┐
│                                                      [ℹ️] [⚙️]    │
│  [Exit]                                                          │
│  Player: John Doe    [Switch Player]                            │
│                                                                  │
│                                                                  │
│                    ┌─────────────────┐                          │
│                    │  DEFENDER OF    │                          │
│                    │     EGRIL       │                          │
│                    └─────────────────┘                          │
│                                                                  │
│              Turn-based Tower Defense Game                      │
│                                                                  │
│                                                                  │
│                  ┌─────────────────┐                            │
│                  │  START GAME     │                            │
│                  └─────────────────┘                            │
│                                                                  │
│                  ┌─────────────────┐                            │
│                  │     RULES       │                            │
│                  └─────────────────┘                            │
│                                                                  │
│                                                                  │
│                                                                  │
│  v1.0 (abc1234)                Impressum                        │
└─────────────────────────────────────────────────────────────────┘
```

### Impressum Link Behavior
- **Default state**: Shows small "Impressum" text link at bottom center
- **On click**: Expands to show full impressum information in a bordered box
- **Expanded state**: Shows "X" button in top-right corner to close

## Main Menu Screen (impressum expanded)

```
┌─────────────────────────────────────────────────────────────────┐
│                                                      [ℹ️] [⚙️]    │
│  [Exit]                                                          │
│  Player: John Doe    [Switch Player]                            │
│                                                                  │
│                                                                  │
│                    ┌─────────────────┐                          │
│                    │  DEFENDER OF    │                          │
│                    │     EGRIL       │                          │
│                    └─────────────────┘                          │
│                                                                  │
│              Turn-based Tower Defense Game                      │
│                                                                  │
│                                                                  │
│                  ┌─────────────────┐                            │
│                  │  START GAME     │                            │
│                  └─────────────────┘                            │
│                                                                  │
│                  ┌─────────────────┐                            │
│                  │     RULES       │                            │
│                  └─────────────────┘                            │
│                                                                  │
│  v1.0 (abc1234)  ┌───────────────────────────────┐             │
│                  │                             X  │             │
│                  │  Impressum                     │             │
│                  │                                │             │
│                  │  Julian Egner                  │             │
│                  │  Weissstrasse 18               │             │
│                  │  53123 Bonn                    │             │
│                  │  Germany                       │             │
│                  │                                │             │
│                  │  mail: admin@egril.de          │             │
│                  └───────────────────────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

## Installation Info Screen (bottom section, with impressum)

```
┌─────────────────────────────────────────────────────────────────┐
│                                 Installation Info     [⚙️]       │
│                                                                  │
│  [...scrollable content above...]                               │
│                                                                  │
│  Web/Browser                                                     │
│  • No installation required - just visit the website            │
│  • Works in modern browsers with WebAssembly support            │
│                                                                  │
│  For detailed documentation, visit:                             │
│  github.com/julianegner/defender-of-egril               │
│                                                                  │
│  ─────────────────────────────────────────────────────────────  │
│  Impressum                                                       │
│                                                                  │
│  Julian Egner                                                    │
│  Weissstrasse 18                                                │
│  53123 Bonn                                                      │
│  Germany                                                         │
│                                                                  │
│  mail: admin@egril.de                                           │
│                                                                  │
│                                                                  │
│                      ┌─────────────┐                            │
│                      │    BACK     │                            │
│                      └─────────────┘                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Key Visual Features

### Main Menu Impressum Link
- **Location**: Bottom center of screen
- **Size**: Small text (MaterialTheme.typography.bodySmall)
- **Color**: Follows theme colors
- **Interaction**: Clickable, expands on click

### Expanded Impressum Box
- **Border**: 1dp outline in theme's outline color
- **Padding**: 8dp around content
- **Background**: Follows theme background
- **Close button**: "X" in gray, positioned top-right
- **Content padding**: 12dp inside the box

### Installation Info Screen Impressum
- **Location**: Bottom of scrollable content, above back button
- **Separator**: Horizontal line above the impressum section
- **Title**: "Impressum" in TitleLarge typography
- **Layout**: Static display (not collapsible)
- **Spacing**: 20dp above the section

### Email Link Styling
- **Color**: Primary color from theme
- **Decoration**: Underlined
- **Interaction**: Clickable, opens "mailto:" link

## Behavior Notes
- Impressum only appears when:
  1. Platform is WASM (web browser)
  2. `withImpressum=true` in gradle.properties
- On other platforms (Desktop, Android, iOS), no impressum is shown
- Email link opens the default mail application
- All text maintains readability in both light and dark themes
