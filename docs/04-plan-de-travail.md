# OliveConnect — Plan de travail

## Étapes avant codage (pré-code)
| # | Livrable | Fichier | État |
|---|---|---|---|
| 0.1 | Analyse de l'énoncé, acteurs, cas d'utilisation, règles de gestion | `01-specification.md` | ✅ |
| 0.2 | Architecture, diagramme de classes, schéma NoSQL, règles de transformation, API | `02-architecture.md` | ✅ |
| 0.3 | Structure du dépôt | `03-structure.md` | ✅ |
| 0.4 | Diagramme du flux métier | `diagrams/flux-oliveconnect.png` | ✅ |
| 0.5 | Maquettes (wireframes) | `wireframes/` | ✅ |
| 0.6 | Validation par le groupe / l'encadrant | — | ⏳ |

## Phases de réalisation
| Phase | Contenu | Module |
|---|---|---|
| 1 | Squelette : Maven Spring Boot, `ng new` Angular + Tailwind CSS (proxy `/api`), Docker Compose Mongo, git | Tous |
| 2 | Utilisateurs, authentification JWT, rôles, administration des comptes | J2EE |
| 3 | Référentiels + imports planifiés avec contrôle de conformité, jeu de données de démo | BD massives |
| 4 | F1 : demandes de récolte, planification des tournées, notifications | J2EE |
| 5 | F2 : alertes géolocalisées temps réel (WebSocket) | J2EE |
| 6 | F3 : ressources, calendrier, détection de conflits, approvisionnements | J2EE / BD |
| 7 | F4 : carte interactive (Leaflet + requêtes 2dsphere) | BD massives |
| 8 | F5 : tableau de bord (pipelines d'agrégation) | BD massives |
| 9 | Aide contextuelle, visite guidée (driver.js), finitions mobile Tailwind | Frontend Angular |
| 10 | Tests : unitaires, MockMvc, Testcontainers, tests Angular (services, composants), Cypress (parcours), test de charge (k6 / JMeter, 100 utilisateurs) | J2EE |
| 11 | Déploiement Nginx, sauvegardes, Prometheus/Grafana, documentation | Admin serveurs |
| 12 | Rapport final | Tous |

## Répartition proposée (3 étudiants)
| Étudiant | Responsabilité principale | Responsabilité secondaire |
|---|---|---|
| A | Schéma NoSQL, index, imports, tableau de bord (agrégations), carte | Rapport partie BD |
| B | API Spring Boot (sécurité, demandes, tournées, ressources, alertes), tests | Rapport conception |
| C | Frontend Angular + Tailwind (mobile-first), aide/visite guidée, déploiement Nginx/Docker, supervision, sauvegardes | Rapport déploiement |

Chaque étudiant développe aussi les écrans Angular de ses domaines (ex. A : carte et tableau de bord ; B : écrans de
planification) afin que le frontend ne repose pas sur une seule personne. C fournit le squelette commun (`core/`, `shared/`, layout).

## Prise en main Angular (à prévoir en phase 1)
| Sujet | Ce qu'il faut maîtriser |
|---|---|
| Composants standalone, signals | Affichage et état local d'un écran |
| Routeur, `loadChildren`, guards | Espaces par profil et protection par rôle |
| `HttpClient` + intercepteurs | Appels à l'API avec le JWT, gestion 401/403 |
| Reactive Forms | Formulaires courts avec validation (récolte, signalement, réception) |
| RxJS (bases) | Notifications temps réel via STOMP |
| Tailwind CSS | Mise en page mobile-first à partir des maquettes |

## Scénario de démonstration (bout en bout)
1. Visiteur consulte la carte → demande d'inscription → admin active le compte.
2. Agriculteur déclare une récolte URGENTE + signale un ravageur (GPS).
3. Responsable de collecte reçoit la notification, valide, planifie la tournée (conflit de benne détecté puis résolu).
4. Agriculteur voit la collecte planifiée ; tentative d'annulation après démarrage → refusée.
5. Chef d'équipe démarre la tournée, saisit quantités → agriculteur notifié en temps réel.
6. Responsable du pressoir réceptionne (poids, acidité) → tableau de bord mis à jour.
7. Admin montre l'historique d'import, Grafana, et une sauvegarde.
