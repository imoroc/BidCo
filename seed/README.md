# Catálogo de demostración

Puebla la web con subastas, sorteos, usuarios, pujas y comentarios de expertos para que se vea viva. Es una herramienta
manual: no es una migración de Flyway y no la ejecuta ni la aplicación ni el CI.

Qué crea (todo en `catalogo.json`, se puede editar):

- 38 subastas (28 normales y 10 a ciegas) en las seis categorías, con 85 pujas repartidas entre 14 usuarios.
- 10 sorteos con participaciones.
- 4 expertos y 24 comentarios con precio estimado.
- Todo cierra dentro de unos 8 años, así que no caduca.

Las reglas de la aplicación se respetan: solo quien lidera tiene el importe retenido del saldo, en las subastas a ciegas
el precio no se actualiza, crear una subasta da 50 puntos y participar en un sorteo gasta los puntos aportados.

## Requisitos

- Node 18 o superior (no hay dependencias que instalar) y `psql`.
- Las claves de Cloudinary, las mismas que usa el backend (`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`).
- La cadena de conexión externa de la base de datos de PostgreSQL.

## Pasos

1. **Haz una copia de la base de datos.**

2. **Sube las fotos.** Busca una foto libre de cada artículo en Wikimedia Commons (solo licencias CC0, dominio público y CC BY),
   la limita a 1600 px y la sube a Cloudinary en la carpeta `bidco-demo`. Las claves se pasan solo en el comando, no en archivos:

   ```bash
   export CLOUDINARY_CLOUD_NAME=... CLOUDINARY_API_KEY=... CLOUDINARY_API_SECRET=...
   node seed/sembrar.mjs fotos
   ```

   Se puede repetir: no vuelve a subir lo que ya está en `seed/salida/imagenes.json`.

3. **Revisa las fotos** abriendo `seed/salida/revision.html`. Si alguna no te gusta:
   - guarda tu propia foto como `seed/fotos/<slug>.jpg` (el `<slug>` aparece bajo cada foto), o
   - fija una URL en `seed/fotos.json`: `{ "<slug>": "https://..." }`,

   borra su entrada de `seed/salida/imagenes.json` y vuelve a lanzar el paso 2.

4. **Genera el SQL:**

   ```bash
   node seed/sembrar.mjs sql
   ```

5. **Simula.** No guarda nada y muestra el resumen. La contraseña (de al menos 10 caracteres) es la de todas las cuentas de
   demostración; no se guarda en ningún archivo:

   ```bash
   psql "$DATABASE_URL" -v clave_demo='<contraseña>' -f seed/salida/sembrar.sql
   ```

6. **Guarda los cambios** repitiendo el comando con `-v confirmar=1`. Se puede ejecutar más de una vez: cada subasta o sorteo
   solo se crea si no existe ya.

## Cuentas de demostración

- Usuarios: `lucia_ferrer`, `marcos_ortega`, etc. (ver `catalogo.json`), con la contraseña que hayas elegido. Nacen con el correo
  verificado, con 3000 € de saldo y 1500 puntos.
- Expertos: `experto_tecnologia`, `experto_hogar`, `experto_moda` y `experto_deportes`.
- Los correos son `@example.com`, un dominio reservado que nunca recibe correo.

## Tamaño

Pensado para los planes gratuitos: 48 fotos de unos 200 kB (unos 10 MB en Cloudinary) y unas 150 filas en la base de datos.

## Créditos

`seed/salida/creditos.md` lista el autor y la licencia de cada foto de Commons. Las licencias CC BY exigen atribución:
conviene enlazarlo desde el README o el pie de la web. El contenido (productos, pujas y comentarios) es de demostración.
