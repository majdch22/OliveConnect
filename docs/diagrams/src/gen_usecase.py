import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uml_common import SVG, wrap, esc, OLIVE, DARK, INK

OUT = sys.argv[1]

ACT_COL = {"primary": "#5b7a1e", "secondary": "#777"}


class UC:
    def __init__(self, w, h, title, subtitle, boundary, btitle="Système OliveConnect"):
        self.s = SVG(w, h, title, subtitle)
        self.nodes = {}
        self.edges = []
        self.boundary = boundary
        self.btitle = btitle

    def actor(self, id, x, y, label, kind="primary"):
        self.nodes[id] = ("actor", x, y, label, kind)

    def uc(self, id, x, y, label, rx=128, ry=34, code=None, secondary=False):
        self.nodes[id] = ("uc", x, y, label, (rx, ry, code, secondary))

    def pkg(self, id, x, y, label, w=300, h=78, sub=""):
        self.nodes[id] = ("pkg", x, y, label, (w, h, sub))

    def e(self, a, b, kind="assoc", label=None):
        self.edges.append((a, b, kind, label))

    def _clip(self, nid, tx, ty):
        t, x, y, label, extra = self.nodes[nid]
        dx, dy = tx - x, ty - y
        if dx == 0 and dy == 0:
            return x, y
        if t == "uc":
            rx, ry = extra[0], extra[1]
            s = 1 / math.sqrt((dx / rx) ** 2 + (dy / ry) ** 2)
            return x + dx * s, y + dy * s
        if t == "actor":
            hw, hh = 40, 74
            cy = y + 18
            dy2 = ty - cy
            s = min(hw / abs(dx) if dx else 1e9, hh / abs(dy2) if dy2 else 1e9)
            return x + dx * s, cy + dy2 * s
        w, h = extra[0], extra[1]
        s = min((w / 2) / abs(dx) if dx else 1e9, (h / 2) / abs(dy) if dy else 1e9)
        return x + dx * s, y + dy * s

    def render(self, path):
        s = self.s
        bx, by, bw, bh = self.boundary
        s.add(f'<rect x="{bx}" y="{by}" width="{bw}" height="{bh}" rx="14" fill="#f8faf3" stroke="{OLIVE}" stroke-width="2"/>')
        s.text(bx + 18, by + 28, self.btitle, 16, weight="700", fill=DARK)
        # edges first
        for a, b, kind, label in self.edges:
            na, nb = self.nodes[a], self.nodes[b]
            p1 = self._clip(a, nb[1], nb[2] + (4 if nb[0] == "actor" else 0))
            p2 = self._clip(b, na[1], na[2] + (4 if na[0] == "actor" else 0))
            if kind == "assoc":
                s.line([p1, p2], width=1.4)
            elif kind in ("include", "extend"):
                s.line([p1, p2], dash=True, end="open", width=1.3)
                mx, my = (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2
                txt = label or f"«{kind}»"
                s.add(f'<rect x="{mx-34:.1f}" y="{my-11:.1f}" width="68" height="16" fill="#fff" opacity="0.9"/>')
                s.text(mx, my + 1, txt, 11.5, "middle", italic=True, fill="#444")
            elif kind == "gen":
                s.line([p1, p2], end="tri", width=1.4)
        for nid, (t, x, y, label, extra) in self.nodes.items():
            if t == "actor":
                col = ACT_COL[extra]
                s.add(f'<circle cx="{x}" cy="{y-36}" r="12" fill="#fff" stroke="{col}" stroke-width="2.2"/>')
                s.add(f'<path d="M{x},{y-24} V{y+8} M{x-22},{y-14} H{x+22} M{x},{y+8} L{x-17},{y+36} M{x},{y+8} L{x+17},{y+36}" stroke="{col}" stroke-width="2.2" fill="none" stroke-linecap="round"/>')
                for i, ln in enumerate(wrap(label, 18)):
                    s.text(x, y + 56 + i * 16, ln, 13.5, "middle", "700", col if extra == "secondary" else INK)
            elif t == "uc":
                rx, ry, code, sec = extra
                fill = "#ffffff" if not sec else "#f3f3f3"
                stroke = OLIVE if not sec else "#888"
                s.add(f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{ry}" fill="{fill}" stroke="{stroke}" stroke-width="1.8" filter="url(#sh)"/>')
                lines = wrap(label, 26)
                y0 = y - (len(lines) - 1) * 8 + 4
                if code:
                    y0 += 5
                    s.text(x, y - ry + 14, code, 10.5, "middle", "700", OLIVE)
                for i, ln in enumerate(lines):
                    s.text(x, y0 + i * 16, ln, 12.5, "middle")
            else:
                w, h, sub = extra
                xx, yy = x - w / 2, y - h / 2
                s.add(f'<path d="M{xx},{yy} h{w*0.38} l8,-12 h-{w*0.38 - 8} z" fill="#e8f0d8" stroke="{OLIVE}" stroke-width="1.6"/>')
                s.add(f'<rect x="{xx}" y="{yy}" width="{w}" height="{h}" rx="4" fill="#fff" stroke="{OLIVE}" stroke-width="1.8" filter="url(#sh)"/>')
                s.text(x, y - (6 if sub else -4), label, 14, "middle", "700", DARK)
                if sub:
                    s.text(x, y + 15, sub, 11.5, "middle", fill="#666")
        legend(s, 30, self.s.h - 22)
        s.save(path)


def legend(s, x, y):
    s.text(x, y, "Légende :", 13, weight="700")
    s.line([(x + 80, y - 4), (x + 130, y - 4)], width=1.4)
    s.text(x + 138, y, "association acteur ↔ cas", 12.5)
    s.line([(x + 320, y - 4), (x + 370, y - 4)], dash=True, end="open", width=1.3)
    s.text(x + 378, y, "«include» / «extend»", 12.5)
    s.line([(x + 540, y - 4), (x + 590, y - 4)], end="tri", width=1.4)
    s.text(x + 598, y, "généralisation", 12.5)
    s.add(f'<ellipse cx="{x+760}" cy="{y-4}" rx="30" ry="11" fill="#f3f3f3" stroke="#888" stroke-width="1.5"/>')
    s.text(x + 798, y, "cas inclus / d'extension", 12.5)
    s.add(f'<path d="M{x+990},{y-20} m0,0" />')
    s.text(x + 990, y, "Acteur gris = acteur secondaire", 12.5, fill="#777")


# ---------------------------------------------------------------- 0. Global
d = UC(1950, 1080, "Diagramme de cas d'utilisation — vue globale (paquetages)",
       "Chaque paquetage regroupe les cas détaillés dans les diagrammes suivants", (470, 100, 1160, 900))
L, R = 700, 1300
for i, (pid, lab, sub) in enumerate([("p_acc", "Accès & information publique", "UC01–UC07"),
                                      ("p_ver", "Vergers & récoltes", "UC10–UC14"),
                                      ("p_ale", "Alertes terrain", "UC13, UC22"),
                                      ("p_exe", "Exécution des tournées", "UC30–UC34"),
                                      ("p_rec", "Réception au pressoir", "UC50–UC52")]):
    d.pkg(pid, L, 200 + i * 160, lab, sub=sub)
for pid, lab, sub, y in [("p_pla", "Planification des collectes", "UC20–UC21", 200),
                         ("p_tdb", "Carte & tableau de bord", "UC23, UC52, UC64", 360),
                         ("p_res", "Gestion des ressources", "UC40–UC42", 680),
                         ("p_adm", "Administration & paramétrage", "UC60–UC64", 840),
                         ("p_imp", "Imports de données", "UC70–UC71", 980 - 140 + 140 - 0)]:
    d.pkg(pid, R, y, lab, sub=sub)
d.nodes["p_imp"] = ("pkg", R, 940, "Imports de données", (300, 78, "UC70–UC71"))
d.nodes["p_adm"] = ("pkg", R, 800, "Administration & paramétrage", (300, 78, "UC60–UC64"))
d.nodes["p_res"] = ("pkg", R, 660, "Gestion des ressources", (300, 78, "UC40–UC42"))
d.actor("vis", 180, 190, "Visiteur")
d.actor("agr", 180, 420, "Agriculteur")
d.actor("chef", 180, 690, "Chef d'équipe de récolte")
d.actor("pre", 180, 880, "Responsable du pressoir")
d.actor("col", 1780, 440, "Responsable de collecte")
d.actor("log", 1780, 650, "Responsable logistique")
d.actor("adm", 1780, 830, "Administrateur")
d.actor("ext", 1780, 1000 - 40, "Systèmes externes (simulés)", "secondary")
d.nodes["ext"] = ("actor", 1780, 985, "Systèmes externes (simulés)", "secondary")
for a, b in [("vis", "p_acc"), ("agr", "p_acc"), ("agr", "p_ver"), ("agr", "p_ale"), ("chef", "p_exe"), ("pre", "p_rec"),
             ("col", "p_pla"), ("col", "p_tdb"), ("col", "p_ale"), ("log", "p_res"), ("adm", "p_adm"), ("adm", "p_imp"),
             ("ext", "p_imp")]:
    d.e(a, b)
d.render(f"{OUT}/uc-0-global.svg")

# ---------------------------------------------------------------- 1. Commun
d = UC(1820, 1300, "Diagramme de cas d'utilisation — visiteur et fonctions communes",
       "Généralisation des acteurs : tout profil authentifié hérite des cas communs", (640, 100, 1140, 1060))
C1, C2 = 930, 1480
d.actor("vis", 440, 270, "Visiteur")
d.uc("u01", C1, 180, "Consulter la carte publique des vergers", code="UC01")
d.uc("u02", C1, 290, "Consulter les informations générales", code="UC02")
d.uc("u03", C1, 400, "Demander une inscription", code="UC03")
d.uc("x01", C2, 180, "Filtrer par variété / maturité", secondary=True)
d.uc("x03", C2, 400, "Déclarer un premier verger", secondary=True)
d.uc("x03b", C2, 510, "Accepter les conditions d'utilisation", secondary=True)
for u in ("u01", "u02", "u03"):
    d.e("vis", u)
d.e("x01", "u01", "extend")
d.e("x03", "u03", "extend")
d.e("u03", "x03b", "include")
d.actor("aut", 440, 820, "Utilisateur authentifié")
d.uc("u04", C1, 660, "S'authentifier", code="UC04")
d.uc("u05", C1, 790, "Consulter ses notifications", code="UC05")
d.uc("u06", C1, 920, "Consulter l'aide / visite guidée", code="UC06")
d.uc("u07", C1, 1050, "Gérer son profil", code="UC07")
d.uc("x04", C2, 660, "Réinitialiser le mot de passe (SMS)", secondary=True)
d.uc("x05", C2, 790, "Recevoir les notifications en temps réel", secondary=True)
d.uc("x07", C2, 1050, "Changer le mot de passe", secondary=True)
for u in ("u04", "u05", "u06", "u07"):
    d.e("aut", u)
d.e("x04", "u04", "extend")
d.e("u05", "x05", "include")
d.e("x07", "u07", "extend")
kids = [("k1", "Agriculteur", 90, 560), ("k2", "Responsable de collecte", 270, 600), ("k3", "Chef d'équipe", 90, 760),
        ("k4", "Responsable logistique", 270, 1030), ("k5", "Responsable du pressoir", 90, 960), ("k6", "Administrateur", 270, 1150 - 40)]
kids = [(f"k{i}", lab, 110, 450 + i * 145) for i, lab in enumerate(["Agriculteur", "Responsable de collecte", "Chef d'équipe", "Responsable logistique", "Responsable du pressoir", "Administrateur"])]
for k, lab, x, y in kids:
    d.actor(k, x, y, lab)
    d.e(k, "aut", "gen")
d.render(f"{OUT}/uc-1-commun.svg")

# ---------------------------------------------------------------- 2. Agriculteur
d = UC(1700, 980, "Diagramme de cas d'utilisation — agriculteur (adhérent)",
       "Pré-condition commune : être authentifié (UC04) · RG01 : l'agriculteur n'agit que sur ses propres données", (400, 100, 1260, 840))
C1, C2 = 720, 1330
d.actor("agr", 190, 500, "Agriculteur")
d.uc("u10", C1, 190, "Gérer ses vergers et parcelles", code="UC10")
d.uc("u11", C1, 340, "Déclarer une récolte (planifiée / urgente)", code="UC11")
d.uc("u12", C1, 490, "Modifier / annuler une demande", code="UC12")
d.uc("u13", C1, 640, "Signaler maturité ou problème", code="UC13")
d.uc("u14", C1, 790, "Suivre l'avancement de ses collectes", code="UC14")
for u in ("u10", "u11", "u12", "u13", "u14"):
    d.e("agr", u)
d.uc("i10", C2, 190, "Tracer le contour sur la carte", secondary=True)
d.uc("i11", C2, 340, "Vérifier la propriété du verger (RG01)", secondary=True)
d.uc("i12", C2, 490, "Vérifier que la tournée n'a pas démarré (RG03)", secondary=True)
d.uc("i13", C2, 610, "Géolocaliser le verger (GPS)", secondary=True)
d.uc("x13", C2, 720, "Joindre une photo", secondary=True)
d.uc("x14", C2, 840, "Consulter la pesée et la qualité", secondary=True)
d.e("u10", "i10", "include")
d.e("u11", "i11", "include")
d.e("u12", "i12", "include")
d.e("u13", "i13", "include")
d.e("x13", "u13", "extend")
d.e("x14", "u14", "extend")
d.render(f"{OUT}/uc-2-agriculteur.svg")

# ---------------------------------------------------------------- 3. Collecte & terrain
d = UC(1900, 1300, "Diagramme de cas d'utilisation — responsable de collecte et chef d'équipe",
       "Planification et exécution des tournées", (400, 100, 1180, 1160))
C1, C2 = 720, 1290
d.actor("col", 190, 380, "Responsable de collecte")
d.uc("u20", C1, 190, "Valider / rejeter une demande", code="UC20")
d.uc("u21", C1, 320, "Planifier une tournée", code="UC21")
d.uc("u22", C1, 450, "Traiter une alerte", code="UC22")
d.uc("u23", C1, 580, "Consulter la carte et le tableau de bord", code="UC23")
for u in ("u20", "u21", "u22", "u23"):
    d.e("col", u)
d.actor("chef", 190, 930, "Chef d'équipe de récolte")
d.uc("u30", C1, 740, "Consulter ses tournées", code="UC30")
d.uc("u31", C1, 850, "Démarrer une tournée", code="UC31")
d.uc("u32", C1, 960, "Mettre à jour l'avancement et les quantités", code="UC32")
d.uc("u33", C1, 1070, "Mettre à jour le statut du verger", code="UC33")
d.uc("u34", C1, 1180, "Livrer au pressoir", code="UC34")
for u in ("u30", "u31", "u32", "u33", "u34"):
    d.e("chef", u)
d.uc("i21", C2, 320, "Vérifier la disponibilité des ressources (RG04)", secondary=True)
d.uc("i21b", C2, 190, "Saisir le motif du rejet", secondary=True)
d.uc("x22", C2, 530, "Créer une demande urgente", secondary=True)
d.uc("ntf", C2, 760, "Notifier les acteurs concernés", secondary=True)
d.uc("x34", C2, 1180, "Clôturer la tournée", secondary=True)
d.uc("x21", C2, 420, "Annuler une tournée", secondary=True)
d.e("u21", "i21", "include")
d.e("i21b", "u20", "extend")
d.e("x22", "u22", "extend")
d.e("x21", "u21", "extend")
d.e("x34", "u34", "extend")
d.e("u31", "ntf", "include")
d.e("u32", "ntf", "include")
d.e("u34", "ntf", "include")
d.actor("log", 1740, 320, "Responsable logistique", "secondary")
d.actor("agr", 1740, 760, "Agriculteur", "secondary")
d.actor("pre", 1740, 1110, "Responsable du pressoir", "secondary")
d.e("log", "i21")
d.e("agr", "ntf")
d.e("pre", "ntf")
d.render(f"{OUT}/uc-3-collecte-terrain.svg")

# ---------------------------------------------------------------- 4. Logistique, pressoir, admin, systèmes
d = UC(1900, 1440, "Diagramme de cas d'utilisation — logistique, pressoir, administration et systèmes externes",
       "Ressources, réception des olives, exploitation de la plateforme et imports automatiques", (400, 100, 1180, 1300))
C1, C2 = 720, 1290
d.actor("log", 190, 280, "Responsable logistique")
d.uc("u40", C1, 180, "Gérer les ressources", code="UC40")
d.uc("u41", C1, 290, "Consulter le calendrier des ressources", code="UC41")
d.uc("u42", C1, 400, "Gérer les approvisionnements", code="UC42")
for u in ("u40", "u41", "u42"):
    d.e("log", u)
d.uc("x40", C2, 180, "Déclarer une indisponibilité (panne)", secondary=True)
d.e("x40", "u40", "extend")
d.actor("pre", 190, 650, "Responsable du pressoir")
d.uc("u50", C1, 550, "Réceptionner une benne (poids, qualité)", code="UC50")
d.uc("u51", C1, 660, "Publier la capacité de traitement", code="UC51")
d.uc("u52", C1, 770, "Consulter le tableau de bord", code="UC52")
for u in ("u50", "u51", "u52"):
    d.e("pre", u)
d.uc("i50", C2, 520, "Calculer le taux de remplissage", secondary=True)
d.uc("x50", C2, 630, "Clôturer la tournée", secondary=True)
d.e("u50", "i50", "include")
d.e("x50", "u50", "extend")
d.actor("adm", 190, 1100, "Administrateur")
d.uc("u60", C1, 920, "Gérer les comptes et les rôles", code="UC60")
d.uc("u61", C1, 1030, "Valider les inscriptions", code="UC61")
d.uc("u62", C1, 1140, "Paramétrer le système", code="UC62")
d.uc("u63", C1, 1250, "Superviser les imports", code="UC63")
d.uc("u64", C1, 1360 - 10, "Superviser le serveur (sauvegardes, logs)", code="UC64")
for u in ("u60", "u61", "u62", "u63", "u64"):
    d.e("adm", u)
d.uc("u71", C2, 1060, "Importer les données externes (planifié)", code="UC70–71")
d.uc("i71", C2, 1230, "Contrôler la conformité des données", secondary=True)
d.uc("x71", C2, 900, "Générer une alerte de maturité automatique", secondary=True)
d.e("u71", "i71", "include")
d.e("x71", "u71", "extend")
d.actor("met", 1740, 860, "Service météo / capteurs", "secondary")
d.actor("sp", 1740, 1070, "Système du pressoir", "secondary")
d.actor("ref", 1740, 1280, "Référentiel coopérative", "secondary")
for a in ("met", "sp", "ref"):
    d.e(a, "u71")
d.render(f"{OUT}/uc-4-logistique-pressoir-admin.svg")
print("ok")
