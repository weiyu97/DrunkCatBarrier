# -*- coding: utf-8 -*-
"""生成自适应图标的前景/背景层"""
from PIL import Image, ImageDraw
import os

SRC = r"D:\workbuddy\DrunkCatBarrier\docs\醉猫结界 Logo 设计\Logo App 图标版.png"
RES = r"D:\workbuddy\DrunkCatBarrier\app\src\main\res"

# 自适应图标各密度尺寸（108dp 画布，前景图形占中间 72dp 安全区）
SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

src = Image.open(SRC).convert("RGBA")

for folder, size in SIZES.items():
    out_dir = os.path.join(RES, folder)
    os.makedirs(out_dir, exist_ok=True)

    # ---- 背景层：纯深紫色 ----
    bg = Image.new("RGBA", (size, size), (26, 8, 48, 255))
    bg.save(os.path.join(out_dir, "ic_launcher_background.png"))

    # ---- 前景层：Logo 缩放到安全区（约 66%），居中，四周留白 ----
    fg_size = int(size * 0.66)
    logo = src.resize((fg_size, fg_size), Image.LANCZOS)
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = (size - fg_size) // 2
    fg.paste(logo, (offset, offset), logo)
    fg.save(os.path.join(out_dir, "ic_launcher_foreground.png"))
    print("生成", folder, size)

print("完成")
