#!/usr/bin/env python3
"""
Verify mixin targets AND handler parameter lists against the real 1.21.11 jar.

Ported from the sister project TaCZ_Refabricated_Unofficial (1.21.11 branch,
docs/verify_mixin_targets.py, GPL-3.0, same author family). Adapted for this
NeoForge repo: mixin configs are read from src/main/templates/META-INF/
neoforge.mods.toml ([[mixins]] config=...) instead of fabric.mod.json, and the
vanilla jar is discovered from ModDevGradle caches instead of Loom's.

Why this exists: the mixin AP only *warns* for targets without a descriptor, so
a wrong name (pickBlockOrEntity) or a wrong parameter list (renderItemInHand)
compiles cleanly and only explodes at launch. Both classes of bug have already
bitten the sister port once each.

Checks:
  1. method = "name"                    -> name exists on target (incl. inherited)
  2. method = "name(desc)"              -> that exact descriptor exists
  3. @At(target = "Lowner;name(desc)")  -> invoked member exists on owner
  4. @Inject handler parameters         -> leading params match the target method's
                                           own parameters (the usual crash)

Run from the repo root AFTER a successful `./gradlew help` (fills MDG caches):
    python3 docs/verify_mixin_targets.py
    python3 docs/verify_mixin_targets.py --jar /path/to/1.21.11-joined.jar
"""
import re, json, os, subprocess, glob, sys, zipfile

JAVAP = None
for cand in [os.environ.get('JAVAP'), 'javap',
             '/usr/lib/jvm/java-21-openjdk-amd64/bin/javap']:
    if cand and os.path.isfile(cand) or cand == 'javap':
        try:
            subprocess.run([cand, '-version'], capture_output=True, timeout=20)
            JAVAP = cand
            break
        except Exception:
            continue
if JAVAP is None:
    sys.exit('javap not found - install a JDK (java-21) and re-run')

SRC = 'src/main/java'
VANILLA = ('net.minecraft', 'com.mojang')

_sig = {}
_params = {}


def find_jar():
    """Locate a 1.21.11 jar that carries vanilla classes + shader includes."""
    explicit = [a.split('=', 1)[1] for a in sys.argv if a.startswith('--jar=')]
    cands = list(explicit)
    cands += glob.glob(os.path.expanduser(
        '~/.gradle/caches/moddev*/**/*1.21.11*.jar'), recursive=True)
    cands += glob.glob(os.path.expanduser(
        '~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/'
        'minecraft-merged/1.21.11*/*.jar'))
    cands += glob.glob('build/**/*1.21.11*.jar', recursive=True)
    cands += glob.glob('build/neoform/**/*.jar', recursive=True)
    seen = set()
    for c in cands:
        if c in seen or not c.endswith('.jar'):
            continue
        seen.add(c)
        try:
            with zipfile.ZipFile(c) as z:
                names = set(z.namelist())
                if ('net/minecraft/client/Minecraft.class' in names
                        and 'assets/minecraft/shaders/include/fog.glsl' in names):
                    return c
        except Exception:
            continue
    sys.exit('1.21.11 vanilla jar not found - run `./gradlew help` first, or '
             'pass --jar=/path/to/jar')


JAR = find_jar()
print('vanilla jar: %s' % JAR)


def _run(cls):
    try:
        return subprocess.run([JAVAP, '-p', '-s', '-classpath', JAR, cls],
                              capture_output=True, text=True, timeout=180).stdout
    except Exception:
        return ''


def _own(cls):
    out = _run(cls)
    names, full, simple = set(), set(), cls.split('.')[-1].split('$')[-1]
    params, cur, sup = {}, None, None
    for line in out.splitlines():
        if sup is None:
            h = re.match(r'\s*(?:public |final |abstract )*class\s+[\w.$]+\s+extends\s+([\w.$]+)', line)
            if h:
                sup = h.group(1)
        d = re.match(r'\s*descriptor:\s*(\S+)', line)
        if d and cur:
            full.add(cur + d.group(1))
            params.setdefault(cur, []).append(d.group(1))
            cur = None
            continue
        m = re.search(r'([\w$<>]+)\s*\(([^)]*)\)\s*(?:throws [\w.,\s]+)?;\s*$', line)
        if m:
            n = m.group(1)
            if n == simple or n == cls:
                n = '<init>'
            names.add(n)
            cur = n
    return names, full, params, bool(out.strip()), sup


def sigs_of(cls):
    if cls in _sig:
        return _sig[cls]
    names, full, params, ok, sup = _own(cls)
    seen = {cls}
    while sup and sup.startswith(VANILLA) and sup not in seen:
        seen.add(sup)
        n2, f2, p2, ok2, sup = _own(sup)
        if not ok2:
            break
        names |= n2
        full |= f2
        for k, v in p2.items():
            params.setdefault(k, []).extend(v)
    _sig[cls] = (names, full, ok)
    _params[cls] = params
    return _sig[cls]


def split_desc(desc):
    """'(FZLorg/joml/Matrix4f;)V' -> ['F','Z','Lorg/joml/Matrix4f;']"""
    inner = desc[1:desc.rindex(')')]
    out, i = [], 0
    while i < len(inner):
        c = inner[i]
        if c == 'L':
            j = inner.index(';', i)
            out.append(inner[i:j + 1])
            i = j + 1
        elif c == '[':
            j = i
            while inner[j] == '[':
                j += 1
            if inner[j] == 'L':
                j = inner.index(';', j)
            out.append(inner[i:j + 1])
            i = j + 1
        else:
            out.append(c)
            i += 1
    return out


JTYPE = {'float': 'F', 'boolean': 'Z', 'int': 'I', 'double': 'D',
         'long': 'J', 'short': 'S', 'byte': 'B', 'char': 'C'}


