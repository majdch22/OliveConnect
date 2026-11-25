# OliveConnect — Décisions techniques

> Décisions prises le 24/11/2026 après la revue complète du dossier de conception (docs 01 à 04, diagrammes, maquettes).
> Les sujets « en attente » et les points « à décider pendant le codage » sont listés plus bas pour ne rien oublier.

## 1. Décisions prises

| # | Sujet | Décision | Impact |
|---|---|---|---|
| 1 | HTTPS (le GPS du téléphone exige https) | **En attente** — à traiter au déploiement. Tendance : certificat auto-signé sur l'adresse IP | Nginx, déploiement |
| 2 | Enregistrer plusieurs documents « tout ou rien » | MongoDB en mode **replica set** (un seul nœud) + `@Transactional` | docker-compose, services |
| 3 | Deux personnes modifient la même donnée | **Verrou optimiste** `@Version` sur DemandeRecolte, Tournee, Pressoir, Ressource → message « modifié par quelqu'un d'autre » (409) | Entités, tests |
| 4 | Lien entre une personne et son périmètre | Responsable du pressoir **rattaché à un pressoir** (`pressoirId`) ; chef d'équipe n'agit que sur **ses** tournées, le jour prévu | Utilisateur, services, sécurité |
| 5 | Numéros lisibles (D-118, T-42, AP-21, ADH-0042) | Collection **`counters`** (`findAndModify` + `$inc`) ; l'ObjectId reste l'identifiant interne | Toutes les entités affichées |
| 6 | Codes métier pour les imports | Chaque verger, pressoir, variété, ressource a un **code unique** (V-0042, PR-01, TR-03…) utilisé comme clé d'upsert à l'import | Référentiels, imports |
| 7 | Capacité des pressoirs | **Capacité par pressoir et par jour** (max, réservé, reçu). Le max vient du responsable ou de l'import ; réservé et reçu sont calculés par l'application | Nouvelle collection, planification, réception |
| 8 | Indisponibilité des ressources | **Périodes datées** (ressource, début, fin, motif). Masquées à la planification ; alerte si la ressource est déjà dans une tournée planifiée | Nouvelle collection, RG04, calendrier |
| 9 | Paramètres du système | **Collection de paramètres** modifiable depuis l'écran admin ; planificateur d'import dynamique (`SchedulingConfigurer`) | Admin, imports |
| 10 | États des comptes et mot de passe oublié | États : **en attente, actif, refusé (motif), désactivé** ; compteur d'échecs + blocage ; codes de réinitialisation avec **index TTL** (10 min) ; compte désactivé = accès coupé immédiatement (vérifié dans le filtre JWT) | Utilisateur, sécurité |
| 11 | KPI : olives comptées deux fois | Poids d'une benne **réparti entre vergers** au prorata des quantités saisies par le chef d'équipe | Agrégation rendement |
| 12 | Tableau de bord rapide | KPIs **pré-calculés** (toutes les 15 min + après chaque réception) dans une collection de chiffres journaliers (`$merge`) | Dashboard, module BD massives |
| 13 | Notifications temps réel | Authentification JWT sur la connexion STOMP ; messages envoyés **par personne ou par rôle** (pas de diffusion à tous) | WebSocket, RG01 |
| 14 | Index manquants | Uniques : téléphone, CIN ; e-mail **unique seulement s'il est renseigné** (index partiel) ; 2dsphere sur `vergers.contour` et `pressoirs.localisation` ; index texte en **français** ; index des nouvelles collections. Chaque index justifié dans le rapport | Schéma NoSQL |
| 15 | Photos des signalements | **Fichiers sur le serveur** (volume Docker), images seulement, 5 Mo max, réduites sur le téléphone, incluses dans les sauvegardes ; `client_max_body_size` Nginx | Alertes, déploiement |
| 16 | Mode hors connexion | **En attente** | Frontend |
| 17 | SMS | **Simulés** : un service d'envoi avec une version factice (journal + liste « messages envoyés » côté admin), remplaçable par un vrai fournisseur | Notifications |
| 18 | Page de supervision | **Version simple** : état des services, liste des sauvegardes, dernières erreurs, utilisateurs récents + lien « Ouvrir Grafana » (onglet séparé, protégé). Bouton sauvegarde → signal au conteneur de sauvegarde | Admin, déploiement |
| 19 | Version de Spring Boot | **Spring Boot 4.0** (4.0.10). Spring Initializr ne propose plus la version 3, et le pilote livré avec la 4 est compatible avec MongoDB 9 | Backend |
| 20 | Calendrier des ressources | **Grille faite par nous** (ressources × jours) en Angular + Tailwind, sans bibliothèque payante | Frontend logistique |
| 21 | Récolte urgente pour aujourd'hui | Demande **URGENTE : aujourd'hui ou plus tard** ; demande **PLANIFIEE : à partir de demain** | Validation, tests |
| 22 | Fuseau horaire | **Africa/Tunis** partout (serveur, application, conteneurs) | Configuration |

## Environnement et outils (décidé le 25/11/2026)

