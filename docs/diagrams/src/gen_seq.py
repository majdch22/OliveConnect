import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uml_common import SVG, esc, wrap, OLIVE, DARK, INK

OUT = sys.argv[1]
KIND = {"actor": ("#fff", OLIVE), "ui": ("#e8f0d8", OLIVE), "ctrl": ("#dde7f3", "#3d5a80"), "svc": ("#f1e9f5", "#7a4d93"),
        "repo": ("#fbf3df", "#a8741a"), "db": ("#eee", "#555"), "ext": ("#eee", "#555")}
LAYER = {"ui": "«frontend»", "ctrl": "«controller»", "svc": "«service»", "repo": "«repository»", "db": "«database»", "ext": "«système»"}


def seq(fname, title, subtitle, parts, steps, spacing=210, left=140):
    n = len(parts)
    xs = {p[0]: left + i * spacing for i, p in enumerate(parts)}
    # pre-compute height
    y = 0
    for st in steps:
        y += {"msg": 40, "self": 52, "alt": 40, "opt": 40, "loop": 40, "else": 36, "end": 18, "note": 46}[st[0]]
    H = 210 + y + 90
    W = left + (n - 1) * spacing + 390
    s = SVG(W, H, title, subtitle)
    top = 100
    bottom = H - 70
    # lifelines + headers
    for pid, label, kind in parts:
        x = xs[pid]
        bg, fg = KIND[kind]
        s.add(f'<line x1="{x}" y1="{top+70}" x2="{x}" y2="{bottom}" stroke="#999" stroke-width="1.3" stroke-dasharray="6 5"/>')
        if kind == "actor":
            s.add(f'<circle cx="{x}" cy="{top+8}" r="11" fill="#fff" stroke="{fg}" stroke-width="2.2"/>')
            s.add(f'<path d="M{x},{top+19} V{top+42} M{x-18},{top+27} H{x+18} M{x},{top+42} L{x-14},{top+60} M{x},{top+42} L{x+14},{top+60}" stroke="{fg}" stroke-width="2.2" fill="none"/>')
            s.text(x, top + 78, label, 13.5, "middle", "700")
        else:
            w = spacing - 24
            s.add(f'<rect x="{x-w/2}" y="{top}" width="{w}" height="60" rx="6" fill="{bg}" stroke="{fg}" stroke-width="1.8" filter="url(#sh)"/>')
            s.text(x, top + 20, LAYER[kind], 11, "middle", italic=True, fill="#555")
            lines = wrap(label, 24)
            for i, ln in enumerate(lines[:2]):
                s.text(x, top + 38 + i * 15 - (7 if len(lines) > 1 else 0), ln, 13, "middle", "700", DARK)
    y = top + 110
    frames = []
    num = 0
    xmin = min(xs.values()) - 70
    xmax = max(xs.values()) + 90
    elements = []
    for st in steps:
        k = st[0]
        if k in ("alt", "opt", "loop"):
            d = len(frames)
            frames.append([k, st[1], y, [], d])
            y += 40
        elif k == "else":
            frames[-1][3].append((y, st[1]))
            y += 36
        elif k == "end":
            kind, label, y0, elses, d = frames.pop()
            y += 8
            x0, x1 = xmin + d * 12, xmax - d * 12
            col = {"alt": "#a8741a", "opt": "#3d5a80", "loop": "#5b7a1e"}[kind]
            elements.insert(0, f'<rect x="{x0}" y="{y0}" width="{x1-x0}" height="{y-y0}" fill="none" stroke="{col}" stroke-width="1.6"/>')
            tw = 46
            elements.append(f'<path d="M{x0},{y0} h{tw} v14 l-8,8 h-{tw-8} z" fill="{col}"/>')
            elements.append(f'<text x="{x0+6}" y="{y0+15}" font-size="12" font-weight="700" fill="#fff">{kind}</text>')
            elements.append(f'<rect x="{x0+tw+6}" y="{y0+3}" width="{len(label)*7+20}" height="17" fill="#fff" opacity="0.93"/>')
            elements.append(f'<text x="{x0+tw+10}" y="{y0+16}" font-size="12.5" font-weight="700" fill="{col}">[{esc(label)}]</text>')
            for ey, el in elses:
                elements.append(f'<line x1="{x0}" y1="{ey+4}" x2="{x1}" y2="{ey+4}" stroke="{col}" stroke-width="1.3" stroke-dasharray="8 5"/>')
                elements.append(f'<rect x="{x0+6}" y="{ey+9}" width="{len(el)*7+20}" height="17" fill="#fff" opacity="0.93"/>')
                elements.append(f'<text x="{x0+10}" y="{ey+22}" font-size="12.5" font-weight="700" fill="{col}">[{esc(el)}]</text>')
            y += 10
        elif k == "msg":
            _, a, b, text, typ = st
            num += 1
            y += 26
            xa, xb = xs[a], xs[b]
            ret = typ == "ret"
            dash = ' stroke-dasharray="7 5"' if ret else ""
            mk = "open" if ret else "fill"
            elements.append(f'<path d="M{xa},{y} L{xb},{y}" stroke="#2b2b2b" stroke-width="1.5" fill="none"{dash} marker-end="url(#{mk})"/>')
            mx = (xa + xb) / 2
            lab = f"{num}: {text}"
            lw = len(lab) * 6.5 + 10
            elements.append(f'<rect x="{mx-lw/2:.1f}" y="{y-20}" width="{lw:.1f}" height="17" fill="#fff" opacity="0.93"/>')
            elements.append(f'<text x="{mx}" y="{y-7}" font-size="12.5" text-anchor="middle" fill="{"#555" if ret else INK}"{" font-style=\"italic\"" if ret else ""}>{esc(lab)}</text>')
            y += 14
        elif k == "self":
            _, a, text = st
            num += 1
            y += 20
            x = xs[a]
            elements.append(f'<path d="M{x},{y} h40 v22 h-40" stroke="#2b2b2b" stroke-width="1.5" fill="none" marker-end="url(#fill)"/>')
            lab = f"{num}: {text}"
            elements.append(f'<rect x="{x+44}" y="{y+2}" width="{len(lab)*6.5+10:.1f}" height="17" fill="#fff" opacity="0.93"/>')
            elements.append(f'<text x="{x+48}" y="{y+15}" font-size="12.5" fill="{INK}">{esc(lab)}</text>')
            y += 32
        elif k == "note":
            _, a, text = st
            x = xs[a]
            w = max(len(text) * 6.6 + 24, 160)
            y += 8
            elements.append(f'<path d="M{x-w/2},{y} h{w-12} l12,12 v22 h-{w} z" fill="#fffbe9" stroke="#d9c27a" stroke-width="1.3"/>')
            elements.append(f'<text x="{x}" y="{y+22}" font-size="12" text-anchor="middle" fill="#555">{esc(text)}</text>')
            y += 38
    for e in elements:
        s.add(e)
    # legend
    ly = H - 28
    s.text(30, ly, "Légende :", 13, weight="700")
    s.line([(120, ly - 4), (175, ly - 4)], end="fill")
    s.text(183, ly, "appel synchrone", 12.5)
    s.line([(320, ly - 4), (375, ly - 4)], dash=True, end="open")
    s.text(383, ly, "réponse", 12.5)
    for i, (k, lab) in enumerate([("ui", "frontend Angular"), ("ctrl", "@RestController"), ("svc", "@Service"), ("repo", "Spring Data Repository")]):
        bg, fg = KIND[k]
        x = 500 + i * 200
        s.add(f'<rect x="{x}" y="{ly-15}" width="26" height="16" fill="{bg}" stroke="{fg}" stroke-width="1.5"/>')
        s.text(x + 34, ly, lab, 12.5)
    s.save(f"{OUT}/{fname}.svg")


