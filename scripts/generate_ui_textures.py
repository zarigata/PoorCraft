"""
Generate UI textures for PoorCraft
Creates simple placeholder textures with Minecraft-style appearance
"""
from PIL import Image, ImageDraw
import os

def create_button_texture():
    """Create a 64x16 Minecraft-style button texture"""
    img = Image.new('RGBA', (64, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Base color - gray stone
    draw.rectangle([0, 0, 63, 15], fill=(139, 139, 139, 255))
    
    # Top highlight
    draw.rectangle([1, 1, 62, 2], fill=(180, 180, 180, 255))
    
    # Bottom shadow
    draw.rectangle([1, 13, 62, 14], fill=(100, 100, 100, 255))
    
    # Left highlight
    draw.rectangle([1, 2, 2, 12], fill=(160, 160, 160, 255))
    
    # Right shadow
    draw.rectangle([61, 2, 62, 12], fill=(110, 110, 110, 255))
    
    # Corners
    draw.point((0, 0), fill=(120, 120, 120, 255))
    draw.point((63, 0), fill=(120, 120, 120, 255))
    draw.point((0, 15), fill=(120, 120, 120, 255))
    draw.point((63, 15), fill=(120, 120, 120, 255))
    
    return img

def create_hotbar_frame():
    """Create 800x60 hotbar frame"""
    img = Image.new('RGBA', (800, 60), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Dark semi-transparent background
    draw.rectangle([0, 0, 799, 59], fill=(0, 0, 0, 180))
    
    # Border
    draw.rectangle([0, 0, 799, 59], outline=(80, 80, 80, 255), width=2)
    
    return img

def create_hotbar_slot():
    """Create 48x48 hotbar slot"""
    img = Image.new('RGBA', (48, 48), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Dark background
    draw.rectangle([0, 0, 47, 47], fill=(30, 30, 30, 200))
    
    # Border
    draw.rectangle([0, 0, 47, 47], outline=(60, 60, 60, 255), width=1)
    
    return img

def create_hotbar_selection():
    """Create 56x56 selection highlight"""
    img = Image.new('RGBA', (56, 56), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Bright border with glow
    for i in range(3):
        alpha = 255 - (i * 60)
        draw.rectangle([i, i, 55-i, 55-i], outline=(255, 255, 255, alpha), width=1)
    
    return img

def create_heart_full():
    """Create 20x20 full heart"""
    img = Image.new('RGBA', (20, 20), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Simple heart shape - red
    heart_pixels = [
        (5, 4), (6, 4), (7, 4), (12, 4), (13, 4), (14, 4),
        (4, 5), (5, 5), (6, 5), (7, 5), (8, 5), (11, 5), (12, 5), (13, 5), (14, 5), (15, 5),
        (3, 6), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6), (12, 6), (13, 6), (14, 6), (15, 6), (16, 6),
        (3, 7), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7), (12, 7), (13, 7), (14, 7), (15, 7), (16, 7),
        (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8), (12, 8), (13, 8), (14, 8), (15, 8),
        (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (11, 9), (12, 9), (13, 9), (14, 9),
        (6, 10), (7, 10), (8, 10), (9, 10), (10, 10), (11, 10), (12, 10), (13, 10),
        (7, 11), (8, 11), (9, 11), (10, 11), (11, 11), (12, 11),
        (8, 12), (9, 12), (10, 12), (11, 12),
        (9, 13), (10, 13),
    ]
    
    for x, y in heart_pixels:
        draw.point((x, y), fill=(220, 20, 20, 255))
    
    return img

def create_heart_empty():
    """Create 20x20 empty heart"""
    img = Image.new('RGBA', (20, 20), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Heart outline only - dark gray
    heart_outline = [
        (5, 4), (6, 4), (7, 4), (12, 4), (13, 4), (14, 4),
        (4, 5), (8, 5), (11, 5), (15, 5),
        (3, 6), (16, 6),
        (3, 7), (16, 7),
        (4, 8), (15, 8),
        (5, 9), (14, 9),
        (6, 10), (13, 10),
        (7, 11), (12, 11),
        (8, 12), (11, 12),
        (9, 13), (10, 13),
    ]
    
    for x, y in heart_outline:
        draw.point((x, y), fill=(80, 20, 20, 255))
    
    return img

def create_armor_full():
    """Create 20x20 full armor icon"""
    img = Image.new('RGBA', (20, 20), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Simple chestplate shape - gray/silver
    armor_pixels = [
        # Shoulders
        (5, 4), (6, 4), (7, 4), (12, 4), (13, 4), (14, 4),
        (5, 5), (6, 5), (7, 5), (12, 5), (13, 5), (14, 5),
        # Body
        (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6), (12, 6), (13, 6),
        (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7), (12, 7), (13, 7),
        (7, 8), (8, 8), (9, 8), (10, 8), (11, 8), (12, 8),
        (7, 9), (8, 9), (9, 9), (10, 9), (11, 9), (12, 9),
        (7, 10), (8, 10), (9, 10), (10, 10), (11, 10), (12, 10),
        (8, 11), (9, 11), (10, 11), (11, 11),
        (8, 12), (9, 12), (10, 12), (11, 12),
    ]
    
    for x, y in armor_pixels:
        draw.point((x, y), fill=(180, 180, 200, 255))
    
    return img

def create_armor_empty():
    """Create 20x20 empty armor icon"""
    img = Image.new('RGBA', (20, 20), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Armor outline only - dark gray
    armor_outline = [
        (5, 4), (7, 4), (12, 4), (14, 4),
        (5, 5), (7, 5), (12, 5), (14, 5),
        (6, 6), (13, 6),
        (6, 7), (13, 7),
        (7, 8), (12, 8),
        (7, 10), (12, 10),
        (8, 11), (11, 11),
        (8, 12), (11, 12),
    ]
    
    for x, y in armor_outline:
        draw.point((x, y), fill=(60, 60, 70, 255))
    
    return img

def create_xp_bar_background():
    """Create 360x10 XP bar background"""
    img = Image.new('RGBA', (360, 10), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Dark bar with border
    draw.rectangle([0, 0, 359, 9], fill=(20, 20, 20, 200))
    draw.rectangle([0, 0, 359, 9], outline=(60, 60, 60, 255), width=1)
    
    return img

def create_xp_bar_fill():
    """Create 360x10 XP bar fill"""
    img = Image.new('RGBA', (360, 10), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Bright green/yellow gradient
    for x in range(360):
        # Gradient from green to yellow
        r = int(100 + (x / 360) * 155)
        g = 220
        b = 20
        draw.line([(x, 1), (x, 8)], fill=(r, g, b, 255))
    
    return img

def main():
    """Generate all UI textures"""
    # Create directories
    ui_files_dir = "UI_FILES"
    textures_ui_dir = os.path.join("src", "main", "resources", "textures", "ui")
    
    os.makedirs(ui_files_dir, exist_ok=True)
    os.makedirs(textures_ui_dir, exist_ok=True)
    
    # Generate and save textures
    textures = {
        os.path.join(ui_files_dir, "button.png"): create_button_texture(),
        os.path.join(textures_ui_dir, "hotbar_frame.png"): create_hotbar_frame(),
        os.path.join(textures_ui_dir, "hotbar_slot.png"): create_hotbar_slot(),
        os.path.join(textures_ui_dir, "hotbar_selection.png"): create_hotbar_selection(),
        os.path.join(textures_ui_dir, "heart_full.png"): create_heart_full(),
        os.path.join(textures_ui_dir, "heart_empty.png"): create_heart_empty(),
        os.path.join(textures_ui_dir, "armor_full.png"): create_armor_full(),
        os.path.join(textures_ui_dir, "armor_empty.png"): create_armor_empty(),
        os.path.join(textures_ui_dir, "xp_bar_background.png"): create_xp_bar_background(),
        os.path.join(textures_ui_dir, "xp_bar_fill.png"): create_xp_bar_fill(),
    }
    
    for path, img in textures.items():
        img.save(path)
        print(f"Created: {path}")
    
    print("\nAll UI textures generated successfully!")

if __name__ == "__main__":
    main()
