# BidCo Backend Server (Cloud Edition)

API RESTful de alto rendimiento modernizada para despliegues en la nube. Esta versión gestiona la persistencia mediante PostgreSQL y utiliza Cloudinary para el almacenamiento externo de archivos multimedia.

## Arquitectura y Stack Tecnológico

* **Framework:** Spring Boot 3.4.4
* **Lenguaje:** Java 25
* **Base de Datos:** PostgreSQL (driver `org.postgresql:postgresql`)
* **Almacenamiento Cloud:** Cloudinary 1.36.0
* **Autenticación:** JJWT 0.12.5 (JSON Web Tokens)
* **Mapeo de Entidades:** MapStruct 1.5.5.Final

## Variables de Entorno (Configuración Cloud)

Para ejecutar este servidor en la nube, **NO escribas tus contraseñas en el código fuente**. Debes inyectar las siguientes variables de entorno en tu plataforma de hosting (Render, Railway, etc.):

* `SPRING_DATASOURCE_URL`: URL de conexión a tu BD PostgreSQL en la nube (formato `jdbc:postgresql://HOST:PUERTO/defaultdb?sslmode=require`).
* `SPRING_DATASOURCE_USERNAME`: Usuario de la BD.
* `SPRING_DATASOURCE_PASSWORD`: Contraseña de la BD.
* `CLOUDINARY_URL`: Credenciales de tu cuenta de Cloudinary para guardar las imágenes.
* `JWT_SECRET`: Clave alfanumérica segura para firmar los tokens de sesión.
* `CORS_ALLOWED_ORIGINS`: URLs permitidas para evitar bloqueos CORS (Ej: `https://bid-co.vercel.app`).

## Despliegue Local (Modo Desarrollo)

Si deseas probar esta versión de producción en tu máquina local:

1. Configura las variables de entorno mencionadas arriba en tu IDE (Eclipse, IntelliJ, VS Code) o en tu archivo `application.properties`.

2. Limpia y compila el proyecto:
```bash
mvn clean install
```

3. Arranca el servidor:
```bash
mvn spring-boot:run
```

La API estará disponible en el puerto `8080`. Al usar Hibernate en modo `update`, las tablas se crearán o actualizarán automáticamente sin necesidad de scripts SQL manuales.
