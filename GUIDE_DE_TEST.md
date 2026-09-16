# Guide de test — API de Gestion d'un Blog (Articles & Commentaires)

> Projet 4 sur 5. Ce guide décrit, pas à pas, comment lancer l'API depuis un
> terminal et vérifier qu'elle répond bien à chaque fonctionnalité demandée par
> le cahier des charges.

## 1. Prérequis

| Outil | Version | Vérification |
|---|---|---|
| Java (JDK) | 17 ou supérieur | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker + Compose | v2 | `docker compose version` |
| curl | — | `curl --version` |

## 2. Ports utilisés

| Service | Port hôte |
|---|---|
| API REST | **8080** |
| PostgreSQL | **5439** |

> **Important — les 5 projets utilisent tous le port 8080.**
> Testez-les **un seul à la fois** et arrêtez toujours le précédent
> (`docker compose down -v`) avant de démarrer le suivant.

> Le port PostgreSQL hôte est volontairement décalé : le port 5432 est
> fréquemment occupé par une installation locale de PostgreSQL. Le conteneur de
> l'API, lui, joint la base par `postgres:5432` sur le réseau Docker interne —
> ce décalage ne concerne donc que vos propres connexions depuis la machine.

## 3. Lancement

### Méthode A — tout en Docker (au plus proche du rendu)

```bash
cd blog-api
docker compose up -d --build
```

Le premier build télécharge l'image Maven et les dépendances : comptez
plusieurs minutes. Ensuite :

```bash
docker compose ps                 # les services doivent être "healthy"
docker compose logs -f blog-api  # suivre le démarrage
```

### Méthode B — base en Docker, application en local (itération rapide)

```bash
cd blog-api
docker compose up -d postgres

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5439/blog_db \
  mvn spring-boot:run -Dspring-boot.run.fork=false
```

> `-Dspring-boot.run.fork=false` exécute l'application dans le processus Maven :
> un simple **Ctrl+C** l'arrête proprement. Sans cette option, Maven lance une
> JVM fille qui survit à l'arrêt de Maven et garde le port 8080 occupé.
>
> Attention : avec `fork=false`, l'option `-Dspring-boot.run.jvmArguments` est
> ignorée. Les réglages doivent passer par des **variables d'environnement**,
> comme ci-dessus.

### Vérifier que l'API est prête

```bash
curl -s http://localhost:8080/api/actuator/health
# {"status":"UP", ...}
```

## 4. Documentation Swagger (livrable exigé)

Ouvrez dans un navigateur :

    http://localhost:8080/api/swagger-ui.html

Le contrat OpenAPI brut est disponible sur `http://localhost:8080/api/v3/api-docs`.

## 5. Vérification de conformité au cahier des charges

Définissez d'abord l'URL de base :

```bash
B=http://localhost:8080/api
```

### Créer un article (titre, contenu, date de publication)

```bash
curl -s -X POST $B/articles -H 'Content-Type: application/json' \
  -d '{"title":"Introduction à Spring Boot",
       "content":"Contenu de l'"'"'article, 100 caractères minimum. Complétez ce texte pour atteindre la longueur requise par la validation.",
       "author":"Jean Dupont","category":"Tech","status":"PUBLISHED",
       "publicationDate":"2026-09-16T10:00:00"}'
```
Attendu : **201 Created**. Notez le `slug` renvoyé — il sert de permalien.

### Lire tous les articles (paginé)

```bash
curl -s "$B/articles?page=0&size=10"
```
Attendu : **200 OK**, objet paginé (`content`, `totalElements`).
Les listes ne contiennent pas les commentaires, seulement `commentCount`.

### Lire un article spécifique

```bash
curl -s $B/articles/introduction-a-spring-boot   # par slug
curl -s $B/articles/id/1                         # par identifiant
```
La consultation incrémente le compteur de vues.

### Mettre à jour un article

```bash
curl -s -X PUT $B/articles/1 -H 'Content-Type: application/json' \
  -d '{"title":"Guide complet Spring Boot",
       "content":"Nouveau contenu, toujours 100 caractères minimum. Complétez ce texte pour satisfaire la validation."}'
```
Le `slug` reste **inchangé** : les liens déjà partagés continuent de fonctionner.

### Ajouter un commentaire sur un article

```bash
curl -s -X POST $B/articles/1/comments -H 'Content-Type: application/json' \
  -d '{"content":"Article très clair, merci !","authorName":"Marie",
       "authorEmail":"marie@example.com"}'
```
Attendu : **201 Created**, statut initial **PENDING**.

### Modération des commentaires

```bash
curl -s $B/articles/1/comments        # [] — non approuvé, donc invisible
curl -s $B/articles/1/comments/all    # visible côté administration

curl -s -X PUT "$B/articles/1/comments/1/status?status=APPROVED"

curl -s $B/articles/1/comments        # le commentaire apparaît
```

Un commentaire appartenant à un autre article est refusé :
```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  -X PUT "$B/articles/999/comments/1/status?status=APPROVED"   # 404
```

### Rechercher et supprimer

```bash
curl -s "$B/articles/search?query=Spring&page=0&size=10"
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE $B/articles/1   # 204
```

## 6. Tests automatisés

La suite complète s'exécute sans Docker ni réseau (base H2 en mémoire) :

```bash
mvn test
```

Résultat attendu : **39 tests, 0 échec**.

## 7. Arrêt et nettoyage

```bash
# Méthode A
docker compose down -v

# Méthode B : Ctrl+C sur l'application, puis
docker compose down -v
```

`-v` supprime aussi le volume de données : le projet suivant repart d'une base
vierge. **À faire systématiquement avant de tester un autre projet.**

## 8. En cas de problème

| Symptôme | Cause probable | Solution |
|---|---|---|
| `port is already allocated` | Un autre projet tourne encore | `docker compose down -v` dans le projet précédent |
| `Web server failed to start. Port 8080 was already in use` | Application précédente non arrêtée | `ss -ltnp \| grep :8080` puis arrêter le processus |
| `Connection refused` vers la base | Base pas encore prête | `docker compose ps` — attendre l'état `healthy` |
| L'application ignore vos réglages | `jvmArguments` avec `fork=false` | Utiliser des variables d'environnement |
| Swagger renvoie 404 | URL incomplète | Le contexte est `/api` : `/api/swagger-ui.html` |
