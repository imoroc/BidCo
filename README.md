<div align="center">
  <h1 align="center">BidCo</h1>

  <p align="center">
    <strong>Plataforma web Full-Stack para gestionar subastas simuladas, subastas a ciegas y sorteos.</strong>
    <br /><br />
    <a href="https://bid-co.vercel.app/auction"><strong>Ver demostración en vivo</strong></a>
    <br /><br />
  </p>
</div>

---

## Sobre el proyecto

BidCo es una aplicación universitaria desarrollada para la asignatura Servicios y Sistemas Web. Está construida con una arquitectura cliente-servidor y utiliza créditos y puntos virtuales para simular la participación. No integra pasarelas de pago ni procesa dinero real.

**Características principales**
- Subastas tradicionales y a ciegas.
- Sorteos con puntos virtuales.
- Gestión de perfiles, privacidad y estadísticas.
- Valoraciones de expertos.
- Almacenamiento de imágenes en Cloudinary.

## Stack tecnológico

| Área | Tecnología |
|---|---|
| Frontend | Angular 21, TypeScript, CSS |
| Backend | Java 25, Spring Boot 4 |
| Persistencia | PostgreSQL, Spring Data JPA |
| Seguridad | JWT, BCrypt, CORS |
| Imágenes | Cloudinary |
| Documentación API | OpenAPI / Swagger |
| CI | GitHub Actions, CodeQL (configuración predeterminada), Dependabot |

## Requisitos

- Node.js 22 y npm.
- JDK 25.
- Maven (o Maven Wrapper).
- PostgreSQL.
- Credenciales de Cloudinary para las funciones de imágenes.

## Puesta en marcha

### 1. Clonar

```bash
git clone https://github.com/imoroc/BidCo.git
cd BidCo
```

### 2. Backend

Configura las siguientes variables de entorno. El secreto JWT es obligatorio y debe contener al menos 32 bytes UTF-8. Genera uno nuevo para cada entorno; no reutilices el valor de ejemplo de la documentación.

Linux/macOS:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/bidco"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="tu-password"
export JWT_SECRET="$(openssl rand -base64 48)"
export CLOUDINARY_API_KEY="tu-api-key"
export CLOUDINARY_API_SECRET="tu-api-secret"
```

PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/bidco"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="tu-password"
$env:JWT_SECRET=[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
$env:CLOUDINARY_API_KEY="tu-api-key"
$env:CLOUDINARY_API_SECRET="tu-api-secret"
```

Arranca el backend:

```bash
cd backend
./mvnw spring-boot:run
```

En Windows utiliza `mvnw.cmd spring-boot:run`.

### 3. Frontend

```bash
cd frontend
npm ci
npm start
```

Angular estará disponible en `http://localhost:4200` y el backend en `http://localhost:8080`.

## Configuración de producción

Guarda las variables de entorno en el proveedor de despliegue (Render, por ejemplo), nunca en el repositorio. Mantén los logs de SQL y los mensajes internos desactivados en producción. Configura CORS únicamente con los orígenes reales del frontend.

## Calidad y seguridad

- Los saldos y puntos iniciales se asignan en el backend.
- Las rutas privadas requieren tokens JWT firmados con un secreto configurado.
- Las operaciones sobre perfiles verifican que el usuario autenticado sea el propietario.
- Las vistas públicas reciben un DTO reducido y no los datos de contacto o dirección.
- GitHub Actions compila ambos proyectos y ejecuta las pruebas del backend.
- CodeQL analiza Java y TypeScript; Dependabot mantiene las dependencias.

## Autores originales

- Iván Moro Cienfuegos
- Gonzalo Sánchez Maroto
