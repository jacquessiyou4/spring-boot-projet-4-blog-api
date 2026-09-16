# Blog API — Gestion d'Articles & Commentaires

API Spring Boot 3 / Java 17 permettant de publier des **articles** de blog et d'ajouter des **commentaires**.

## Prérequis
- Java 17, Maven 3.9+
- PostgreSQL 15 (ou le conteneur fourni par Docker Compose)

## Démarrage rapide (Docker)
```bash
cd blog-api
docker-compose up -d
# Swagger UI : http://localhost:8080/api/swagger-ui.html
```

## Démarrage local (sans Docker)
```bash
mvn spring-boot:run
# L'application lit application.yml (PostgreSQL sur localhost:5432)
```

## Endpoints
### Articles
| Méthode | URL | Description |
|---|---|---|
| POST | `/api/articles` | Créer un article (titre, contenu, auteur, catégorie, date de publication…) |
| GET | `/api/articles` | Liste paginée des articles (`?page=0&size=10`) |
| GET | `/api/articles/{slug}` | Récupérer un article par son slug (incrémente le compteur de vues) |
| GET | `/api/articles/id/{id}` | Récupérer un article par son identifiant |
| PUT | `/api/articles/{id}` | Mettre à jour un article (**le slug reste inchangé**) |
| DELETE | `/api/articles/{id}` | Supprimer un article |
| GET | `/api/articles/search?query=...` | Rechercher des articles |

Exemple de création d'article :
```json
{
  "title": "Mon premier article de blog",
  "content": "Contenu long d'au moins 100 caractères…",
  "author": "Auteur",
  "category": "Tech",
  "status": "PUBLISHED",
  "publicationDate": "2026-09-15T10:00:00"
}
```

### Commentaires
| Méthode | URL | Description |
|---|---|---|
| POST | `/api/articles/{articleId}/comments` | Ajouter un commentaire (statut initial `PENDING`) |
| GET | `/api/articles/{articleId}/comments` | Commentaires **approuvés** |
| GET | `/api/articles/{articleId}/comments/all` | Tous les commentaires |
| PUT | `/api/articles/{articleId}/comments/{commentId}/status?status=APPROVED` | **Modérer** un commentaire (`PENDING` / `APPROVED` / `REJECTED`) |
| DELETE | `/api/articles/{articleId}/comments/{commentId}` | Supprimer un commentaire |

## Notes
- Les slugs sont générés automatiquement depuis le titre (et rendus uniques, fallback si le titre ne contient pas de caractères utilisables). Ils servent de **permaliens** : une mise à jour du titre ne les modifie pas.
- Un commentaire est créé en `PENDING` et n'apparaît dans `GET .../comments` qu'une fois passé à `APPROVED` via l'endpoint de modération.
- `articleId` est vérifié lors de la modération et de la suppression d'un commentaire : un commentaire d'un autre article renvoie `404`.
- Le champ `comments` n'est renseigné que sur la **lecture d'un article seul** ; les listes paginées ne renvoient que `commentCount` (évite N+1).
- Architecture MVC : `controller / service / repository / entity / dto`.

## Tests
```bash
mvn test
```