#!/usr/bin/env bash
# 收尾用 CHANGELOG 条目草稿生成器。
#
# 用法：
#   bash scripts/generate_changelog.sh [起点ref] [--version 版本串] [--out 文件]
#
# 行为：
#   - 取 起点ref..HEAD 的非 merge 提交，剔除 ci-log 回推提交（那是编译日志写回，
#     不是人的改动），按 conventional-commit 前缀分组成 markdown 条目草稿；
#   - 起点ref 省略时依次回退：与主干 26.2 的分叉点 → 最近的 tag → 根提交；
#   - 只做归类，不做判断：机制描述、实机状态与「谁在什么时候验过」必须人工补。
#     产出是草稿，不是可以直接发版的条目。
set -u
cd "$(dirname "$0")/.."

SINCE=""
VERSION=""
OUT=""

usage() {
    cat <<'EOF'
用法: generate_changelog.sh [起点ref] [--version 版本串] [--out 文件]
  --version  写进标题的版本串，例如 1.1.8+neoforge.26.2.R2
  --out      输出文件；省略时打到 stdout
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --version) VERSION="${2:-}"; shift 2 ;;
        --out)     OUT="${2:-}"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *)         SINCE="$1"; shift ;;
    esac
done

# 默认起点：分叉点 → 最近 tag → 根提交
if [ -z "$SINCE" ]; then
    if git rev-parse --verify -q origin/26.2 >/dev/null 2>&1; then
        SINCE="$(git merge-base HEAD origin/26.2)"
    elif git describe --tags --abbrev=0 >/dev/null 2>&1; then
        SINCE="$(git describe --tags --abbrev=0)"
    else
        SINCE="$(git rev-list --max-parents=0 HEAD | tail -1)"
    fi
fi
git rev-parse --verify -q "$SINCE^{commit}" >/dev/null || { echo "起点 ref 不存在: $SINCE" >&2; exit 1; }
SINCE="$(git rev-parse "$SINCE^{commit}")"

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

# hash<TAB>subject，新→旧；剔除 merge 与 ci-log 回推
git log --no-merges --format='%H%x09%s' "$SINCE..HEAD" > "$TMP.all"
awk -F'\t' '$2 !~ /^ci-log:/' "$TMP.all" > "$TMP"

TOTAL_ALL=$(wc -l < "$TMP.all" | tr -d ' ')
TOTAL=$(wc -l < "$TMP" | tr -d ' ')
SKIPPED=$((TOTAL_ALL - TOTAL))

# 已知前缀（conventional-commit 的 type 部分）。「其它」是这些之外的兜底，
# 不是全部再列一遍 —— 用 !~ 取补集，否则每笔提交都会重复出现在兜底组里。
KNOWN='^(feat|fix|sync|perf|refactor|docs|ci|build|chore|test|style|revert)'

emit() {  # $1=小节标题 $2=awk 正则 $3=1 时取不匹配的行
    local label="$1" re="$2" neg="${3:-0}" body n
    if [ "$neg" = "1" ]; then
        body="$(awk -F'\t' -v re="$re" '$2 !~ re { printf "- %s (`%s`)\n", $2, substr($1,1,7) }' "$TMP")"
    else
        body="$(awk -F'\t' -v re="$re" '$2 ~ re { printf "- %s (`%s`)\n", $2, substr($1,1,7) }' "$TMP")"
    fi
    n="$(printf '%s' "$body" | grep -c '^- ' || true)"
    [ "$n" -eq 0 ] && return 0
    printf '\n### %s\n\n%s' "$label" "$body"
}

{
    echo "<!-- 由 scripts/generate_changelog.sh 生成：$(date +%F) · 范围 $(git rev-parse --short "$SINCE")..HEAD"
    echo "     共 ${TOTAL} 笔（已剔除 ci-log 回推 ${SKIPPED} 笔）。草稿：机制描述与实机状态需人工补。 -->"
    echo
    if [ -n "$VERSION" ]; then
        echo "## ${VERSION} — $(date +%F)"
    else
        echo "## $(date +%F) 条目草稿"
    fi
    emit "新增"   '^feat'
    emit "修复"   '^fix'
    emit "同步"   '^sync'
    emit "性能"   '^perf'
    emit "重构"   '^refactor'
    emit "文档"   '^docs'
    emit "工程"   '^(ci|build|chore|test|style)'
    emit "回退"   '^revert'
    emit "其它"   "$KNOWN" 1
    cat <<EOF

---

范围：\`$(git rev-parse --short "$SINCE")..HEAD\`（\`git log --no-merges $SINCE..HEAD\`）。
本文件是**草稿**：只按提交前缀归类，不判断机制描述是否准确，也不含实机验证状态。
并入 \`CHANGELOG.md\` 前必须逐条人工核对，并补上「谁在什么时候验过 / 未实机」。
EOF
} > "$TMP.out"

if [ -n "$OUT" ]; then
    cp "$TMP.out" "$OUT"
    echo "已生成 $OUT（${TOTAL} 笔提交，剔除 ci-log ${SKIPPED} 笔）" >&2
else
    cat "$TMP.out"
fi
