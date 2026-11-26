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

## Fonctionnalité 3 — Espace administrateur

- Mise en page : menu fixe à gauche (ordinateur) ou en haut (téléphone), badge du nombre d'inscriptions en attente.
- **Accueil** `/administration` : chiffres réels (inscriptions en attente, comptes actifs, désactivés, refusés, comptes actifs par rôle)
  et les cinq dernières inscriptions à traiter.
- **Inscriptions** `/administration/inscriptions` : onglets En attente / Acceptées / Refusées, recherche (nom, carte d'identité,
  téléphone), filtre par délégation, tri (récentes, anciennes, nom A→Z, Z→A), pagination. Fiche dans un panneau latéral :
  - accepter → compte actif + numéro d'adhérent attribué par la collection `compteurs` (ADH-0001, ADH-0002…, sans doublon) ;
  - refuser → motif obligatoire (motifs rapides proposés) ; un refus peut être annulé en acceptant l'inscription.
- **Utilisateurs** `/administration/utilisateurs` : recherche (nom, e-mail, téléphone, carte d'identité, numéro d'adhérent),
  filtres rôle / état / délégation, tri par clic sur les en-têtes de colonnes (liste de tri sur téléphone), pagination 10/20/50.
  Fiche : modifier, désactiver / réactiver, réinitialiser le mot de passe. Création de comptes du personnel et d'administrateurs
  avec un **mot de passe provisoire** affiché une seule fois.
- Filtres, tri et pagination exécutés par MongoDB (seule la page demandée est envoyée) ; tri alphabétique **à la française**
  (sans tenir compte des majuscules ni des accents) ; index composé (rôle, statut).
- Règles protégées par le serveur :
  - compte désactivé → accès coupé **dès sa requête suivante** (le compte est relu à chaque requête, décision 10) ;
  - le rôle est lu en base à chaque requête : un changement de rôle s'applique immédiatement ;
  - un administrateur ne peut ni se désactiver ni changer son propre rôle ; il reste toujours au moins un administrateur actif ;
  - un compte agriculteur ne devient pas un compte du personnel, et inversement ;
  - l'administrateur ne crée pas de compte agriculteur (les agriculteurs s'inscrivent eux-mêmes).

### Compléments (25/11/2026, soir) — Référentiels, Paramètres, Supervision

