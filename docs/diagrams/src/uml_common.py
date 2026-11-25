from xml.sax.saxutils import escape as _esc
import math

FONT = "Segoe UI, Arial, sans-serif"
INK = "#2b2b2b"
OLIVE = "#5b7a1e"
DARK = "#2f3e12"


def esc(s):
    return _esc(str(s))


class SVG:
    def __init__(self, w, h, title=None, subtitle=None):
        self.w, self.h = w, h
        self.p = []
        self.add(f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" font-family="{FONT}">')
        self.add('''<defs>
<marker id="fill" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="#2b2b2b"/></marker>
<marker id="open" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="10" markerHeight="10" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10" fill="none" stroke="#2b2b2b" stroke-width="1.4"/></marker>
<marker id="tri" viewBox="0 0 12 12" refX="12" refY="6" markerWidth="14" markerHeight="14" orient="auto-start-reverse"><path d="M0,0 L12,6 L0,12 z" fill="#fff" stroke="#2b2b2b" stroke-width="1.3"/></marker>
<filter id="sh" x="-5%" y="-5%" width="110%" height="115%"><feDropShadow dx="0" dy="1.5" stdDeviation="1.6" flood-opacity="0.15"/></filter>
</defs>''')
        self.add(f'<rect width="{w}" height="{h}" fill="#fff"/>')
        if title:
            self.add(f'<text x="30" y="42" font-size="26" font-weight="700" fill="{DARK}">{esc(title)}</text>')
        if subtitle:
            self.add(f'<text x="30" y="68" font-size="14" fill="#666">{esc(subtitle)}</text>')

    def add(self, s):
        self.p.append(s)

    def text(self, x, y, s, size=13, anchor="start", weight="400", fill=INK, italic=False, family=None):
        st = ' font-style="italic"' if italic else ""
        fam = f' font-family="{family}"' if family else ""
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" text-anchor="{anchor}" font-weight="{weight}" fill="{fill}"{st}{fam}>{esc(s)}</text>')

    def line(self, pts, dash=False, start=None, end=None, color=INK, width=1.5):
        d = "M" + " L".join(f"{x:.1f},{y:.1f}" for x, y in pts)
        da = ' stroke-dasharray="7 5"' if dash else ""
        ms = f' marker-start="url(#{start})"' if start else ""
        me = f' marker-end="url(#{end})"' if end else ""
        self.add(f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{width}"{da}{ms}{me}/>')

    def save(self, path):
        self.add("</svg>")
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(self.p))


def wrap(s, n):
    if "\n" in s:
        return s.split("\n")
    words, lines, cur = s.split(" "), [], ""
    for w in words:
        if cur and len(cur) + 1 + len(w) > n:
            lines.append(cur)
            cur = w
        else:
            cur = (cur + " " + w).strip()
    lines.append(cur)
    return lines


def diamond(svg, pt, direction, filled=True):
    """Draw a composition diamond whose tip touches pt; direction = unit vector from pt along the line."""
    x, y = pt
    dx, dy = direction
    L, Wd = 18, 7
    tip = (x, y)
    mid = (x + dx * L / 2, y + dy * L / 2)
    back = (x + dx * L, y + dy * L)
    p1 = (mid[0] - dy * Wd, mid[1] + dx * Wd)
    p2 = (mid[0] + dy * Wd, mid[1] - dx * Wd)
    pts = " ".join(f"{a:.1f},{b:.1f}" for a, b in (tip, p1, back, p2))
    svg.add(f'<polygon points="{pts}" fill="{"#2b2b2b" if filled else "#fff"}" stroke="#2b2b2b" stroke-width="1.3"/>')
    return back
