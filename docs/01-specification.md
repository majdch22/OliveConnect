# OliveConnect — Spécification fonctionnelle

> Plateforme de gestion intelligente de la collecte des olives — Coopérative oléicole de Sfax
> 2ème année ING INFO — AU 2026-2027 — Groupe de 3 étudiants

---

## 1. Étude préalable

### 1.1 Contexte
La coopérative fait face à une hausse de production (extension des vergers, mécanisation). Les collectes sont
mal coordonnées entre récolte, logistique et pressoirs, et les ressources (ouvriers saisonniers, tracteurs, bennes)
sont sous tension. Le volume de données (relevés de maturité, météo, historiques de tournées, pesées) explose en
période de récolte → choix d'une base **NoSQL**.

### 1.2 Problèmes identifiés → réponses OliveConnect
| Problème | Réponse du système |
|---|---|
| Pas de plateforme commune entre services | Application web unique, rôles par profil, notifications |
| Alertes vergers traitées de façon désorganisée | Module d'alertes géolocalisées avec workflow de traitement |
| Aucun suivi temps réel des collectes | Statuts de tournée mis à jour par le chef d'équipe + notifications |
| Pas d'indicateurs de performance | Tableau de bord calculé par agrégation MongoDB |

### 1.3 Fonctionnalités retenues (5/5)
| Code | Fonctionnalité | Justification |
|---|---|---|
| F1 | Plateforme de gestion des collectes et des vergers | Cœur métier, indispensable au flux de bout en bout |
| F2 | Gestion des alertes terrain | Réactivité à la maturité / pluie / ravageurs |
| F3 | Gestion des ressources | Planification sans conflit ouvriers / tracteurs / bennes |
| F4 | Carte interactive vergers / rendements | Exploite les index géospatiaux NoSQL, utile au visiteur |
| F5 | Tableau de bord des performances | Exploite le pipeline d'agrégation sur données massives |

---

## 2. Acteurs (complétés et justifiés)

| Acteur | Rôle technique | Responsabilités | Justification |
|---|---|---|---|
| Visiteur | `ANONYMOUS` | Carte publique, infos générales, demande d'inscription | Vitrine de la coopérative, recrutement d'adhérents |
| Agriculteur (adhérent) | `AGRICULTEUR` | Gère ses vergers, déclare des récoltes, signale maturité/problème, suit ses collectes | Source des demandes |
| Responsable de collecte | `RESP_COLLECTE` | Valide les demandes, planifie les tournées, traite les alertes | Pivot de la coordination |
| Chef d'équipe de récolte | `CHEF_EQUIPE` | Démarre/termine la tournée, saisit avancement et quantités, statut du verger | Remontée terrain temps réel |
| Responsable logistique | `RESP_LOGISTIQUE` | Gère ouvriers, tracteurs, bennes, matériel ; calendrier des tournées | Disponibilité des ressources |
| Responsable du pressoir | `RESP_PRESSOIR` | Réceptionne les bennes, pèse, note la qualité, publie la capacité | Clôture du flux, données qualité |
| Administrateur | `ADMIN` | Comptes et droits, paramétrage, imports externes, supervision serveur | Exploitation du système |
| Service météo / capteurs (simulé) | Système | Fournit météo et niveaux de maturité (JSON/CSV) | Données dynamiques |
| Système du pressoir (simulé) | Système | Fournit la capacité de traitement | Données dynamiques |
| Référentiel coopérative (simulé) | Système | Fournit vergers, parcelles, variétés, ressources, pressoirs | Données de référence |

---

## 3. Cas d'utilisation

### 3.1 Liste par acteur
**Visiteur** : UC01 Consulter la carte publique · UC02 Consulter les informations · UC03 Demander une inscription
**Tous les authentifiés** : UC04 S'authentifier · UC05 Consulter ses notifications · UC06 Consulter l'aide / visite guidée · UC07 Gérer son profil
**Agriculteur** : UC10 Gérer ses vergers · UC11 Déclarer une récolte (planifiée / urgente) · UC12 Modifier / annuler une demande · UC13 Signaler maturité ou problème (géolocalisé) · UC14 Suivre l'avancement de ses collectes
**Responsable de collecte** : UC20 Valider / rejeter une demande · UC21 Planifier une tournée · UC22 Traiter une alerte · UC23 Consulter la carte et le tableau de bord
**Chef d'équipe** : UC30 Consulter ses tournées · UC31 Démarrer une tournée · UC32 Mettre à jour avancement / quantités · UC33 Mettre à jour le statut du verger · UC34 Déclarer la livraison au pressoir
**Responsable logistique** : UC40 Gérer les ressources · UC41 Consulter le calendrier par ressource · UC42 Gérer les approvisionnements (filets, vibreurs)
**Responsable du pressoir** : UC50 Réceptionner une benne (poids, qualité) · UC51 Publier la capacité de traitement · UC52 Consulter le tableau de bord
**Administrateur** : UC60 Gérer comptes et rôles · UC61 Valider les inscriptions · UC62 Paramétrer le système · UC63 Superviser les imports · UC64 Consulter le tableau de bord global
**Systèmes externes** : UC70 Importer référentiels · UC71 Importer données dynamiques (planifié)

