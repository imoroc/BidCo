<div align="center">
  <img src="frontend/public/Logo%20BidCo%20Recortado.jpg" alt="Logo de BidCo" height="96" />
  <h1 align="center">BidCo</h1>

  <p align="center">
    <strong>Plataforma web Full-Stack de subastas simuladas, subastas a ciegas y sorteos.</strong>
    <br /><br />
    <a href="https://bid-co.vercel.app/auction"><strong>Ver demostración en vivo</strong></a>
    <br /><br />
    <a href="https://github.com/imoroc/BidCo/actions/workflows/ci-validation.yml"><img alt="CI" src="https://github.com/imoroc/BidCo/actions/workflows/ci-validation.yml/badge.svg?branch=deploy" /></a>
    <a href="LICENSE"><img alt="Licencia MIT" src="https://img.shields.io/badge/licencia-MIT-blue.svg" /></a>
    <img alt="Angular 22" src="https://img.shields.io/badge/Angular-22-dd0031.svg" />
    <img alt="Spring Boot 4.1" src="https://img.shields.io/badge/Spring%20Boot-4.1-6db33f.svg" />
    <img alt="Java 25" src="https://img.shields.io/badge/Java-25-007396.svg" />
  </p>
</div>

---

## Qué es BidCo

BidCo es una aplicación desarrollada para la asignatura Servicios y Sistemas Web. Tiene una arquitectura cliente-servidor: un frontend Angular que consume una API REST de Spring Boot sobre PostgreSQL.

**Qué no es.** BidCo **no maneja dinero real**: no integra pasarelas de pago ni cobra nada. El saldo y los puntos son créditos virtuales que sirven para simular la participación, y el saldo se recarga sin pago (hasta 10 000 por operación).

## Funcionalidades

- **Subastas normales:** la puja visible sube el precio; hay que superar la puja actual.
- **Subastas a ciegas:** nadie ve las pujas hasta que termina; solo cuenta superar el precio base.
- **Sorteos:** se participa aportando puntos; el ganador sale de un sorteo ponderado por los puntos aportados.
- **Perfil de usuario:** datos, saldo, puntos, estadísticas con gráficos y opciones de privacidad (pujas anónimas, estadísticas y perfil visibles u ocultos).
- **Valoraciones de expertos:** comentarios con precio estimado sobre las subastas.
- **Búsqueda y filtros** por nombre, categoría, precio y fecha, con subastas y sorteos vencidos ocultos por defecto.
- **Recuperación de contraseña** por correo electrónico.

### Roles y permisos

| Acción | Visitante | Usuario | Experto | Administración |
|---|:-:|:-:|:-:|:-:|
| Ver listados, filtros y fichas de subastas y sorteos | Sí | Sí | Sí | Sí |
| Leer las valoraciones de expertos | Sí | Sí | Sí | Sí |
| Registrarse y recuperar la contraseña por correo | Sí | Sí | No | No |
| Pujar en subastas y participar en sorteos | No | Sí | No | No |
| Crear subastas | No | Sí | No | No |
| Recargar saldo, editar el perfil y ver estadísticas | No | Sí | No | No |
| Cerrar una subasta o sorteo ya vencido | No | Sí | No | No |
| Escribir y editar valoraciones | No | No | Sí | No |
| Crear sorteos | No | No | No | Sí |
| Dar de alta trabajadores y expertos | No | No | No | Sí |

Usuario y experto se identifican con un token JWT y tienen su propio acceso (`/login` y `/expert`). La administración (`/admin/login`) usa una sesión con cookie y una cabecera anti-CSRF; es una cuenta de trabajador no experto.

## Reglas de negocio

**Alta y saldo.** Una cuenta nueva empieza con saldo 0 y 10 puntos; ambos valores los fija el servidor. El saldo se recarga con un máximo de 10 000 por operación.

**Pujas en subastas.**

- No se puede pujar en una subasta propia, ni antes de su inicio ni después de su cierre.
- En una subasta **normal**, la puja debe superar el precio actual (o el base si aún no hay pujas). El importe de la puja líder queda retenido del saldo y se devuelve cuando alguien la supera.
- En una subasta **a ciegas**, basta con superar el precio base. El precio no se actualiza ni se muestran las pujas hasta el cierre, para no revelar la máxima. Una puja que no supera a la líder se registra sin retener saldo.
- Un usuario no puede gastar el mismo saldo dos veces en paralelo: la fila del usuario se bloquea durante la puja.

**Cierre de subastas.** Pasada la fecha final, gana la puja más alta y el ganador recibe 100 puntos (normal) o 200 (a ciegas). En las ciegas el precio definitivo se publica en ese momento. El cierre es **idempotente**: si ya hay ganador, repetir la petición no vuelve a repartir puntos, y dos cierres simultáneos no pueden puntuar dos veces.

**Sorteos.**

- Solo la administración los crea.
- Cada participación aporta puntos enteros, con un mínimo por sorteo. Los puntos se descuentan al participar.
- Al cerrarse, el ganador se elige con `SecureRandom` y probabilidad proporcional a los puntos aportados.
- El resultado es definitivo: un sorteo cerrado no se puede volver a tirar.

## Stack tecnológico

| Área | Tecnología |
|---|---|
| Frontend | Angular 22, TypeScript 6, CSS |
| Backend | Java 25, Spring Boot 4.1 |
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
