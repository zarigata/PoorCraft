# Fonts

Place TrueType (.ttf) font files here for UI rendering.

## Default Font

The UI system will look for `default.ttf` as the primary font. If not found, it will attempt to use a system font.

## Recommended Fonts

For a Minecraft-like aesthetic, consider:
- **Minecraft Font** (if licensing allows)
- **Press Start 2P** (free, retro pixel font)
- **Roboto** (clean, modern, free)
- **Arial/Liberation Sans** (system fallback)

## Font Requirements

- Format: TrueType (.ttf)
- Character set: At minimum, ASCII 32-126 (printable characters)
- Size: Font will be rasterized at 16-24px for UI elements

## Licensing

Ensure any fonts used have appropriate licenses for distribution. Many free fonts are available under SIL Open Font License or similar permissive licenses.

## Note

If no font file is provided, the system will fall back to immediate-mode text rendering.
This works but doesn't look as nice. For best results, include a TTF font file.
