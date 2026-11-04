# Colabora+ Backend

Este proyecto es una **API** construida con [Spring Boot](https://spring.io/projects/spring-boot), conectada a una base de datos **PostgreSQL**.  
Esta API servira para la comunicación con la aplicación móvil desarrollada en [React Native](https://reactnative.dev/), utilizando en framework [Expo](https://docs.expo.dev/).


---

## Requisitos previos

Antes de comenzar asegúrate de tener instalado:

- [Java 21](https://sdkman.io/)
- [Maven 3.9+](https://maven.apache.org/)
- [PostgreSQL 14+](https://www.postgresql.org/)

---

## Configuración de la base de datos

Se debe crear una base de datos y un usuario para esta base de datos.

## Variables de entorno

La aplicación se configura con variables de entorno.
Para desarrollo local se puede crear un archivo `.env` en la raíz del proyecto (está en `.gitignore`, nunca se sube):

```bash
# Base de datos
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/db
SPRING_DATASOURCE_USERNAME=dbusr
SPRING_DATASOURCE_PASSWORD=dbpass
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Seguridad
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=admin123
SECURITY_JWT_USER_GENERATOR=AUTH0JWT-BACKEND
SECURITY_JWT_KEY_PRIVATE=key
CORS_ALLOWED_ORIGINS=*

# Admin inicial (se crea solo si no existe; si faltan, no se crea)
ADMIN_EMAIL=admin@ejemplo.com
ADMIN_PASSWORD=cambiar-esto

# Almacenamiento de imágenes (MinIO en local, compatible con S3)
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=user-images

# Correo (contraseña de aplicación de Gmail)
GMAIL_USER=cuenta@gmail.com
GMAIL_APP_PASSWORD=xxxx

# Firebase (notificaciones push)
FIREBASE_CREDENTIALS_PATH=serviceAccountKey.json
```

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Manejo del esquema. No usar `create-drop` en entornos con datos. |
| `SECURITY_JWT_KEY_PRIVATE` | `key` | Clave HS256 para firmar los JWT. **Definirla siempre** fuera de desarrollo (por ejemplo con `openssl rand -base64 32`). |
| `CORS_ALLOWED_ORIGINS` | `*` | Orígenes permitidos separados por coma. |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | vacías | Usuario ADMIN inicial. |
| `MINIO_BUCKET_NAME` | `user-images` | Bucket de imágenes. Si no existe y no se puede crear, la app arranca igual. |
| `FIREBASE_CREDENTIALS_PATH` | `serviceAccountKey.json` | Ruta al JSON de la cuenta de servicio de Firebase. **Obligatorio**: sin él la app no arranca. |

Las fotos de perfil se guardan en el bucket y la API las devuelve como **URL firmada** válida por 60 minutos.

## Levantar la aplicación

```bash
mvn spring-boot:run
```

O con Docker Compose (API, PostgreSQL y MinIO), usando `.env`, `.env.db` y `.env.minio`:

```bash
docker compose up --build
```

## Despliegue

- Cada push a `develop` publica la imagen `ghcr.io/estebancancino/colaboramas-backend:sha-dev-<sha7>` (workflow `.github/workflows/publish-dev.yml`).
- La imagen corre con el perfil `prod` (sin Swagger), usuario no root (`10001`) y soporta rootfs de solo lectura (solo `/tmp` escribible).
- Health checks: `/api/actuator/health/liveness` y `/api/actuator/health/readiness`.
- Las credenciales (Firebase, Gmail, base de datos, bucket) las entrega la plataforma como variables de entorno; nunca van en el repo ni en la imagen.
