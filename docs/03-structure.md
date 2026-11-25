# OliveConnect — Structure du projet

Monorepo unique : `backend/` (Spring Boot), `frontend/` (Angular + Tailwind CSS), `deploy/` (Nginx, Docker, supervision), `data/` (sources simulées), `docs/` (rapport).

```
oliveconnect/
├── README.md
├── docker-compose.yml                 # mongo, backend, nginx(+frontend), prometheus, grafana, backup
├── .env.example                       # MONGO_URI, JWT_SECRET, IMPORT_CRON, ...
│
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/tn/oliveconnect/
│       │   │   ├── OliveConnectApplication.java
│       │   │   ├── config/            # SecurityConfig, WebSocketConfig, MongoConfig (index), OpenApiConfig, CorsConfig
│       │   │   ├── security/          # JwtService, JwtAuthFilter, CurrentUser, UserDetailsServiceImpl
│       │   │   ├── common/
│       │   │   │   ├── exception/     # GlobalExceptionHandler, NotFound/Conflict/Forbidden/BusinessException
│       │   │   │   └── dto/           # ApiError, PageResponse
│       │   │   ├── utilisateur/       # ── un package par domaine (feature) ──
│       │   │   │   ├── Utilisateur.java, Role.java, InfosAgriculteur.java      (Entity)
│       │   │   │   ├── UtilisateurRepository.java                              (DAO)
│       │   │   │   ├── UtilisateurService.java, AuthService.java               (Service)
│       │   │   │   ├── AuthController.java, AdminUtilisateurController.java    (Controller)
│       │   │   │   └── dto/  (LoginRequest, InscriptionRequest, UtilisateurDto, ...)
│       │   │   ├── verger/            # Verger, Parcelle, Variete, StatutRecolte, VergerRepository, VergerService, VergerController, PublicController
│       │   │   ├── demande/           # DemandeRecolte, TypeRecolte, StatutDemande, ChangementStatut, ... Controller/Service/Repository
│       │   │   ├── alerte/            # Alerte, TypeAlerte, StatutAlerte, Traitement, ...
│       │   │   ├── tournee/           # Tournee, EtapeTournee, Affectation, StatutTournee, PlanificationService, ...
│       │   │   ├── ressource/         # Ressource, TypeRessource, Approvisionnement, DisponibiliteService, ...
│       │   │   ├── pressoir/          # Pressoir, Reception, ReceptionService, ...
│       │   │   ├── notification/      # Notification, NotificationService (persist + STOMP push), Controller
│       │   │   ├── dashboard/         # DashboardService (MongoTemplate aggregations), KpiDto, DashboardController
│       │   │   └── importation/       # ImportScheduler, parsers/, validators/, ImportJob, ImportJobRepository, MockExternalController
│       │   └── resources/
│       │       ├── application.yml, application-dev.yml, application-prod.yml
│       │       ├── log4j2-spring.xml
│       │       └── seed/              # jeu de données de démo (comptes de test par rôle)
│       └── test/java/tn/oliveconnect/
│           ├── demande/  DemandeRecolteServiceTest (Mockito), DemandeRecolteControllerTest (MockMvc)
│           ├── tournee/  PlanificationServiceTest (conflits de ressources)
│           ├── importation/ ImportValidatorTest
│           └── it/       *IT.java (Testcontainers MongoDB, scénarios bout en bout)
│
├── frontend/                          # Angular 20 (Angular CLI) + Tailwind CSS 4
│   ├── package.json, angular.json, tsconfig.json
│   ├── .postcssrc.json                # plugin @tailwindcss/postcss
│   ├── proxy.conf.json                # dev : /api et /ws → http://localhost:8080
│   └── src/
│       ├── main.ts, index.html
│       ├── styles.css                 # @import "tailwindcss"; + thème (couleurs olive, police)
│       ├── environments/              # environment.ts / environment.prod.ts (apiUrl, wsUrl)
│       └── app/
│           ├── app.config.ts          # provideRouter, provideHttpClient(withInterceptors([jwtInterceptor]))
│           ├── app.routes.ts          # routes par profil, chargées à la demande (loadChildren)
│           ├── core/
│           │   ├── auth/              # auth.service.ts, jwt.interceptor.ts, role.guard.ts, current-user.ts
│           │   ├── models/            # interfaces miroir des DTO Java (verger.ts, demande.ts, tournee.ts, ...)
│           │   ├── api/               # api-error.interceptor.ts (401 → login, 403 → page refusée)
│           │   ├── realtime/          # stomp.service.ts (@stomp/rx-stomp), notifications.store.ts
│           │   └── layout/            # shell mobile (bottom-nav) et bureau (side-nav), header, notification-bell
│           ├── shared/
│           │   ├── ui/                # status-badge, kpi-card, card, button, segmented, timeline, empty-state
│           │   ├── map/               # map-view (Leaflet), geoloc.service.ts
│           │   ├── help/              # help-button, help-panel, tour.service.ts (driver.js)
│           │   └── pipes/             # statut-label, kg, date-fr
│           └── features/              # un dossier par profil = écrans des maquettes
│               ├── public/            # accueil (P1), carte-publique (M1), connexion (M8, AU1, AU5),
│               │                      # mot-de-passe-oublie (AU2), inscription (P2, AU3, AU4, AU6)
│               ├── commun/            # notifications (P3), profil (P4), aide (P5), erreurs (P7)
│               ├── agriculteur/       # accueil (M2), vergers (A1, A2), recolte (M3), signalement (M4),
│               │                      # collectes (M5, A3), alertes (A4)
│               ├── collecte/          # demandes + planification (D1, RC1), tournees (RC2), alertes (RC3), carte (RC4)
│               ├── chef/              # tournees (C1), terrain (M6), livraison (C2)
│               ├── logistique/        # calendrier (D2), ressources (L1), approvisionnements (L2)
│               ├── pressoir/          # reception (M7), historique-capacite (PR1)
│               ├── dashboard/         # tableau-de-bord (D3)
│               └── admin/             # imports (D4), utilisateurs (AD1), inscriptions (AD2), parametres (AD3), supervision (AD4)
│                                      # chaque feature : pages/ (composants), data/ (service HTTP), *.routes.ts
│
├── deploy/
│   ├── README.md               # procédure de déploiement, utilisateurs/droits, restauration
│   ├── nginx/nginx.conf        # reverse proxy /api /ws, gzip, cache, HTTPS
│   ├── backend.Dockerfile, frontend.Dockerfile
│   ├── backup/backup.sh        # mongodump + rotation
│   ├── prometheus/prometheus.yml
│   └── grafana/dashboards/oliveconnect.json
│
├── data/                       # sources externes simulées (montées dans le conteneur)
│   ├── referentiels/  vergers.json, varietes.json, ressources.json, pressoirs.json
│   ├── meteo/         meteo_YYYYMMDD.csv
│   ├── maturite/      maturite_YYYYMMDD.csv
│   └── pressoirs/     capacite.json
│
└── docs/
    ├── 01-specification.md
    ├── 02-architecture.md
    ├── 03-structure.md
    ├── 04-plan-de-travail.md
    ├── diagrams/   flux-oliveconnect.(svg|png), uml/ (cas d'utilisation, classes, séquence), src/ (générateurs)
    └── wireframes/ wireframes.html, *.png
```

