import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PaginaInformativaComponent } from '../../components/pagina-informativa/pagina-informativa.component';

@Component({
  selector: 'app-terminos',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [PaginaInformativaComponent, RouterLink],
  template: `
    <app-pagina-informativa titulo="Términos de uso" actualizacion="8 de noviembre de 2026">
      <p>
        Al registrarte o usar BidCo aceptas estas condiciones. Si no estás de acuerdo, no utilices
        la plataforma.
      </p>

      <h2>1. Qué es BidCo</h2>
      <p>
        BidCo es un proyecto académico de subastas, subastas a ciegas y sorteos. Es una simulación:
        no se realiza ninguna compraventa real ni se entrega ningún artículo.
      </p>

      <h2>2. Saldo y puntos virtuales</h2>
      <p>
        El saldo y los puntos son virtuales. No se pueden canjear por dinero ni por bienes, no se
        pueden transferir fuera de la plataforma y pueden reiniciarse o modificarse en cualquier
        momento, por ejemplo al restablecer los datos de demostración.
      </p>

      <h2>3. Cuenta y conducta</h2>
      <ul>
        <li>Debes facilitar datos veraces y mantener en secreto tu contraseña.</li>
        <li>Eres responsable de lo que se haga desde tu cuenta.</li>
        <li>No está permitido manipular pujas, usar varias cuentas para ganar ventaja, atacar el servicio ni publicar contenido ilegal u ofensivo.</li>
        <li>Podemos suspender o eliminar cuentas y contenidos que incumplan estas normas.</li>
      </ul>

      <h2>4. Subastas</h2>
      <p>
        Una puja es un compromiso dentro de la plataforma y no se puede retirar. En las subastas
        normales se muestra la puja más alta; en las subastas a ciegas no se revela hasta el cierre.
        Al terminar el plazo, gana la puja más alta y el resultado se registra una sola vez.
      </p>

      <h2>5. Sorteos</h2>
      <p>
        Para participar en un sorteo se aportan puntos, con el mínimo que fije cada sorteo. El
        ganador se elige al azar entre los participantes con un generador criptográficamente seguro.
      </p>

      <h2>6. Contenido que publicas</h2>
      <p>
        Al crear una subasta, y en particular al subir imágenes, declaras que tienes derecho a usar
        ese contenido y permites que se muestre en la plataforma. Puede retirarse si incumple estas
        condiciones.
      </p>

      <h2>7. Disponibilidad y responsabilidad</h2>
      <p>
        El servicio se ofrece tal cual, sin garantías de disponibilidad ni de ausencia de errores, y
        puede tardar en responder o interrumpirse sin aviso. En la medida que permita la ley, los
        responsables del proyecto no responden de daños derivados de su uso.
      </p>

      <h2>8. Cambios</h2>
      <p>
        Estas condiciones pueden actualizarse. La fecha de la parte superior indica la última
        revisión.
      </p>

      <h2>9. Contacto</h2>
      <p>Para cualquier duda, visita la página de <a routerLink="/contacto">contacto</a>.</p>
    </app-pagina-informativa>
  `
})
export class TerminosComponent {}