# ---------------------------------------------------------- 1. Authentification
seq("seq-1-authentification", "Diagramme de séquence — S'authentifier (UC04)",
    "Connexion avec JWT : scénario nominal et scénarios alternatifs",
    [("u", "Utilisateur", "actor"), ("f", "Application Angular", "ui"), ("c", "AuthController", "ctrl"),
     ("s", "AuthService", "svc"), ("j", "JwtService", "svc"), ("r", "UtilisateurRepository", "repo"), ("db", "MongoDB", "db")],
    [("msg", "u", "f", "saisit identifiant + mot de passe", "call"),
     ("msg", "f", "c", "POST /api/auth/login {login, motDePasse}", "call"),
     ("msg", "c", "s", "authentifier(LoginRequest)", "call"),
     ("msg", "s", "r", "findByEmailOrTelephone(login)", "call"),
     ("msg", "r", "db", "find({email | telephone})", "call"),
     ("msg", "db", "r", "document utilisateur", "ret"),
     ("msg", "r", "s", "Optional<Utilisateur>", "ret"),
     ("alt", "identifiants valides et compte actif"),
     ("self", "s", "BCrypt.matches(motDePasse, hash)"),
     ("msg", "s", "j", "generer(id, role)", "call"),
     ("msg", "j", "s", "jwt (HS256, 8 h)", "ret"),
     ("msg", "s", "c", "AuthResponse(token, role)", "ret"),
     ("msg", "c", "f", "200 OK {token, role}", "ret"),
     ("self", "f", "stocke le JWT et redirige vers l'espace du rôle"),
     ("else", "utilisateur introuvable ou mot de passe incorrect"),
     ("msg", "s", "c", "BadCredentialsException", "ret"),
     ("msg", "c", "f", "401 Unauthorized", "ret"),
     ("msg", "f", "u", "« Identifiants incorrects » (tentatives restantes)", "ret"),
     ("else", "compte inactif — inscription non validée (RG09)"),
     ("msg", "s", "c", "DisabledException", "ret"),
     ("msg", "c", "f", "403 « compte en attente de validation »", "ret"),
     ("end",)],
    spacing=225)

