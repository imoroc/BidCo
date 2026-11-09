import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PaginaInformativaComponent } from '../../components/pagina-informativa/pagina-informativa.component';

@Component({
  selector: 'app-ayuda',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [PaginaInformativaComponent, RouterLink],
  template: `
    <app-pagina-informativa titulo="Ayuda" actualizacion="8 de noviembre de 2026">
      <p>Respuestas rápidas sobre cómo funciona BidCo. Recuerda que todo el dinero y los puntos son virtuales.</p>

      <h2>Subastas y sorteos</h2>
      <details>
        <summary>¿Cómo funciona una subasta normal?</summary>
        <p>Cada subasta tiene un precio inicial y un plazo. Mientras está abierta puedes pujar y se muestra la puja más alta. Al terminar el plazo gana la puja más alta.</p>
      </details>
      <details>
        <summary>¿Qué es una subasta a ciegas?</summary>
        <p>Es una subasta en la que la puja más alta no se revela hasta el cierre. Pujas sin saber cuánto han ofrecido los demás y el precio definitivo se publica al terminar.</p>
      </details>
      <details>
        <summary>¿Cómo funcionan los sorteos?</summary>
        <p>Participas aportando puntos, con el mínimo que fije cada sorteo. Al cerrarse, el ganador se elige al azar entre los participantes con un generador seguro y el resultado se registra una sola vez.</p>
      </details>
      <details>
        <summary>¿Puedo retirar una puja?</summary>
        <p>No. Una puja es un compromiso dentro de la plataforma, así que revisa el importe antes de confirmar.</p>
      </details>

      <h2>Saldo y puntos</h2>
      <details>
        <summary>¿Es dinero real?</summary>
        <p>No. El saldo y los puntos son virtuales, no se pueden canjear por dinero ni por bienes y no se envía ningún artículo.</p>
      </details>
      <details>
        <summary>¿Cómo consigo puntos?</summary>
        <p>Recibes puntos al registrarte, al crear subastas y al ganar subastas; las ciegas dan más que las normales. Los puntos sirven para participar en los sorteos.</p>
      </details>
      <details>
        <summary>¿Cómo recargo saldo?</summary>
        <p>Desde tu perfil. La recarga es virtual y tiene un máximo por operación.</p>
      </details>

      <h2>Cuenta</h2>
      <details>
        <summary>No puedo iniciar sesión</summary>
        <p>Comprueba que usas tu nombre de usuario o tu correo y la contraseña correcta. Tras varios intentos fallidos el acceso se bloquea unos minutos. Si el servidor tarda en responder, espera un momento: puede estar arrancando.</p>
      </details>
      <details>
        <summary>He olvidado mi contraseña</summary>
        <p>La recuperación automática todavía no está disponible. Escríbenos desde la página de <a routerLink="/contacto">contacto</a>.</p>
      </details>
      <details>
        <summary>¿Qué hacéis con mis datos?</summary>
        <p>Lo explicamos en la <a routerLink="/privacidad">política de privacidad</a>, y las normas de uso están en los <a routerLink="/terminos">términos</a>.</p>
      </details>

      <p>¿No encuentras lo que buscas? Visita <a routerLink="/contacto">contacto</a>.</p>
    </app-pagina-informativa>
  `,
  styles: `
    details {
      border-bottom: 1px solid #e0e3e7;
      padding: 10px 0;
    }

    summary {
      cursor: pointer;
      font-weight: 600;
      color: #0d0063;
    }

    summary:focus-visible {
      outline: 2px solid #0d0063;
      outline-offset: 2px;
    }

    details p {
      margin: 8px 0 4px;
    }
  `
})
export class AyudaComponent {}