### 3.2 Fiches détaillées (cas principaux)

#### UC11 — Déclarer une récolte
- **Acteur** : Agriculteur · **Pré-condition** : authentifié, au moins un verger enregistré
- **Scénario nominal** :
  1. L'agriculteur ouvre « Nouvelle récolte ».
  2. Il choisit le verger, le type (PLANIFIEE / URGENTE), la date souhaitée, la quantité estimée (kg).
  3. Le système valide et crée la demande au statut `SOUMISE`.
  4. Le système notifie les responsables de collecte (URGENTE = priorité haute).
- **Scénarios alternatifs** :
  - A1 Date passée → message d'erreur, pas de création (HTTP 400).
  - A2 Verger appartenant à un autre agriculteur → refus (HTTP 403).
  - A3 Demande active déjà existante pour ce verger et cette date → refus (HTTP 409).
- **Post-condition** : demande `SOUMISE` visible par l'agriculteur et le responsable de collecte.

#### UC12 — Modifier / annuler une demande
- **Règle clé** : autorisé uniquement si la tournée associée n'est pas `EN_COURS` (statuts `SOUMISE`, `VALIDEE`, `PLANIFIEE`).
- **Alternatif** : tournée démarrée → refus (HTTP 409, message « collecte déjà démarrée »).
- Une annulation libère la place dans la tournée et notifie le responsable de collecte.

#### UC13 — Signaler maturité ou problème
- **Scénario nominal** : choix du verger → type (MATURITE / PLUIE / RAVAGEUR / AUTRE) → position GPS proposée
  automatiquement (modifiable) → description + photo optionnelle → alerte `OUVERTE` → notification temps réel au responsable de collecte.
- **Alternatifs** : GPS refusé → saisie manuelle sur carte ; position hors du verger (> 500 m) → avertissement.

#### UC21 — Planifier une tournée
- **Acteur** : Responsable de collecte
- **Scénario nominal** :
  1. Il sélectionne des demandes `VALIDEE` (tri par urgence puis maturité).
  2. Il choisit la date, le chef d'équipe, le pressoir de destination.
  3. Le système propose les ressources **disponibles** à cette date (ouvriers, tracteurs, bennes).
  4. Il affecte les ressources ; le système vérifie l'absence de conflit et la capacité des bennes vs quantités estimées.
  5. Tournée créée `PLANIFIEE` ; demandes passent à `PLANIFIEE` ; agriculteurs et chef d'équipe notifiés.
- **Alternatifs** : ressource déjà affectée le même jour → refus (409) ; capacité insuffisante → avertissement ;
  pressoir saturé ce jour → avertissement + suggestion d'un autre pressoir.

#### UC32 — Mettre à jour l'avancement
- Le chef d'équipe saisit pour chaque étape (verger) : avancement %, quantité récoltée (kg), statut du verger.
- Chaque mise à jour notifie l'agriculteur concerné. Quantité négative → refus (400).

#### UC50 — Réceptionner une benne
- Le responsable du pressoir sélectionne la tournée livrée, la benne, saisit poids net (kg) et qualité (acidité %, catégorie).
- Le système calcule le **taux de remplissage** = poids / capacité de la benne, met à jour la capacité restante du pressoir.
- Toutes les bennes reçues → tournée `CLOTUREE`, demandes `TERMINEE`.
- **Alternatif** : poids > capacité nominale × 1.1 → refus (incohérence de pesée).

#### UC71 — Import planifié des données externes
- Toutes les N minutes (paramétrable), le système lit les fichiers JSON/CSV (ou service simulé), **contrôle la conformité**
  (format, champs obligatoires, bornes, référence verger existante), insère les lignes valides, rejette les autres
  et trace un `ImportJob` (nb OK, nb rejets, erreurs). L'administrateur consulte l'historique.

---

## 4. Cycles de vie (machines à états)

