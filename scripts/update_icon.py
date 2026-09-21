import os
import sys
from PIL import Image, ImageDraw

def process_icon(source_path, res_dir):
    if not os.path.exists(source_path):
        print(f"Error: Source image not found at '{source_path}'")
        return False

    print(f"Processing icon from: {source_path}")
    src_img = Image.open(source_path).convert("RGBA")

    # Target densities and sizes for ic_launcher (square & round) and foreground
    sizes_launcher = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    sizes_foreground = {
        "drawable-mdpi": 108,
        "drawable-hdpi": 162,
        "drawable-xhdpi": 216,
        "drawable-xxhdpi": 324,
        "drawable-xxxhdpi": 432,
    }

    # 1. Generate Legacy Square and Round ic_launcher
    for folder, size in sizes_launcher.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)

        # Square icon
        sq_img = src_img.resize((size, size), Image.Resampling.LANCZOS)
        sq_path = os.path.join(folder_path, "ic_launcher.png")
        sq_img.save(sq_path, "PNG")

        # Round icon with circular mask
        mask = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)

        rd_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        rd_img.paste(sq_img, (0, 0), mask=mask)
        rd_path = os.path.join(folder_path, "ic_launcher_round.png")
        rd_img.save(rd_path, "PNG")
        print(f"Generated {folder}: ic_launcher.png & ic_launcher_round.png ({size}x{size})")

    # 2. Generate Adaptive Foreground ic_launcher_foreground
    for folder, canvas_size in sizes_foreground.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)

        # For adaptive foreground, center the image within safe zone (~72% scale)
        safe_size = int(canvas_size * 0.72)
        scaled_src = src_img.resize((safe_size, safe_size), Image.Resampling.LANCZOS)

        fg_img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
        offset = (canvas_size - safe_size) // 2
        fg_img.paste(scaled_src, (offset, offset))

        fg_path = os.path.join(folder_path, "ic_launcher_foreground.png")
        fg_img.save(fg_path, "PNG")
        print(f"Generated {folder}: ic_launcher_foreground.png ({canvas_size}x{canvas_size})")

    # Also save 512x512 preview version in drawable-xxxhdpi
    fg_512 = src_img.resize((512, 512), Image.Resampling.LANCZOS)
    fg_512.save(os.path.join(res_dir, "drawable-xxxhdpi", "ic_launcher_foreground_512.png"), "PNG")

    # 3. Generate ic_app.png in core/designsystem
    ds_res_dir = "D:/定位/core/designsystem/src/main/res"
    for folder, size in sizes_launcher.items():
        ds_folder_path = os.path.join(ds_res_dir, folder)
        os.makedirs(ds_folder_path, exist_ok=True)
        app_img = src_img.resize((size, size), Image.Resampling.LANCZOS)
        app_img.save(os.path.join(ds_folder_path, "ic_app.png"), "PNG")
        print(f"Generated core:designsystem {folder}: ic_app.png ({size}x{size})")

    # 4. Generate ic_app_launcher.png in feature/widget/impl
    widget_drawable_dir = "D:/定位/feature/widget/impl/src/main/res/drawable"
    os.makedirs(widget_drawable_dir, exist_ok=True)
    widget_img = src_img.resize((96, 96), Image.Resampling.LANCZOS)
    widget_img.save(os.path.join(widget_drawable_dir, "ic_app_launcher.png"), "PNG")
    print("Generated feature:widget:impl: ic_app_launcher.png (96x96)")

    print("\nAll launcher icon assets generated successfully!")
    return True

if __name__ == "__main__":

    possible_sources = [
        "D:/定位/app_icon.png",
        "D:/定位/app_icon.jpg",
        "D:/定位/app_icon.jpeg",
        "D:/定位/app_icon.webp",
        os.path.expanduser("~/Desktop/app_icon.png"),
        os.path.expanduser("~/Downloads/app_icon.png")
    ]

    source_path = None
    if len(sys.argv) > 1:
        source_path = sys.argv[1]
    else:
        for p in possible_sources:
            if os.path.exists(p):
                source_path = p
                break

    res_dir = "D:/定位/app/src/main/res"

    if source_path and os.path.exists(source_path):
        process_icon(source_path, res_dir)
    else:
        print("Usage: python scripts/update_icon.py <path_to_image>")
        print("Or place your icon image as 'app_icon.png' in project root (D:/定位/app_icon.png).")
