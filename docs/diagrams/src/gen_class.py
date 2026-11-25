import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uml_common import SVG, esc, diamond, OLIVE, DARK, INK

OUT = sys.argv[1]
COLX = [40 + i * 430 for i in range(6)]
BW = 360
LH = 17
GAP = 95

# name: (col, row, stereotype, attributes, methods, color)
C = {
    "Traitement": (0, 0, None, ["- date : LocalDateTime", "- auteurId : String", "- commentaire : String"], [], "emb"),
    "Utilisateur": (1, 0, None, ["- id : String", "- nom : String", "- prenom : String", "- email : String", "- telephone : String",
                                 "- motDePasse : String", "- role : Role", "- actif : boolean", "- dateCreation : LocalDateTime"],
                    ["+ activer() : void", "+ desactiver() : void"], "doc"),
    "Notification": (2, 0, None, ["- id : String", "- type : TypeNotification", "- message : String", "- lien : String",
                                  "- lu : boolean", "- date : LocalDateTime"], ["+ marquerLue() : void"], "doc"),
    "Tournee": (3, 0, None, ["- id : String", "- date : LocalDate", "- statut : StatutTournee", "- dateDebut : LocalDateTime",
                             "- dateFin : LocalDateTime"],
                ["+ demarrer() : void", "+ livrer() : void", "+ cloturer() : void", "+ annuler() : void"], "doc"),
    "Pressoir": (4, 0, None, ["- id : String", "- nom : String", "- localisation : GeoJsonPoint", "- capaciteJournaliereKg : double",
                              "- capaciteDisponibleKg : double", "- majCapacite : LocalDateTime"], ["+ reserver(kg : double) : void"], "doc"),
    "Reception": (5, 0, None, ["- id : String", "- poidsKg : double", "- aciditePct : double", "- categorie : CategorieQualite",
                               "- tauxRemplissage : double", "- dateHeure : LocalDateTime"],
                  ["+ calculerTauxRemplissage(capaciteKg) : double"], "doc"),
    "Alerte": (0, 1, None, ["- id : String", "- type : TypeAlerte", "- position : GeoJsonPoint", "- description : String",
                            "- photoUrl : String", "- statut : StatutAlerte", "- dateCreation : LocalDateTime"],
               ["+ prendreEnCharge() : void", "+ resoudre() : void"], "doc"),
    "Agriculteur": (1, 1, None, ["- cin : String", "- adresse : String", "- numeroAdherent : String"], [], "doc"),
    "DemandeRecolte": (2, 1, None, ["- id : String", "- type : TypeRecolte", "- dateSouhaitee : LocalDate", "- quantiteEstimeeKg : double",
                                    "- statut : StatutDemande", "- dateSoumission : LocalDateTime", "- commentaire : String"],
                       ["+ valider() : void", "+ rejeter(motif : String) : void", "+ annuler() : void", "+ estModifiable() : boolean"], "doc"),
    "EtapeTournee": (3, 1, None, ["- ordre : int", "- quantiteEstimeeKg : double", "- quantiteRecolteeKg : double",
                                  "- avancement : int", "- statutVerger : StatutRecolte"], ["+ mettreAJour(kg, pct) : void"], "emb"),
    "Affectation": (4, 1, None, ["- ressourceId : String", "- type : TypeRessource", "- libelle : String"], [], "emb"),
    "Ressource": (5, 1, None, ["- id : String", "- type : TypeRessource", "- nom : String", "- matricule : String",
                               "- capaciteKg : double", "- etat : EtatRessource"], ["+ estDisponible(date : LocalDate) : boolean"], "doc"),
    "Variete": (0, 2, None, ["- id : String", "- nom : String", "- rendementMoyenKgHa : double", "- debutRecolte : MonthDay",
                             "- finRecolte : MonthDay"], [], "doc"),
    "Verger": (1, 2, None, ["- id : String", "- nom : String", "- localisation : GeoJsonPoint", "- contour : GeoJsonPolygon",
                            "- superficieHa : double", "- nbArbres : int", "- niveauMaturite : int", "- statutRecolte : StatutRecolte"],
               ["+ estMur(seuil : int) : boolean"], "doc"),
    "ChangementStatut": (2, 2, None, ["- ancien : String", "- nouveau : String", "- date : LocalDateTime", "- auteurId : String"], [], "emb"),
    "ReleveMeteo": (3, 2, "time-series", ["- timestamp : Instant", "- zone : String", "- temperature : double", "- pluieMm : double",
                                         "- humidite : double", "- vent : double"], [], "ts"),
    "ImportJob": (4, 2, None, ["- id : String", "- source : SourceImport", "- date : LocalDateTime", "- nbOk : int", "- nbRejets : int",
                               "- erreurs : List<String>", "- statut : StatutImport"], [], "doc"),
    "Approvisionnement": (5, 2, None, ["- id : String", "- materiel : String", "- quantite : int", "- fournisseur : String",
                                       "- statut : StatutAppro", "- dateDemande : LocalDate", "- dateLivraison : LocalDate"], [], "doc"),
    "ReleveMaturite": (0, 3, "time-series", ["- timestamp : Instant", "- indiceMaturite : int", "- source : String"], [], "ts"),
    "Parcelle": (1, 3, None, ["- code : String", "- superficieHa : double", "- variete : String", "- anneePlantation : int"], [], "emb"),
}
STYLE = {"doc": ("#e8f0d8", OLIVE), "emb": ("#f1f1f1", "#777"), "ts": ("#e3ecf5", "#3d5a80"), "enum": ("#fbf3df", "#a8741a")}