- **Référentiels** `/administration/referentiels` : chaque onglet explique l'élément en une phrase.
  - Variétés (collection `varietes`, code VAR-01…) et Pressoirs (collection `pressoirs`, code PR-01…, position GeoJSON
    avec index 2dsphere, limitée à la région de Sfax) : recherche, archives, tri par colonne, ajout, modification, archivage.
  - Un pressoir qui a encore des responsables rattachés ne peut pas être archivé.
  - Délégations et Catégories de qualité : listes fixes en lecture seule (catégorie déduite de l'acidité).
  - Un **responsable du pressoir est rattaché à un pressoir** (champ `pressoirId` du compte, décision 4).
- **Paramètres** `/administration/parametres` (décision 9) : document unique `parametres` en sections (campagne, règles métier,
  imports, notifications, sécurité), valeurs bornées, auteur et date de modification. Gardé en mémoire et relu après chaque
  modification : la durée de session et le blocage après mots de passe incorrects s'appliquent **immédiatement**.
- **Supervision** `/administration/supervision` (décision 18) :
  - mesures relevées toutes les 10 s (Micrometer) et gardées une heure : processeur, mémoire, requêtes par minute,
    temps de réponse moyen ; une courbe par mesure (jamais deux échelles sur un même graphique), survol et clavier,
    tableau « Voir les valeurs » ;
  - état des composants (application, MongoDB, mémoire, disque, sauvegardes) avec icône + libellé ; seuils d'alerte :
    mémoire > 85 %, disque libre < 10 %, base > 200 ms, dernière sauvegarde > 26 h ;
  - utilisateurs actifs (requête dans les 15 dernières minutes) ; derniers avertissements et erreurs du journal ;
  - **sauvegardes réelles avec mongodump** : automatique chaque nuit à 3 h + bouton « Sauvegarder maintenant »,
    7 dernières gardées, dossier `sauvegardes/` (exclu de git) ; lien Grafana affiché seulement si son adresse est configurée.
- Les pages de l'administration sont **chargées à la demande** : les visiteurs ne téléchargent pas ce code.

### Compléments (26/11/2026) — Ressources et Carte

- **Ressources** `/administration/ressources` (décisions 8 et M3, rôle R4) :
  - une collection `ressources` pour tracteurs, bennes, ouvriers saisonniers et matériel (champ `type`), codes TR-01, BN-01,
    OUV-001, MAT-01 ; état : disponible, en panne, en révision, en congé ; archivage au lieu de suppression ;
  - règles : une benne a une capacité, un matériel une quantité, seul un ouvrier appartient à une équipe, le type ne change pas ;
  - collection `equipes` (EQ-01…) : effectif et ouvriers disponibles (« 5 sur 6 ») ; une équipe non vide ne peut pas être archivée ;
  - adresses `/api/ressources` et `/api/equipes` ouvertes à l'administrateur et au **responsable logistique** (réutilisées par son espace) ;
  - les périodes d'indisponibilité datées (décision 8) viendront avec le calendrier de la logistique.
- **Carte** `/administration/carte` (fonctionnalité F4) : Leaflet + fond OpenStreetMap France, chargée à la demande.
  Couches activables : pressoirs (repère + fiche au clic) et agriculteurs actifs par délégation (cercles dont la surface
  est proportionnelle au nombre, comptés par une **agrégation MongoDB** `$match` + `$group`). Légende et listes cliquables.
  Les vergers s'y ajouteront avec leur fonctionnalité.
- Bouton « Se déconnecter » de l'administration présenté comme un vrai bouton.

## Fonctionnalité 4 — Espace agriculteur (26/11/2026)

- Mise en page : menu en haut sur ordinateur, **barre d'onglets en bas sur téléphone** (utilisation au verger).
- **Accueil** : « Déclarer une récolte », « Signaler maturité ou problème », prochaine collecte, vergers, derniers signalements.
- **Mes vergers** (collection `vergers`, code V-0001, position GeoJSON + index 2dsphere) : ajout, modification, archivage ;
  position choisie sur la carte ou par le GPS du téléphone (« Ma position »), sinon placée à la main.
- **Déclarer une récolte** (collection `demandes_recolte`, numéro D-0001) : verger, type, date, quantité (+ commentaire).
  Quantité estimée proposée (superficie × rendement de la variété). Règles du serveur :
  urgente dès aujourd'hui, planifiée dès demain, date dans la campagne des paramètres, une seule demande active par verger
  et par date, modification et annulation seulement avant le démarrage de la collecte.
  Historique des changements d'état rangé dans la demande ; **verrou optimiste** (`@Version`, décision 3).
- **Signalements** (collection `alertes`, numéro AL-0001) : type, verger, position, description. Distance au verger calculée
  (haversine) ; au-delà de la distance des paramètres, le signalement est marqué « loin du verger ».
  Photo facultative : voir « Fonctionnalité 5 ».
- **Mon compte** : remplacé par la page commune « Mon profil » (fonctionnalité 5).
- Toutes les adresses `/api/agriculteur/**` sont réservées au rôle AGRICULTEUR et travaillent sur l'agriculteur **du jeton** :
  un agriculteur ne voit ni ne modifie jamais les données d'un autre (403).

## Fonctionnalité 5 — Notifications, mot de passe oublié, profil, photos, carte publique, aide (26/11/2026)

Ce qui manquait encore à notre partie (pages publiques, administrateur, agriculteur) d'après les cas d'utilisation et les maquettes.

- **Notifications (UC05, décision 13)** — collection `notifications`, une copie par destinataire (« pour un rôle » = chaque compte
  actif du rôle, jamais une diffusion à tous). Index (destinataire, date) pour la cloche ; **index TTL de 90 jours**.
  Temps réel : WebSocket STOMP à l'adresse `/ws` ; le **jeton est vérifié dans la trame CONNECT** (un navigateur ne peut pas
  joindre d'en-tête à l'ouverture du WebSocket, qui reste donc publique) ; abonnement permis seulement à sa propre file
  `/user/queue/…` ; envoi de messages par le navigateur refusé. Reconnexion automatique toutes les 5 s.
  Interface : cloche avec pastille, bandeau des notifications en direct, page « Notifications » (Toutes, Non lues, Collectes,
  Alertes, Compte ; « Tout marquer lu »), notifications récentes sur l'accueil de l'agriculteur.
  Envoyées aujourd'hui : nouvelle demande (prioritaire si urgente), demande modifiée ou annulée, signalement → responsables de
  collecte ; nouvelle inscription → administrateurs ; inscription acceptée → bienvenue à l'agriculteur. Les autres espaces
  réutiliseront `NotificationService.notifierPersonne` / `notifierRole`.
