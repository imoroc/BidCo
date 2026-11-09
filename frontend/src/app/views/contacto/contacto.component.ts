import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PaginaInformativaComponent } from '../../components/pagina-informativa/pagina-informativa.component';

@Component({
  selector: 'app-contacto',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [PaginaInformativaComponent, RouterLink],
  template: `
    <app-pagina-informativa titulo="Contacto" actualizacion="8 de noviembre de 2026">
      <p>
        BidCo es un proyecto académico. El canal de contacto es el repositorio público del proyecto
        en GitHub.
      </p>

      <h2>Dudas, errores y sugerencias</h2>
      <p>
        Abre una incidencia en
        <a href="https://github.com/imoroc/BidCo/issues" target="_blank" rel="noopener noreferrer">GitHub Issues</a>
        y cuéntanos qué ocurre, en qué página y desde qué dispositivo. Las incidencias son públicas:
        no incluyas contraseñas, datos personales ni capturas con información privada.
      </p>

      <h2>Problemas de seguridad</h2>
      <p>
        Si has encontrado una vulnerabilidad, no la publiques en una incidencia abierta. Sigue las
        indicaciones de la
        <a href="https://github.com/imoroc/BidCo/security/policy" target="_blank" rel="noopener noreferrer">política de seguridad</a>
        del repositorio.
      </p>

      <h2>Tus datos personales</h2>
      <p>
        Para ejercer tus derechos sobre tus datos consulta la
        <a routerLink="/privacidad">política de privacidad</a>.
      </p>

      <h2>Importante</h2>
      <p>
        Nadie de BidCo te pedirá nunca tu contraseña. Todo el dinero y los puntos de la plataforma
        son virtuales y no tienen valor real.
      </p>
    </app-pagina-informativa>
  `
})
export class ContactoComponent {}