def java_params(arglist, imports):
    """Parse a handler's Java parameter list into JVM-ish tokens."""
    out = []
    for a in [x.strip() for x in arglist.split(',') if x.strip()]:
        a = re.sub(r'@\w+(\([^)]*\))?\s*', '', a).strip()   # drop annotations
        a = re.sub(r'\s*<[^>]*>', '', a)                     # drop generics
        parts = a.split()
        if len(parts) < 2:
            continue
        t = parts[-2]
        if t in JTYPE:
            out.append(JTYPE[t])
        elif t in imports:
            out.append(imports[t])
        elif '.' in t:
            # nested type written as Outer.Inner -> Louter$Inner;
            outer, inner = t.split('.', 1)
            if outer in imports:
                out.append(imports[outer][:-1] + '$' + inner.replace('.', '$') + ';')
            else:
                out.append(t)
        else:
            out.append(t)
    return out


def mixin_configs():
    """[[mixins]] config=... entries from the mods.toml template."""
    toml = 'src/main/templates/META-INF/neoforge.mods.toml'
    s = open(toml, encoding='utf-8').read()
    return re.findall(r'\[\[mixins\]\]\s*\n\s*config\s*=\s*"([^"]+)"', s)


def main():
    problems, checked = [], 0

    for cfg in mixin_configs():
        d = json.load(open('src/main/resources/' + cfg))
        pkg = d.get('package', '').replace('.', '/')
        for key in ('mixins', 'client', 'server'):
            for entry in d.get(key, []):
                path = f'{SRC}/{pkg}/{entry.replace(".", "/")}.java'
                if not os.path.exists(path):
                    continue
                s = open(path, encoding='utf-8').read()
                base = os.path.basename(path)
                imports = {m.split('.')[-1]: 'L' + m.replace('.', '/') + ';'
                           for m in re.findall(r'^import\s+([\w.]+);', s, re.M)}
                mm = re.search(r'@Mixin\(\s*(?:value\s*=\s*)?(?:targets\s*=\s*")?([\w.$]+)', s)
                if not mm:
                    continue
                decl = mm.group(1)
                if decl.endswith('.class'):
                    decl = decl[:-6]
                cls = decl if '.' in decl else None
                if cls is None:
                    im = re.search(r'^import\s+([\w.]*\.' + re.escape(decl) + r');', s, re.M)
                    cls = im.group(1) if im else None

                if cls and cls.startswith(VANILLA):
                    names, full, ok = sigs_of(cls)
                    if ok:
                        for t in re.findall(r'method\s*=\s*"([^"]+)"', s):
                            # lambda$xxx$N 是【非混淆】版本(26.x)下 javac 的合成名。
                            # 1.21.11 是混淆版本，这类 lambda 在 intermediary 里有正式的
                            # method_NNNNN 名，refmap 里没有 lambda$ 条目——写 lambda$ 名
                            # 必然找不到目标。直接判定为错误。
                            if t.startswith('lambda$'):
                                checked += 1
                                problems.append((
                                    base, 'LAMBDA', cls,
                                    f'{t} —— 混淆版本下不存在此合成名，'
                                    f'需改用 intermediary 的 method_NNNNN(+描述符)'))
                                continue
                            checked += 1
                            bare = t.split('(')[0]
                            if bare not in names:
                                problems.append((base, 'NAME', cls, t))
                            elif '(' in t and (bare + t[t.index('('):]) not in full:
                                problems.append((base, 'DESC', cls, t))

                        # ---- 4: handler parameter lists ----
                        for m in re.finditer(
                                r'@Inject\s*\(([^)]*(?:\([^)]*\)[^)]*)*)\)\s*'
                                r'(?:@\w+\s*)*private\s+\w[\w<>\[\].]*\s+(\w+)\s*\(([^)]*)\)', s):
                            ann, hname, args = m.group(1), m.group(2), m.group(3)
                            tm = re.search(r'method\s*=\s*"([^"]+)"', ann)
                            if not tm:
                                continue
                            bare = tm.group(1).split('(')[0]
                            cands = _params.get(cls, {}).get(bare, [])
                            if len(cands) != 1:
                                continue     # overloaded or unknown - skip
                            want = split_desc(cands[0])
                            got = java_params(args, imports)
                            got = [g for g in got
                                   if 'CallbackInfo' not in g and 'LocalRef' not in g]
                            if len(got) < len(want):
                                continue     # partial capture is legal
                            checked += 1
                            if got[:len(want)] != want:
                                problems.append((base, 'PARAMS', cls,
                                                 f'{hname}: want {want} got {got[:len(want)]}'))

                for tgt in re.findall(r'target\s*=\s*"(L[^"]+)"', s):
                    t2 = re.match(r'L([\w/$]+);([\w$<>]+)(\(.*)', tgt)
                    if not t2:
                        continue
                    owner = t2.group(1).replace('/', '.')
                    if not owner.startswith(VANILLA):
                        continue
                    names, full, ok = sigs_of(owner)
                    if not ok:
                        continue
                    checked += 1
                    if t2.group(2) not in names:
                        problems.append((base, 'AT-NAME', owner, t2.group(2) + t2.group(3)))
                    elif (t2.group(2) + t2.group(3)) not in full:
                        problems.append((base, 'AT-DESC', owner, t2.group(2) + t2.group(3)))

    print(f'checked {checked} items across {len(_sig)} vanilla classes\n')
    if not problems:
        print('OK - all targets and handler signatures resolve against 1.21.11')
        return 0
    print(f'!! {len(problems)} PROBLEMS:\n')
    for f, kind, c, t in problems:
        print(f'  [{kind:7s}] {f:36s} {c.split(".")[-1]:24s} {t}')
    return 1


if __name__ == '__main__':
    sys.exit(main())