| Sujet | Décision |
|---|---|
| Docker | **Pas de Docker.** Tout est installé directement sur le poste (et sur le serveur au déploiement) |
| Emplacement | Tout le projet dans ce dossier (monorepo : `backend/`, `frontend/`, `data/`, `docs/`), partagé avec git |
| Backend | Spring Boot 4.0.10, Java 21, Maven Wrapper (`mvnw`, pas besoin d'installer Maven) |
| Frontend | **Angular 22** (au lieu de 20, dont le support se termine) + Tailwind CSS 4, tests Vitest |
| Base | MongoDB 9.0 installé en service Windows. Mode replica set (décision 2) activé plus tard, au moment de la planification des tournées |
| Tests base de données | Base de test séparée sur le MongoDB local (à la place de Testcontainers, qui exige Docker) |
| Serveur web | Nginx 1.30 déjà présent (`C:\nginx-1.30.6`), utilisé au déploiement |
| Supervision | Prometheus / Grafana installés sans Docker au moment du déploiement (à confirmer à cette étape) |

## Rôles et relations entre profils (décidé le 25/11/2026)

| # | Sujet | Décision |
|---|---|---|
| R1 | Premier administrateur | Créé **automatiquement à l'installation** (e-mail et mot de passe lus dans la configuration du serveur). Un admin peut ensuite créer **d'autres admins** |
| R2 | Plusieurs rôles par personne | **Un seul rôle par compte.** Une personne qui a deux fonctions a deux comptes |
| R3 | Pouvoirs de l'administrateur | **L'admin peut tout faire**, y compris les actions métier (valider, planifier, réceptionner). L'historique (RG10) enregistre toujours l'auteur réel, donc une action faite par l'admin apparaît comme telle |
| R4 | Affectation des ressources aux tournées | Le **responsable de collecte** choisit tracteurs, bennes et ouvriers en planifiant. Le **responsable logistique** peut **remplacer** une ressource sur une tournée déjà planifiée (panne…) ; le responsable de collecte et le chef d'équipe sont notifiés |
| R5 | Chef d'équipe | **Une seule tournée par chef d'équipe et par jour** (même règle que les ressources, RG04). La planification ne propose que les chefs libres ce jour-là |
| R6 | Responsables du pressoir | Chaque responsable est rattaché à **un seul pressoir** ; un pressoir peut avoir **plusieurs responsables** (équipes de jour). Chacun n'agit que sur son pressoir |
| R7 | Agriculteurs et vergers | Chaque verger appartient à **un seul agriculteur** (propriétaire ou exploitant). En cas de location, l'admin transfère le verger vers le nouveau compte |
| R8 | Plusieurs responsables de collecte | Ils **partagent tout** (demandes, tournées, alertes) ; l'historique indique qui a agi et le contrôle de version (décision 3) évite les modifications simultanées. Découpage par zone possible plus tard |

## Modèle de données (décidé le 25/11/2026)

| # | Sujet | Décision |
|---|---|---|
| M1 | Informations de l'agriculteur | **Une seule collection `utilisateurs`** ; les agriculteurs ont un sous-document `agriculteur` (CIN, adresse, n° d'adhérent), absent pour le personnel. Règle de transformation : héritage → sous-document optionnel |
| M2 | Variétés | **Collection `varietes`** (code, nom, rendement moyen kg/ha, saison de récolte), gérée par l'admin et alimentée par l'import. Le verger référence une variété (code + nom copié pour l'affichage) |
| M3 | Ouvriers saisonniers | **Ouvriers listés un par un et regroupés en équipes.** On affecte une équipe en un clic ; un ouvrier absent n'est pas compté (« 7 / 8 disponibles »). RG04 et les indisponibilités s'appliquent à chaque ouvrier |
| M4 | Agriculteur sans verger | **Autorisé.** Le formulaire de récolte demande d'ajouter un verger d'abord |
| M5 | Suppression d'un élément ayant un historique | **Archivage** (marque « archivé » : masqué des listes, plus sélectionnable, toujours visible dans l'historique). Suppression réelle seulement si l'élément n'a jamais été utilisé |

Rappels du fonctionnement retenu : les comptes du personnel sont créés par l'admin ; les agriculteurs s'inscrivent eux-mêmes puis l'admin
valide ; les ouvriers saisonniers n'ont pas de compte (ce sont des ressources gérées par la logistique).

## Règles d'écriture du code (décidé le 25/11/2026)

1. **Aucune abréviation dans l'application** (code, commentaires, écrans) : `RESPONSABLE_COLLECTE` et non `RESP_COLLECTE`,
   « un agriculteur ne voit que ses vergers » et non « RG01 ». Personne ne doit avoir à se souvenir d'un code.
