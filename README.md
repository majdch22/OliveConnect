# OliveConnect

Plateforme de gestion de la collecte des olives — coopérative oléicole de Sfax.
Projet 2ème année ING INFO (Bases de données massives · J2EE · Administration des serveurs Web), AU 2026-2027.

| Dossier | Contenu |
|---|---|
| `backend/` | API Spring Boot 4.0 (Java 21, Maven) — couches Controller / Service / Repository / Entity |
| `frontend/` | Application Angular 22 + Tailwind CSS 4 (mobile-first) |
| `data/` | Sources externes simulées (JSON / CSV) chargées par les imports planifiés |
| `docs/` | Dossier de conception, décisions (`docs/05-decisions.md`), diagrammes, maquettes |

## Prérequis (déjà installés sur le poste)
- Java 21 · Node.js 24 · MongoDB 9 (service Windows, port 27017) · Git
- Maven n'a pas besoin d'être installé : le projet utilise `mvnw` (Maven Wrapper).

## Lancer en développement
```bash
# 1. API (http://localhost:8081)
cd backend
./mvnw spring-boot:run        # Windows PowerShell : .\mvnw spring-boot:run

# 2. Interface (http://localhost:4200) — /api et /ws sont redirigés vers l'API
cd frontend
npm install                   # première fois seulement
npm start
```

## Tests
```bash
cd backend  && ./mvnw test
cd frontend && npm test
```

## Base de données
Base `oliveconnect` sur `mongodb://localhost:27017` (modifiable par la variable `MONGO_URI`).