def height(attrs, meths, stereo):
    h = 34 + (14 if stereo else 0)
    h += 10 + len(attrs) * LH
    if meths:
        h += 10 + len(meths) * LH
    return h + 6


rows = {}
for n, (c, r, st, a, m, k) in C.items():
    rows[r] = max(rows.get(r, 0), height(a, m, st))
ROWY = {0: 130}
for r in range(1, 4):
    ROWY[r] = ROWY[r - 1] + rows[r - 1] + GAP
B = {}
for n, (c, r, st, a, m, k) in C.items():
    B[n] = (COLX[c], ROWY[r], BW, height(a, m, st))

# enumerations
ENUMS = [
    ("Role", ["AGRICULTEUR", "RESP_COLLECTE", "CHEF_EQUIPE", "RESP_LOGISTIQUE", "RESP_PRESSOIR", "ADMIN"]),
    ("StatutDemande", ["SOUMISE", "VALIDEE", "REJETEE", "PLANIFIEE", "EN_COURS", "TERMINEE", "ANNULEE"]),
    ("StatutTournee", ["PLANIFIEE", "EN_COURS", "LIVREE", "CLOTUREE", "ANNULEE"]),
    ("StatutRecolte", ["NON_MUR", "MUR", "EN_RECOLTE", "RECOLTE"]),
    ("StatutAlerte", ["OUVERTE", "PRISE_EN_CHARGE", "RESOLUE"]),
    ("TypeRecolte", ["PLANIFIEE", "URGENTE"]),
    ("TypeAlerte", ["MATURITE", "PLUIE", "RAVAGEUR", "AUTRE"]),
    ("TypeRessource", ["OUVRIER", "TRACTEUR", "BENNE", "MATERIEL"]),
    ("EtatRessource", ["DISPONIBLE", "EN_PANNE", "REVISION", "CONGE"]),
    ("CategorieQualite", ["EXTRA", "VIERGE", "COURANTE", "LAMPANTE"]),
]
EW, EX0 = 226, COLX[2]
enum_y = ROWY[3]
ENUM_POS = []
for i, (n, vals) in enumerate(ENUMS):
    col, row = i % 7, i // 7
    ENUM_POS.append((n, vals, EX0 + col * (EW + 12), enum_y + row * 205))

H = max(ROWY[3] + rows[3], max(y + height(v, [], 1) for _, v, _, y in ENUM_POS)) + 80
W = COLX[5] + BW + 40
s = SVG(W, int(H), "Diagramme de classes — couche Model d'OliveConnect",
        "Entités persistées dans MongoDB (vert = collection, gris = sous-document imbriqué, bleu = collection time-series)")


def box(name, x, y, w, stereo, attrs, meths, kind, title=None):
    bg, fg = STYLE[kind]
    h = height(attrs, meths, stereo)
    s.add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="#fff" stroke="{fg}" stroke-width="1.8" filter="url(#sh)"/>')
    hh = 34 + (14 if stereo else 0)
    s.add(f'<rect x="{x}" y="{y}" width="{w}" height="{hh}" fill="{bg}" stroke="{fg}" stroke-width="1.8"/>')
    ty = y + 22
    if stereo:
        s.text(x + w / 2, y + 16, f"«{stereo}»", 11.5, "middle", italic=True, fill="#555")
        ty += 14
    s.text(x + w / 2, ty, title or name, 15, "middle", "700", DARK)
    cy = y + hh + 6
    for a in attrs:
        cy += LH
        s.text(x + 10, cy - 3, a, 12.5, family="Consolas, monospace")
    cy += 6
    if meths:
        s.add(f'<line x1="{x}" y1="{cy}" x2="{x+w}" y2="{cy}" stroke="{fg}" stroke-width="1.2"/>')
        cy += 4
        for m in meths:
            cy += LH
            s.text(x + 10, cy - 3, m, 12.5, family="Consolas, monospace")


