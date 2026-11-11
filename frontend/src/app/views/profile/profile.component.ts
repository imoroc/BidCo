import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
 

import { FooterComponent } from "../../components/footer/footer.component";
import { HeaderComponent } from "../../components/header/header.component";
import { SesionActivaService } from '../../services/sesion-activa.service';

@Component({
  selector: 'app-profile',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [
    RouterLink,
    RouterOutlet,
    FooterComponent,
    HeaderComponent
],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})

export class ProfileComponent {
    isExpert = false;
    
    constructor(private sesionActiva: SesionActivaService) {}

    ngOnInit() {
        //Comprobamos si un experto ha iniciado sesion
        this.sesionActiva.estado$.subscribe(({ esExperto }) => {
            this.isExpert = esExperto;
        });
    }
}