```
DemandeRecolte : SOUMISE ─► VALIDEE ─► PLANIFIEE ─► EN_COURS ─► TERMINEE
                    │          │           │
                    └─► REJETEE└───────────┴─► ANNULEE   (annulation impossible après EN_COURS)

Tournee        : PLANIFIEE ─► EN_COURS ─► LIVREE ─► CLOTUREE
                    └─► ANNULEE

Alerte         : OUVERTE ─► PRISE_EN_CHARGE ─► RESOLUE

StatutRecolte (Verger) : NON_MUR ─► MUR ─► EN_RECOLTE ─► RECOLTE
```

---

## 5. Règles de gestion

| ID | Règle |
|---|---|
| RG01 | Un agriculteur ne voit et ne modifie que ses propres vergers, demandes et alertes. |
| RG02 | Une demande URGENTE est traitée en priorité dans la liste de planification. |
| RG03 | Modification / annulation d'une demande impossible une fois la tournée `EN_COURS`. |
| RG04 | Une ressource ne peut être affectée qu'à une seule tournée par jour. |
| RG05 | La somme des capacités des bennes affectées doit couvrir la quantité estimée (sinon avertissement). |
| RG06 | Une tournée a exactement un chef d'équipe et un pressoir de destination. |
| RG07 | Le poids reçu au pressoir ne peut dépasser 110 % de la capacité de la benne. |
| RG08 | Les données importées non conformes sont rejetées et journalisées, jamais insérées partiellement. |
| RG09 | Un compte agriculteur créé via le formulaire visiteur est inactif jusqu'à validation par l'administrateur. |
| RG10 | Toute transition de statut est historisée (date, auteur, ancien → nouveau statut). |

---

## 6. Exigences non fonctionnelles

| Catégorie | Exigence | Moyen de garantie |
|---|---|---|
| Ergonomie | Utilisable sur téléphone au verger | Design mobile-first avec Tailwind CSS, gros boutons, navigation basse |
| Simplicité | Déclaration / signalement en ≤ 4 saisies | Valeurs par défaut, GPS automatique, verger pré-sélectionné |
| Aide | Aide contextuelle + visite guidée | Bouton « ? » par écran, tour guidé à la 1re connexion |
| Performance | Centaines d'agriculteurs, ~100 connexions simultanées, carte/recherche < 10 s (cible < 1 s) | Index 2dsphere, index composés, projections, pagination, cache |
| Sécurité | Authentification + droits par profil | Spring Security + JWT, BCrypt, contrôles d'appartenance ; côté Angular : intercepteur JWT et guards par rôle |
| Disponibilité | Accès distant permanent | Nginx, Docker restart policy, supervision |
| Sauvegarde | Sauvegarde régulière | `mongodump` quotidien + rotation 7 jours |
| Traçabilité | Logs applicatifs et imports | Log4j2, rotation, `ImportJob` |

### Contraintes techniques retenues
| Domaine | Imposé par l'énoncé | Choix du groupe |
|---|---|---|
| Backend | API web Spring Boot, couches Controller / Service / DAO / Entity, Spring Data, Maven ou Gradle | Spring Boot 3, Java 21, Maven |
| Base de données | SGBD NoSQL | MongoDB 7 + Spring Data MongoDB |
| Frontend | HTML5, CSS3, JavaScript ou framework au choix, utilisable sur téléphone | **Angular 20 + Tailwind CSS 4** (TypeScript) |
| Hébergement | Apache, Nginx ou IIS, sauvegardes, supervision | Nginx + Docker Compose, Prometheus/Grafana, `mongodump` |

Justification du frontend : Angular reprend l'organisation en couches de l'API (services injectables, guards,
intercepteurs), fournit nativement les formulaires réactifs et le client HTTP, et impose une structure commune aux
trois membres du groupe. Tailwind CSS permet de reproduire fidèlement les maquettes en mobile-first.

---

## 7. Indicateurs du tableau de bord (F5)

| KPI | Calcul |
|---|---|
| Rendement par verger | Σ poids reçus (kg) / superficie (ha) — par saison |
| Délai de collecte | moyenne (date début tournée − date de soumission de la demande), séparé PLANIFIEE / URGENTE |
| Taux de remplissage des bennes | moyenne (poids reçu / capacité benne) |
| Volume par pressoir / jour | Σ poids reçus groupés par pressoir et jour |
| Qualité moyenne | moyenne acidité par variété |
| Alertes | nombre par type, délai moyen de prise en charge |
| Utilisation des ressources | jours affectés / jours disponibles par type de ressource |
