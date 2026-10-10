# -*- coding: utf-8 -*-
"""从单色版裁切 Logo 生成启动器图标 + 通知栏图标"""
from PIL import Image, ImageDraw
import os

SRC = r"D:\workbuddy\DrunkCatBarrier\docs\醉猫结界 Logo 设计\Logo 主标 单色版裁切.png"
RES = r"D:\workbuddy\DrunkCatBarrier\app\src\main\res"

LAUNCHER = {
    "mipmap-mdpi": 48, "mipmap-hdpi": 72, "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192,
}
ADAPTIVE = {
    "mipmap-mdpi": 108, "mipmap-hdpi": 162, "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324, "mipmap-xxxhdpi": 432,
}

src = Image.open(SRC).convert("RGBA")
print("源图:", src.size)

# ---- 启动器图标：深紫底 + 反相单色 Logo ----
# 单色版是黑线白底，放在深紫底上需要反相处理
def invert(img):
    r, g, b, a = img.split()
    inv = Image.merge("RGB", (
        r.point(lambda x: 255 - x),
        g.point(lambda x: 255 - x),
        b.point(lambda x: 255 - x),
    )).convert("RGBA")
    inv.putalpha(a)
    return inv

for folder, size in LAUNCHER.items():
    out_dir = os.path.join(RES, folder)
    os.makedirs(out_dir, exist_ok=True)
    # 深紫圆底
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg = Image.new("RGBA", (size, size), (42, 15, 66, 255))
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    canvas.paste(bg, (0, 0), mask)
    # Logo 反相后居中贴入
    logo = src.resize((int(size * 0.82), int(size * 0.82)), Image.LANCZOS)
    logo = invert(logo)
    off = (size - logo.width) // 2
    canvas.paste(logo, (off, off), logo)
    canvas.save(os.path.join(out_dir, "ic_launcher.png"))

    # 圆形
    round_img = canvas.copy()
    m2 = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m2).ellipse((0, 0, size, size), fill=255)
    round_img.putalpha(m2)
    round_img.save(os.path.join(out_dir, "ic_launcher_round.png"))
    print("launcher", folder, size)

# ---- 自适应图标：背景 + 前景 ----
for folder, size in ADAPTIVE.items():
    out_dir = os.path.join(RES, folder)
    os.makedirs(out_dir, exist_ok=True)
    # 背景：深紫
    bg = Image.new("RGBA", (size, size), (42, 15, 66, 255))
    bg.save(os.path.join(out_dir, "ic_launcher_background.png"))
    # 前景：反相 Logo，占安全区 66%
    fg_size = int(size * 0.66)
    logo = src.resize((fg_size, fg_size), Image.LANCZOS)
    logo = invert(logo)
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    off = (size - fg_size) // 2
    fg.paste(logo, (off, off), logo)
    fg.save(os.path.join(out_dir, "ic_launcher_foreground.png"))
    print("adaptive", folder, size)

# ---- 通知栏小图标：白色猫头剪影 ----
for folder, size in [("drawable-mdpi", 24), ("drawable-hdpi", 36),
                     ("drawable-xhdpi", 48), ("drawable-xxhdpi", 72),
                     ("drawable-xxxhdpi", 96)]:
    out_dir = os.path.join(RES, folder)
    os.makedirs(out_dir, exist_ok=True)
    img = src.resize((size, size), Image.LANCZOS)
    # 转白色剪影：alpha 来自原图亮度反相
    r, g, b, a = img.split()
    white = Image.new("RGBA", img.size, (255, 255, 255, 0))
    white.putalpha(a)
    white.save(os.path.join(out_dir, "ic_notification.png"))
    print("notif", folder, size)

print("完成")