# ---------------------------------------------------------- 2. Déclarer une récolte
seq("seq-2-declarer-recolte", "Diagramme de séquence — Déclarer une récolte (UC11)",
    "Contrôleur → Service → Repository, règles RG01 (propriété) et anti-doublon, notification temps réel",
    [("a", "Agriculteur", "actor"), ("f", "Application Angular", "ui"), ("c", "DemandeRecolteController", "ctrl"),
     ("s", "DemandeRecolteService", "svc"), ("v", "VergerRepository", "repo"), ("d", "DemandeRecolteRepository", "repo"),
     ("n", "NotificationService", "svc")],
    [("msg", "a", "f", "remplit le formulaire (verger, type, date, kg)", "call"),
     ("msg", "f", "c", "POST /api/demandes + JWT", "call"),
     ("self", "c", "@Valid DemandeRequest (date future, kg > 0)"),
     ("msg", "c", "s", "creer(dto, agriculteurCourant)", "call"),
     ("msg", "s", "v", "findById(vergerId)", "call"),
     ("msg", "v", "s", "Verger", "ret"),
     ("alt", "verger d'un autre agriculteur (RG01)"),
     ("msg", "s", "c", "AccessDeniedException", "ret"),
     ("msg", "c", "f", "403 Forbidden", "ret"),
     ("else", "demande active déjà existante pour ce verger et cette date"),
     ("msg", "s", "d", "existsByVergerIdAndDateSouhaiteeAndStatutIn(…)", "call"),
     ("msg", "d", "s", "true", "ret"),
     ("msg", "s", "c", "ConflictException", "ret"),
     ("msg", "c", "f", "409 Conflict", "ret"),
     ("else", "cas nominal"),
     ("msg", "s", "d", "save(demande statut = SOUMISE)", "call"),
     ("msg", "d", "s", "demande enregistrée (id)", "ret"),
     ("msg", "s", "n", "notifierResponsablesCollecte(demande)", "call"),
     ("self", "n", "save(Notification) + push STOMP /topic/demandes"),
     ("msg", "s", "c", "DemandeDto", "ret"),
     ("msg", "c", "f", "201 Created", "ret"),
     ("msg", "f", "a", "« Demande envoyée » — statut SOUMISE", "ret"),
     ("end",)],
    spacing=225)

# ---------------------------------------------------------- 3. Planifier une tournée
seq("seq-3-planifier-tournee", "Diagramme de séquence — Planifier une tournée (UC21)",
    "Vérification des disponibilités (RG04), capacité des bennes (RG05), mise à jour des demandes et notifications",
    [("r", "Responsable de collecte", "actor"), ("f", "Application Angular", "ui"), ("c", "TourneeController", "ctrl"),
     ("s", "PlanificationService", "svc"), ("rr", "RessourceRepository", "repo"), ("t", "TourneeRepository", "repo"),
     ("d", "DemandeRecolteRepository", "repo"), ("n", "NotificationService", "svc")],
    [("msg", "r", "f", "choisit demandes validées, date, chef, pressoir", "call"),
     ("msg", "f", "c", "GET /api/ressources/disponibles?date=", "call"),
     ("msg", "c", "s", "ressourcesDisponibles(date)", "call"),
     ("msg", "s", "rr", "findByEtat(DISPONIBLE)", "call"),
     ("msg", "s", "t", "findByDate(date) → ressources déjà prises", "call"),
     ("msg", "s", "f", "liste des ressources libres", "ret"),
     ("msg", "r", "f", "affecte tracteur, bennes, équipe", "call"),
     ("msg", "f", "c", "POST /api/tournees", "call"),
     ("msg", "c", "s", "planifier(TourneeRequest)", "call"),
     ("msg", "s", "t", "existsByDateAndAffectationsRessourceIdIn(…)", "call"),
     ("alt", "une ressource est déjà affectée ce jour (RG04)"),
     ("msg", "s", "c", "ConflictException « TR-05 déjà affecté »", "ret"),
     ("msg", "c", "f", "409 Conflict", "ret"),
     ("else", "cas nominal"),
     ("self", "s", "Σ capacité bennes ≥ Σ kg estimés ? sinon avertissement (RG05)"),
     ("msg", "s", "t", "save(tournée statut = PLANIFIEE)", "call"),
     ("msg", "s", "d", "updateStatut(demandes, PLANIFIEE) + historique", "call"),
     ("msg", "s", "n", "notifier(agriculteurs, chef d'équipe)", "call"),
     ("msg", "s", "c", "TourneeDto", "ret"),
     ("msg", "c", "f", "201 Created", "ret"),
     ("end",)],
    spacing=215)