2. Noms métier en français, mots techniques en anglais (`ConnexionService`, `UtilisateurRepository`).
3. Un dossier par domaine métier contenant ses couches (Entity, Repository, Service, Controller, dto).
4. Commentaires courts et utiles, en français : à quoi sert la classe, quelle règle métier est vérifiée.
5. Une fonctionnalité à la fois, complète (API + écran + tests) et qui marche avant de passer à la suivante.
6. Chaque document ne reçoit que les champs dont la fonctionnalité en cours a besoin.
7. Tests nommés comme des phrases (`connexion_refusee_si_mot_de_passe_incorrect`), scénario nominal + scénarios d'erreur.

## Fonctionnalité 1 — Page d'accueil et connexion

- Page d'accueil publique sur `/`, avec un bouton clair « Se connecter » vers `/connexion`.
- `/connexion` : connexion de tous les profils **sauf l'administrateur**.
- `/administration/connexion` : connexion **réservée à l'administrateur**, adresse séparée, non liée depuis l'accueil.
  Chaque page refuse les comptes qui ne lui correspondent pas.

## Fonctionnalité 2 — Inscription d'un agriculteur

- Page `/inscription`, accessible depuis l'accueil (« Devenir adhérent ») et depuis la connexion (« Créer un compte agriculteur »).
- Champs : nom, prénom, carte d'identité (8 chiffres), téléphone tunisien, e-mail facultatif, délégation (liste des 16 délégations
  de Sfax), mot de passe + confirmation, acceptation des conditions d'utilisation.
- Téléphone, e-mail et carte d'identité uniques (index uniques + contrôle avant enregistrement, message sous le champ concerné).
- Le compte est créé **en attente de validation** ; la page `/inscription/envoyee` explique la suite.
- L'étape « premier verger » des maquettes viendra avec la fonctionnalité des vergers (décision M4 : verger facultatif).
- Prochaine étape logique : l'écran administrateur qui valide ou refuse les inscriptions.

## Notes techniques

- L'API écoute sur le port **8081** : le port 8080 est déjà utilisé par Oracle sur le poste de développement.
- Abréviations : les seuls noms courts restants appartiennent aux bibliothèques et ne peuvent pas être renommés
  (`Jwt` de Spring Security, `MediaType.APPLICATION_JSON`, `HttpClient` d'Angular, fichiers de test `*.spec.ts` imposés par l'outil de test).

## Principe de travail

- **Les idées comptent plus que les diagrammes.** Les décisions ci-dessus et le code priment. Les diagrammes (UML, flux, maquettes)
  ne sont pas retouchés au fil de l'eau : ils seront mis à jour **une seule fois, à la fin**, pour refléter le code réel avant le rapport.
- Les petits points de conception sont tranchés **pendant le codage**, au moment où on écrit la partie concernée.

## 2. À décider pendant le codage

### Phase modèle de données / base
- Les deux versions du diagramme de classes (texte dans `02` et image `classes.png`) ne correspondent pas : choisir une seule référence
- Informations de l'agriculteur : bloc imbriqué dans le compte ou héritage avec discriminant
- Variété : collection à part ou simple texte
- Lien Réception ↔ vergers absent du diagramme de classes
- Un agriculteur doit-il avoir au moins un verger ?
- Listes de valeurs manquantes (TypeNotification, SourceImport, StatutImport, StatutAppro)
- Ouvriers un par un ou par équipes
- Suppression d'un verger ou d'une ressource ayant un historique (suppression logique ?)

### Phase demandes et tournées
- Statut « LIVRÉE » : « en route » ou « arrivée » ?
- Le chef d'équipe peut-il changer de pressoir à la livraison ? (RG06)
- Tournée annulée : que deviennent les demandes ?
- Quantité modifiée après planification : revérifier la capacité des bennes ?
- Deuxième voyage vers le pressoir
- Un chef d'équipe peut-il avoir deux tournées le même jour ?
- Annulation d'une demande encore SOUMISE
- Champs copiés (`vergerNom`, `agriculteurNom`) : mise à jour quand l'original change
- Demande urgente créée depuis une alerte par le responsable de collecte

### Phase alertes et imports
- États d'alerte « NOUVELLE » et « INFO » présents dans les maquettes mais pas dans le modèle
- Alertes automatiques sans auteur ; alertes météo par zone sans verger
- Alerte de maturité automatique répétée toutes les 15 min (anti-doublon)
- Fichier d'import avec des lignes invalides : sort des lignes valides (RG08 vs statut PARTIEL)

### Phase pressoir et tableau de bord
- Catégorie de qualité saisie ou calculée à partir de l'acidité

### Phase gestion des erreurs
- Table erreur → code HTTP (400, 401, 403, 404, 409, 422)

### Phase logistique
- Approvisionnements : stock et seuils

### Mise à jour des diagrammes
- UC64 a deux significations différentes (`01` vs `uc-4`)
- Petites corrections des diagrammes de séquence (couches)
- SVG manquants (classes, uc-0, uc-1, uc-2), script du flux absent, chemin obsolète dans `render.sh`

### Rapport
- Justification de l'API de persistance (section exigée par l'énoncé)
- Nommer la « petite application intranet » demandée par l'énoncé
- Plan du rapport

### Mise en place du projet
- Dépôt git, `.gitignore` (exclure `__pycache__/`)
