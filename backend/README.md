# BidCo · Backend

API REST de BidCo (Spring Boot) con persistencia en PostgreSQL e imágenes en Cloudinary.

## Stack

- Java 25 y Spring Boot 4.1.
- Spring Data JPA con PostgreSQL.
- JWT con JJWT 0.13 y contraseñas con BCrypt (`spring-security-crypto`).
- MapStruct 1.6 para los DTO.
- Cloudinary (SDK 1.39) para las imágenes.
- springdoc-openapi 3.2 (Swagger, desactivado por defecto).

## Variables de entorno

Obligatorias:

| Variable | Descripción |
|---|---|
| `SPRING_DATASOURCE_URL` | Por ejemplo `jdbc:postgresql://localhost:5432/bidco`. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos. |
| `JWT_SECRET` | Secreto HS256 de al menos 32 bytes UTF-8. |
| `CLOUDINARY_API_KEY` | Clave de la API de Cloudinary. |
| `CLOUDINARY_API_SECRET` | Secreto de la API de Cloudinary. |

Opcionales (con su valor por defecto):

| Variable | Por defecto | Descripción |
|---|---|---|
| `JWT_EXPIRATION_MS` | `86400000` | Vida del token (24 h). |
| `DDL_AUTO` | `update` | Modo de Hibernate; en producción conviene `validate`. |
| `APP_TIMEZONE` | `Europe/Madrid` | Zona horaria para cerrar subastas y sorteos. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,https://bid-co.vercel.app` | Orígenes permitidos, separados por comas. |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `https://bid-co-*.vercel.app` | Patrones de origen (previews de Vercel). |
| `SESSION_COOKIE_SAME_SITE` | `none` | Cookie de la sesión de administración. En local: `lax`. |
| `SESSION_COOKIE_SECURE` | `true` | En local sin HTTPS: `false`. |
| `SWAGGER_ENABLED` | `false` | Activa `/swagger-ui.html`. |
| `BREVO_API_KEY` | vacío | Clave de Brevo para enviar el correo de recuperación. |
| `MAIL_FROM` | vacío | Remitente verificado en Brevo. |
| `MAIL_FROM_NAME` | `BidCo` | Nombre del remitente. |
| `FRONTEND_URL` | `https://bid-co.vercel.app` | Base del enlace de restablecimiento de contraseña. |

Sin `BREVO_API_KEY` y `MAIL_FROM`, la recuperación de contraseña queda desactivada y la API responde 503.

## Ejecución local

```bash
./mvnw spring-boot:run      # en Windows: mvnw.cmd spring-boot:run
./mvnw clean verify         # compila y ejecuta las pruebas
```

La API queda en `http://localhost:8080`. Con `DDL_AUTO=update`, Hibernate crea las tablas; `../database/schema_postgres.sql` contiene el esquema de referencia.

## Estructura

```
src/main/java/com/bidco/api_rest/
  config/      CORS, filtro JWT, límite de intentos de login, cabeceras de seguridad, reloj
  controller/  Endpoints REST
  service/     Contratos e implementaciones con la lógica de negocio
  repository/  Spring Data JPA
  model/       Entidades
  dto/         Objetos de entrada y salida
  mapper/      MapStruct
  exception/   Excepciones y manejador global
```