## Correspondance backend ↔ frontend (exemple : demandes de récolte)
| Backend (Spring Boot) | Frontend (Angular) |
|---|---|
| `demande/DemandeRecolteController` — `POST /api/demandes` | `features/agriculteur/data/demande.service.ts` — `creer()` |
| `demande/dto/DemandeRequest`, `DemandeDto` (records) | `core/models/demande.ts` (interfaces TypeScript) |
| Bean Validation (`@NotNull`, `@Future`, `@Positive`) | Reactive Forms (`Validators.required`, validateur date future) |
| `@PreAuthorize("hasRole('AGRICULTEUR')")` | `canActivate: [roleGuard('AGRICULTEUR')]` |
| `NotificationService` (push STOMP) | `core/realtime/stomp.service.ts` (abonnement) |

## Conventions
- **Package par domaine** : chaque dossier métier contient ses 4 couches (Entity, Repository, Service, Controller) + `dto/` →
  la traçabilité « cas d'utilisation → couches » demandée dans le rapport est directe.
- Les entités ne sortent jamais du service : les contrôleurs exposent des **DTO** (records Java) mappés manuellement ou via MapStruct.
- Noms métier en français (cohérence avec le diagramme de classes), code technique en anglais (`Service`, `Repository`).
- Frontend : **composants standalone**, état local avec les **signals**, un service HTTP par domaine, aucun appel HTTP
  directement dans un composant. Styles uniquement en classes Tailwind (pas de CSS par composant sauf exception).
- Les noms des modèles TypeScript reprennent ceux des DTO Java (`DemandeRecolte`, `StatutDemande`…) pour garder la cohérence modèle ↔ API ↔ interface.
- Branches git : `main` (stable), `dev`, `feature/<domaine>`.
