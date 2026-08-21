#!/usr/bin/env bash
# 版本号一致性自检（对齐姊妹仓库 TaCZ_Refabricated_Unofficial 的同名脚本形态）。
# 默认：只报告，恒退出 0。--strict：发布/合并门禁，不一致退出 1。
# 检查点见 AGENTS.md §1。
set -u
cd "$(dirname "$0")/.."

MODE="${1:-}"
V="$(grep -E '^mod_version=' gradle.properties | head -1 | cut -d= -f2-)"
FAIL=0

echo "gradle.properties mod_version = ${V}"
echo "---"

check_file() {
    local f="$1" min="$2" n
    if [ ! -f "$f" ]; then
        echo "MISSING FILE: $f"
        FAIL=1
        return
    fi
    n="$(grep -cF "$V" "$f" || true)"
    if [ "$n" -lt "$min" ]; then
        echo "INCONSISTENT: $f 中当前版本号出现 ${n} 次（期望 >= ${min}）"
        FAIL=1
    else
        echo "OK: $f（${n} 处）"
    fi
}

# README：顶部版本行 + §1 支持环境表 + §5 版本约束段 => 至少 3 处
check_file README.md 3
# CHANGELOG：当前版本（或未发布段引用）=> 至少 1 处
check_file CHANGELOG.md 1

echo "---"
if [ "$FAIL" -ne 0 ]; then
    echo "结果：不一致。发布/合并前必须修复（AGENTS.md §1）。"
    [ "$MODE" = "--strict" ] && exit 1
else
    echo "结果：一致。"
fi
exit 0
