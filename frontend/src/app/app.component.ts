import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AvisoVerificacionComponent } from './components/aviso-verificacion/aviso-verificacion.component';
import { ToastComponent } from './components/toast/toast.component';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [RouterOutlet, ToastComponent, AvisoVerificacionComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'Proyecto_SSW';
}