- **SMS simulés (décision 17)** — interface `EnvoiSms`, version `SmsSimule` : journal du serveur + collection
  `messages_envoyes` (TTL 90 jours), liste « SMS envoyés » dans la supervision de l'administrateur. Toujours envoyés :
  inscription acceptée (avec le numéro d'adhérent) ou refusée (avec le motif), code de mot de passe oublié. Les SMS de suivi
  des collectes respectent le réglage « SMS » des paramètres et le choix de la personne.
- **Mot de passe oublié (décision 10)** — collection `codes_reinitialisation` : code à 6 chiffres **chiffré avec BCrypt**, un seul
  par compte, **index TTL 10 minutes** (et expiration vérifiée par le code), **3 essais** puis code annulé, nouveau code au plus
  une fois par minute. Réponse identique que le compte existe ou non (on n'apprend pas si un numéro est inscrit).
  Réussite : nouveau mot de passe, blocage levé, connexion automatique. Page en trois étapes avec compte à rebours « Renvoyer ».
- **Mon profil (UC07, maquette P4)** — page commune à tous les rôles : téléphone, e-mail, délégation (agriculteur) modifiables
  **avec le mot de passe actuel** (ils servent à se connecter) et vérification qu'aucun autre compte ne les utilise (409) ;
  préférences « notifications dans l'application » et « SMS pour les collectes » ; changement du mot de passe ; visite guidée.
  L'e-mail récapitulatif de la maquette n'est pas fait : aucun envoi d'e-mail n'a été décidé.
- **Photos des signalements (décision 15)** — sans Docker, fichiers dans `uploads/signalements` (hors git), nommés d'après
  l'identifiant du signalement (aucun nom venant de la personne). Images JPEG, PNG ou WebP **reconnues à leurs premiers octets**,
  5 Mo au plus côté serveur ; **réduites sur le téléphone** (1600 pixels, JPEG) avant l'envoi. Visibles seulement par leur auteur,
  jamais mises en cache. **Incluses dans les sauvegardes** : archive `….photos.zip` à côté de chaque sauvegarde mongodump.
- **Vergers** — suppression réelle d'un verger **jamais utilisé** (aucune demande ni aucun signalement), sinon 409 et archivage
  (M5). Un changement de nom est **reporté dans ses demandes et signalements** (point « champs copiés » tranché pour `nomVerger`).
  Chaque verger affiche ses signalements non résolus ; vue « Liste » ou « Carte » (maquette A1).
- **Carte publique (UC01, maquette M1)** — `/carte`, sans connexion : vergers non archivés des adhérents actifs, recherche,
  filtres variété et maturité ≥ 70 %, légende. **Vie privée** : ni nom de l'agriculteur, ni code du verger, ni texte des
  signalements, **position arrondie à environ 100 m**. Liens depuis l'accueil et la page « Demande envoyée ».
- **Aide et visite guidée (UC06, maquettes P5 et P6)** — bouton « ? » qui ouvre l'aide de l'écran en cours ; guides rapides,
  questions fréquentes, recherche sans accents. Visite guidée en 5 étapes proposée à la première connexion (« Plus tard »
  possible), relançable depuis l'aide ou le profil ; une fois terminée ou passée, elle n'est plus proposée (enregistré sur le compte).
- **Pages d'erreur (maquette P7)** — « Accès refusé » (page d'un autre profil, en nommant le profil attendu), « Page
  introuvable » pour toute adresse inconnue, message « Session expirée » sur la page de connexion quand le serveur refuse le jeton.

## Revue de sécurité (26/11/2026)

- **Jetons d'accès** : signature HS256 avec un secret d'au moins 32 caractères (obligatoire en production, sans valeur par défaut),
  date d'expiration et émetteur « oliveconnect » vérifiés ; jeton falsifié, expiré ou d'un autre émetteur → 401 (testé).
- **Compte relu à chaque requête** : un compte désactivé perd l'accès immédiatement, le rôle vient de la base.
- **Mots de passe** : BCrypt ; règle 8 caractères + lettre + chiffre ; blocage après échecs ; mot de passe provisoire donné par
  l'administrateur → changement **obligatoire** avant d'accéder à son espace (page `/mot-de-passe`).
- **Droits** : chaque adresse est publique par choix (connexion, inscription), ouverte à tout compte connecté (profil, listes)
  ou réservée à un rôle (@PreAuthorize) ; les données d'un agriculteur sont filtrées par l'identifiant du jeton.
- **Erreurs** : toujours au format JSON (y compris 401 et 403 de la couche de sécurité) ; une erreur imprévue est écrite dans
  le journal mais ne montre aucun détail technique.
- **En-têtes** : X-Frame-Options DENY, X-Content-Type-Options nosniff, Referrer-Policy no-referrer, Content-Security-Policy stricte.
- **Requêtes MongoDB** : construites avec Criteria ; toute recherche par texte est échappée (Pattern.quote).
- **Interface** : Angular échappe tout le texte affiché ; les seules bulles HTML (cartes) échappent les noms venant de la base
  (fonction commune `echapperHtml`).
- **Temps réel** : jeton vérifié à la connexion STOMP (testé avec un vrai client : sans jeton, refus), file personnelle seulement,
  origines autorisées limitées aux pages de l'application.
- **Fichiers envoyés** : type reconnu au contenu et non au nom (un texte nommé « photo.jpg » est refusé, testé), taille limitée,
  nom de fichier fabriqué par le serveur.
- **Mot de passe oublié** : aucune fuite sur l'existence d'un compte, codes chiffrés, limités dans le temps et en essais.
- **Points connus, à traiter au déploiement** : HTTPS (décision 1, obligatoire pour le GPS en dehors de « localhost ») ;
  limitation du nombre de requêtes par adresse IP au niveau de Nginx (en particulier sur `/api/mot-de-passe-oublie`) ;
  le jeton est gardé dans le navigateur (localStorage), ce qui reste sûr tant qu'aucun script étranger n'est injecté
  (en-tête de sécurité du contenu à configurer aussi sur Nginx).
- **Nginx au déploiement** : `client_max_body_size 6m` (photos, décision 15) ; transmettre `/ws` en WebSocket
  (en-têtes `Upgrade` et `Connection`) ; variable `ORIGINES_AUTORISEES` = adresse publique du site.

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
- ~~Annulation d'une demande encore SOUMISE~~ → permise tant que la collecte n'a pas démarré (fonctionnalité 4)
- ~~Champs copiés : mise à jour quand l'original change~~ → le nom du verger est reporté (fonctionnalité 5) ; le nom de
  l'agriculteur n'est pas copié dans les demandes
- Demande urgente créée depuis une alerte par le responsable de collecte

### Phase alertes et imports
- États d'alerte « NOUVELLE » et « INFO » présents dans les maquettes mais pas dans le modèle
- Alertes automatiques sans auteur ; alertes météo par zone sans verger
- Alerte de maturité automatique répétée toutes les 15 min (anti-doublon)
- Fichier d'import avec des lignes invalides : sort des lignes valides (RG08 vs statut PARTIEL)

### Phase pressoir et tableau de bord
- Catégorie de qualité saisie ou calculée à partir de l'acidité

### Phase gestion des erreurs
- ~~Table erreur → code HTTP~~ → 400 saisie, 401 non connecté, 403 interdit, 404 introuvable, 405, 409 conflit
  (doublon, modifié par quelqu'un d'autre, non modifiable), 500 imprévu ; 422 non utilisé (GestionnaireErreurs)

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
- ~~Dépôt git, `.gitignore`~~ → fait, dépôt GitHub privé

### Encore ouvert dans notre partie
- Mode hors connexion (décision 16, en attente) et HTTPS (décision 1, au déploiement)
- « Rester connecté » de la maquette de connexion (la session dure le temps fixé dans les paramètres)
- E-mail récapitulatif (préférence de la maquette P4 et réglage des paramètres) : aucun envoi d'e-mail décidé
- Décision 14 : index texte en français non utilisé (les recherches utilisent un tri français et une recherche échappée) ;
  le verger a une position (point) et non un contour
