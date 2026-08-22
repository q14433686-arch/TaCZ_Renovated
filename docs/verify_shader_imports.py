#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
校验所有自定义 shader 的 #moj_import 目标在目标 MC 版本里真实存在。

背景（姊妹项目 1.21.11 移植第 5 号运行期故障）：
    scope_body.vsh 是从 26.2 逐字节抄来的 entity.vsh，里面 import 了
    <minecraft:sample_lightmap.glsl>。这个 include 是 26.x 才有的，
    1.21.11 根本没有。ShaderManager 解析 import 时 Map.get() 返回 null，
    抛 NPE -> "Caught error loading resourcepacks, removing all selected
    resourcepacks" -> 资源重载失败 -> 黑屏。

    编译期查不出来（GLSL 不参与 javac），mixin 校验也查不出来（不是 mixin）。
    只能靠这个脚本。

用法（仓库根目录，需先跑过 ./gradlew help 以填充 MDG 缓存）：
    python3 docs/verify_shader_imports.py
退出码 0 = 全部 OK，1 = 存在悬空 import，2 = 找不到 vanilla jar。

移植自姊妹项目 TaCZ_Refabricated_Unofficial 1.21.11 分支同名脚本（GPL-3.0，
同作者族）；jar 发现逻辑改为 ModDevGradle 缓存 + 内容校验。
"""
import io
import os
import re
import sys
import glob
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IMPORT_RE = re.compile(r'^\s*#moj_import\s*<\s*([a-z0-9_.-]+)\s*:\s*([^>\s]+)\s*>', re.M)


def find_vanilla_jar():
    cands = [a.split('=', 1)[1] for a in sys.argv if a.startswith('--jar=')]
    cands += glob.glob(os.path.expanduser(
        '~/.gradle/caches/moddev*/**/*1.21.11*.jar'), recursive=True)
    cands += glob.glob(os.path.expanduser(
        '~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/'
        'minecraft-merged/1.21.11*/*.jar'))
    cands += glob.glob(os.path.join(REPO, 'build', '**', '*.jar'), recursive=True)
    seen = set()
    for p in cands:
        if p in seen or not p.endswith('.jar'):
            continue
        seen.add(p)
        try:
            with zipfile.ZipFile(p) as z:
                names = set(z.namelist())
                if ('net/minecraft/client/Minecraft.class' in names
                        and 'assets/minecraft/shaders/include/fog.glsl' in names):
                    return p
        except Exception:
            continue
    return None


def collect_vanilla_includes(jar):
    """原版提供的 include：assets/<ns>/shaders/include/<file>"""
    out = set()
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            m = re.match(r'assets/([^/]+)/shaders/include/(.+)$', n)
            if m and not n.endswith('/'):
                out.add((m.group(1), m.group(2)))
    return out


def collect_mod_includes():
    """本模组自己提供的 include（如果有）。"""
    out = set()
    root = os.path.join(REPO, "src", "main", "resources", "assets")
    for ns in os.listdir(root) if os.path.isdir(root) else []:
        inc = os.path.join(root, ns, "shaders", "include")
        for dirpath, _, files in os.walk(inc):
            for f in files:
                rel = os.path.relpath(os.path.join(dirpath, f), inc)
                out.add((ns, rel.replace(os.sep, "/")))
    return out


def mod_shader_files():
    """待检查的 shader：源码树 + 资源 bundle jar 里的。"""
    items = []  # (label, text)
    root = os.path.join(REPO, "src", "main", "resources", "assets")
    for dirpath, _, files in os.walk(root):
        if os.sep + "shaders" + os.sep not in dirpath + os.sep:
            continue
        for f in files:
            if f.rsplit(".", 1)[-1] in ("vsh", "fsh", "glsl", "csh"):
                p = os.path.join(dirpath, f)
                items.append((os.path.relpath(p, REPO),
                              io.open(p, encoding="utf-8", errors="replace").read()))
    return items


def main():
    jar = find_vanilla_jar()
    if not jar:
        print("!! 找不到 1.21.11 vanilla jar。先跑: ./gradlew help --no-daemon")
        return 2
    print("vanilla jar: %s" % os.path.basename(jar))

    available = collect_vanilla_includes(jar) | collect_mod_includes()
    print("可用 include: %d 个" % len(available))
    for ns, f in sorted(available):
        print("    %s:%s" % (ns, f))

    files = mod_shader_files()

    print("\n检查 %d 个 shader 文件的 import ..." % len(files))
    bad, checked = [], 0
    for label, text in files:
        hits = IMPORT_RE.findall(text)
        checked += len(hits)
        for ns, fname in hits:
            if (ns, fname) not in available:
                bad.append((label, ns, fname))

    print("共检查 %d 条 #moj_import。" % checked)

    if not bad:
        print("\n所有会进入成品 jar 的 import 均存在 ✓")
        return 0

    print("\n!! %d 条悬空 import（会导致 ShaderManager NPE / 资源重载失败 / 黑屏）:" % len(bad))
    for label, ns, fname in bad:
        print("  %-70s -> <%s:%s>" % (label, ns, fname))
    return 1


if __name__ == "__main__":
    sys.exit(main())
