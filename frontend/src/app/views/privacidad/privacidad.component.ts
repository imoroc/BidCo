import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PaginaInformativaComponent } from '../../components/pagina-informativa/pagina-informativa.component';

@Component({
  selector: 'app-privacidad',
  standalone: true,
  imports: [PaginaInformativaComponent, RouterLink],
  template: `
    <app-pagina-informativa titulo="Política de privacidad" actualizacion="8 de noviembre de 2026">
      <p>
        Esta página explica qué datos trata BidCo, para qué y con quién se comparten. BidCo es un
        proyecto académico sin fines comerciales.
      </p>

      <h2>1. Responsable</h2>
      <p>
        El responsable es el autor del proyecto, cuyo repositorio es
        <a href="https://github.com/imoroc/BidCo" target="_blank" rel="noopener noreferrer">github.com/imoroc/BidCo</a>.
        Puedes contactar desde la página de <a routerLink="/contacto">contacto</a>.
      </p>

      <h2>2. Qué datos tratamos</h2>
      <ul>
        <li>Datos de la cuenta: nombre de usuario, correo electrónico y contraseña, que se guarda cifrada con BCrypt y nunca en claro.</li>
        <li>Dirección: país, ciudad, código postal, calle, número y letra del piso.</li>
        <li>Actividad: saldo y puntos virtuales, pujas, subastas creadas, participaciones en sorteos y comentarios.</li>
        <li>Preferencias de privacidad que configures en tu perfil.</li>
        <li>Imágenes que subas al crear una subasta.</li>
      </ul>

      <h2>3. Para qué los usamos</h2>
      <p>
        Únicamente para crear tu cuenta, permitirte iniciar sesión y hacer funcionar las subastas y
        los sorteos. No hacemos perfiles publicitarios ni vendemos tus datos.
      </p>

      <h2>4. Base legal</h2>
      <p>
        El tratamiento se basa en tu consentimiento al registrarte y en la necesidad de prestar el
        servicio que solicitas.
      </p>

      <h2>5. Cuánto tiempo los conservamos</h2>
      <p>
        Mientras tu cuenta exista. Si pides que se elimine, borraremos tus datos salvo los que deban
        conservarse por obligación legal.
      </p>

      <h2>6. Con quién se comparten</h2>
      <p>Para alojar el servicio usamos proveedores que tratan datos en nuestro nombre:</p>
      <ul>
        <li>Vercel: aloja la aplicación web.</li>
        <li>Render: aloja la API.</li>
        <li>Aiven: aloja la base de datos.</li>
        <li>Cloudinary: almacena las imágenes de las subastas.</li>
      </ul>
      <p>Algunos de estos proveedores pueden tratar datos fuera del Espacio Económico Europeo.</p>

      <h2>7. Almacenamiento en tu navegador</h2>
      <p>
        No usamos cookies de seguimiento ni de publicidad. Para mantener tu sesión guardamos en el
        almacenamiento local del navegador tu token de acceso y los datos básicos de tu perfil, que
        se eliminan al cerrar sesión. El área de administración usa una cookie de sesión técnica.
      </p>

      <h2>8. Tus derechos</h2>
      <p>
        Puedes solicitar el acceso, la rectificación, la supresión, la limitación, la oposición y la
        portabilidad de tus datos, y retirar tu consentimiento en cualquier momento. Para ejercerlos
        escribe desde la página de <a routerLink="/contacto">contacto</a>, sin publicar datos personales
        en canales abiertos. Si consideras que no se han respetado, puedes reclamar ante la Agencia
        Española de Protección de Datos (<a href="https://www.aepd.es" target="_blank" rel="noopener noreferrer">aepd.es</a>).
      </p>

      <h2>9. Seguridad</h2>
      <p>
        Aplicamos medidas razonables: contraseñas cifradas, conexión HTTPS, sesiones con caducidad
        y límite de intentos de acceso. Ningún sistema es completamente infalible.
      </p>

      <h2>10. Cambios</h2>
      <p>Esta política puede actualizarse; la fecha superior indica la última revisión.</p>
    </app-pagina-informativa>
  `
})
export class PrivacidadComponent {}