# ---------------------------------------------------------- 4. Réception au pressoir
seq("seq-4-reception-pressoir", "Diagramme de séquence — Réceptionner une benne au pressoir (UC50)",
    "Contrôle de pesée (RG07), taux de remplissage, capacité du pressoir et clôture automatique de la tournée",
    [("p", "Responsable du pressoir", "actor"), ("f", "Application Angular", "ui"), ("c", "ReceptionController", "ctrl"),
     ("s", "ReceptionService", "svc"), ("rr", "RessourceRepository", "repo"), ("re", "ReceptionRepository", "repo"),
     ("pr", "PressoirRepository", "repo"), ("t", "TourneeRepository", "repo"), ("n", "NotificationService", "svc")],
    [("msg", "p", "f", "saisit benne, poids, acidité, catégorie", "call"),
     ("msg", "f", "c", "POST /api/receptions", "call"),
     ("msg", "c", "s", "enregistrer(ReceptionRequest)", "call"),
     ("msg", "s", "rr", "findById(benneId)", "call"),
     ("msg", "rr", "s", "Ressource (capaciteKg)", "ret"),
     ("alt", "poids > 110 % de la capacité de la benne (RG07)"),
     ("msg", "s", "c", "BusinessException", "ret"),
     ("msg", "c", "f", "422 « pesée incohérente »", "ret"),
     ("else", "cas nominal"),
     ("self", "s", "tauxRemplissage = poids / capacité"),
     ("msg", "s", "re", "save(reception)", "call"),
     ("msg", "s", "pr", "decrementerCapacite(pressoirId, poids)", "call"),
     ("msg", "s", "re", "countByTourneeId(tourneeId)", "call"),
     ("opt", "dernière benne de la tournée reçue"),
     ("msg", "s", "t", "statut CLOTUREE + demandes TERMINEE", "call"),
     ("msg", "s", "n", "notifier agriculteurs (poids, qualité)", "call"),
     ("end",),
     ("msg", "s", "c", "ReceptionDto", "ret"),
     ("msg", "c", "f", "201 Created", "ret"),
     ("end",)],
    spacing=205)

# ---------------------------------------------------------- 5. Import planifié
seq("seq-5-import-planifie", "Diagramme de séquence — Import planifié des données externes (UC71)",
    "Chargement périodique JSON/CSV avec contrôle de conformité et traçabilité (RG08)",
    [("sc", "ImportScheduler (@Scheduled)", "svc"), ("i", "ImportService", "svc"), ("src", "Sources simulées (JSON/CSV)", "ext"),
     ("v", "DonneesValidator", "svc"), ("rm", "ReleveMaturiteRepository", "repo"), ("vg", "VergerRepository", "repo"),
     ("j", "ImportJobRepository", "repo"), ("n", "NotificationService", "svc")],
    [("loop", "toutes les 15 minutes (cron paramétrable)"),
     ("msg", "sc", "i", "importerDonneesDynamiques()", "call"),
     ("msg", "i", "src", "lire(maturite_AAAAMMJJ.csv)", "call"),
     ("msg", "src", "i", "lignes", "ret"),
     ("loop", "pour chaque ligne"),
     ("msg", "i", "v", "valider(ligne)", "call"),
     ("alt", "ligne conforme"),
     ("self", "i", "ajouter au lot à insérer"),
     ("else", "ligne non conforme (format, bornes, verger inconnu)"),
     ("self", "i", "ajouter l'erreur au rapport (RG08)"),
     ("end",),
     ("end",),
     ("msg", "i", "rm", "insertAll(lot valide)", "call"),
     ("msg", "i", "vg", "updateNiveauMaturite(vergerId, indice)", "call"),
     ("opt", "indice de maturité ≥ seuil (70 %)"),
     ("msg", "i", "n", "creerAlerteAutomatique(verger)", "call"),
     ("end",),
     ("msg", "i", "j", "save(ImportJob nbOk, nbRejets, erreurs)", "call"),
     ("self", "i", "déplacer le fichier dans archive/"),
     ("end",)],
    spacing=215, left=150)
print("ok")