def top(n, fx=0.5):
    x, y, w, h = B[n]; return (x + w * fx, y)


def bot(n, fx=0.5):
    x, y, w, h = B[n]; return (x + w * fx, y + h)


def lft(n, dy):
    x, y, w, h = B[n]; return (x, y + dy)


def rgt(n, dy):
    x, y, w, h = B[n]; return (x + w, y + dy)


def unit(a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    L = max(abs(dx), abs(dy)) or 1
    return (dx / L, dy / L)


def edge(pts, comp=False, gen=False, arrow=False, m1=None, m2=None, label=None, lpos=None):
    """pts[0] = source end (composition diamond / multiplicity m1), pts[-1] = target end (m2)."""
    pts = list(pts)
    if comp:
        d = unit(pts[0], pts[1])
        pts[0] = diamond(s, pts[0], d)
    s.line(pts, end="tri" if gen else ("open" if arrow else None), width=1.5)
    for (txt, a, b) in ((m1, pts[0], pts[1]), (m2, pts[-1], pts[-2])):
        if not txt:
            continue
        if txt == m1 and comp:
            a = (a[0] - unit(pts[0], pts[1])[0] * 18, a[1] - unit(pts[0], pts[1])[1] * 18)
        d = unit(a, b)
        px, py = a[0] + d[0] * 14, a[1] + d[1] * 14
        ox, oy = (-d[1] * 10, d[0] * 10) if d[0] == 0 else (0, -6)
        anchor = "start" if d[0] == 0 else ("start" if d[0] > 0 else "end")
        if d[0] == 0:
            px += 6
            anchor = "start"
        s.text(px + (ox if d[0] != 0 else 0), py + (oy if d[0] != 0 else 4), txt, 12.5, anchor, "700", "#3a3a3a")
    if label and lpos:
        s.add(f'<rect x="{lpos[0]-len(label)*3.4-4:.0f}" y="{lpos[1]-12}" width="{len(label)*6.8+8:.0f}" height="16" fill="#fff" opacity="0.92"/>')
        s.text(lpos[0], lpos[1], label, 12, "middle", italic=True, fill="#444")


# ---- relations (drawn before boxes so boxes sit on top of line ends)
# héritage
edge([top("Agriculteur"), bot("Utilisateur")], gen=True)
# Utilisateur 1 -- * Notification
edge([rgt("Utilisateur", 60), lft("Notification", 60)], m1="1", m2="*", label="reçoit", lpos=(865, B["Utilisateur"][1] + 52))
# Agriculteur 1 -- * Alerte
edge([lft("Agriculteur", 40), rgt("Alerte", 40)], m1="1", m2="*", label="signale", lpos=(435, B["Agriculteur"][1] + 32))
# Alerte <>-- Traitement
edge([top("Alerte"), bot("Traitement")], comp=True, m1="1", m2="*")
# Agriculteur 1 -- * DemandeRecolte
edge([rgt("Agriculteur", 40), lft("DemandeRecolte", 40)], m1="1", m2="*", label="déclare", lpos=(865, B["Agriculteur"][1] + 32))
# Agriculteur 1 -- 1..* Verger
edge([bot("Agriculteur"), top("Verger")], m1="1", m2="1..*", label="possède", lpos=(B["Verger"][0] + 180, (bot("Agriculteur")[1] + B["Verger"][1]) / 2 + 4))
# Alerte * -- 1 Verger (lane x=440)
a0 = rgt("Alerte", B["Alerte"][3] - 30)
edge([a0, (440, a0[1]), (440, B["Verger"][1] + 26), lft("Verger", 26)], m1="*", m2="1", label="concerne", lpos=(440, B["Verger"][1] - 40))
# Verger * -- 1 Variete
edge([lft("Verger", 70), rgt("Variete", 70)], m1="*", m2="1")
# ReleveMaturite * -- 1 Verger (lane x=440)
r0 = rgt("ReleveMaturite", 30)
vy = B["Verger"][1] + B["Verger"][3] - 30
edge([r0, (445, r0[1]), (445, vy), lft("Verger", B["Verger"][3] - 30)], m1="*", m2="1", label="mesure", lpos=(445, r0[1] - 30))
# Verger <>-- Parcelle
edge([bot("Verger"), top("Parcelle")], comp=True, m1="1", m2="1..*")
# DemandeRecolte * -- 1 Verger (lane x=862)
d0 = lft("DemandeRecolte", B["DemandeRecolte"][3] - 30)
edge([d0, (862, d0[1]), (862, B["Verger"][1] + 40), rgt("Verger", 40)], m1="*", m2="1", label="concerne", lpos=(862, B["Verger"][1] - 30))
# DemandeRecolte <>-- ChangementStatut
edge([bot("DemandeRecolte"), top("ChangementStatut")], comp=True, m1="1", m2="*", label="historique", lpos=(B["ChangementStatut"][0] + 250, (bot("DemandeRecolte")[1] + B["ChangementStatut"][1]) / 2 + 4))
# Tournee <>-- EtapeTournee
edge([bot("Tournee"), top("EtapeTournee")], comp=True, m1="1", m2="1..*", label="étapes", lpos=(B["Tournee"][0] + 260, (bot("Tournee")[1] + B["EtapeTournee"][1]) / 2 + 4))
# EtapeTournee * -- 1 DemandeRecolte
edge([lft("EtapeTournee", 40), rgt("DemandeRecolte", 40)], m1="*", m2="1", label="collecte", lpos=(1295, B["EtapeTournee"][1] + 32))
# Tournee <>-- Affectation (lane x=1725)
t0 = rgt("Tournee", B["Tournee"][3] - 28)
edge([t0, (1725, t0[1]), (1725, B["Affectation"][1] + 40), lft("Affectation", 40)], comp=True, m1="1", m2="*")
# Tournee * -- 1 Pressoir
edge([rgt("Tournee", 50), lft("Pressoir", 50)], m1="*", m2="1", label="destination", lpos=(1725, B["Tournee"][1] + 42))
# Affectation * -- 1 Ressource
edge([rgt("Affectation", 40), lft("Ressource", 40)], m1="*", m2="1")
# Tournee * -- 1 Utilisateur (chefEquipe) over the top
u0 = top("Utilisateur", 0.5)
tt = top("Tournee", 0.25)
edge([tt, (tt[0], 84), (u0[0], 84), u0], m1="*", m2="1", label="chefEquipe", lpos=((u0[0] + tt[0]) / 2, 80))
# Reception * -- 1 Tournee over the top
r1 = top("Reception", 0.5)
t1 = top("Tournee", 0.75)
edge([r1, (r1[0], 104), (t1[0], 104), t1], m1="*", m2="1", label="livraison", lpos=((r1[0] + t1[0]) / 2, 100))
# Reception * -- 1 Pressoir
edge([lft("Reception", 60), rgt("Pressoir", 60)], m1="*", m2="1")
# Reception * -- 1 Ressource (benne)
edge([bot("Reception"), top("Ressource")], m1="*", m2="1", label="benne", lpos=(B["Reception"][0] + 240, (bot("Reception")[1] + B["Ressource"][1]) / 2 + 4))
# Approvisionnement * -- 0..1 Ressource
edge([top("Approvisionnement"), bot("Ressource")], m1="*", m2="0..1", label="matériel", lpos=(B["Ressource"][0] + 240, (bot("Ressource")[1] + B["Approvisionnement"][1]) / 2 + 4))

for n, (c, r, st, a, m, k) in C.items():
    x, y, w, h = B[n]
    box(n, x, y, w, st, a, m, k)
for n, vals, x, y in ENUM_POS:
    box(n, x, y, EW, "enumeration", vals, [], "enum")

# legend
ly = H - 30
s.text(30, ly, "Légende :", 13, weight="700")
s.line([(120, ly - 4), (175, ly - 4)], end="tri")
s.text(183, ly, "héritage", 12.5)
diamond(s, (270, ly - 4), (1, 0))
s.line([(288, ly - 4), (325, ly - 4)])
s.text(333, ly, "composition (sous-document imbriqué)", 12.5)
s.line([(590, ly - 4), (645, ly - 4)])
s.text(653, ly, "association (référence par id)", 12.5)
for i, (k, lab) in enumerate([("doc", "collection MongoDB"), ("emb", "sous-document"), ("ts", "collection time-series"), ("enum", "énumération")]):
    bg, fg = STYLE[k]
    x = 880 + i * 210
    s.add(f'<rect x="{x}" y="{ly-15}" width="26" height="16" fill="{bg}" stroke="{fg}" stroke-width="1.5"/>')
    s.text(x + 34, ly, lab, 12.5)
s.save(f"{OUT}/classes.svg")
print("ok", W, H)
