import { Component, DestroyRef, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ListadoFiltrado } from '../../utils/listado-filtrado';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { EstadoListadoComponent } from '../../components/estado-listado/estado-listado.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { AuthService } from '../../services/auth.service';
import { SorteoService, SorteoResponseDto, Filtro } from '../../services/raffle.service';
import { AuthExpertService } from '../../services/auth.expert.service';

@Component({
  selector: 'app-raffle',
  imports: [
    RouterLink,
    HeaderComponent,
    FilterComponent,
    ProductItemComponent,
    EstadoListadoComponent,
    FooterComponent
],
  templateUrl: './raffle.component.html',
  styleUrl: './raffle.component.css'
})
export class RaffleComponent {
    listado = new ListadoFiltrado<SorteoResponseDto>(params => this.sorteoService.getSorteosPorFiltro(params), inject(DestroyRef), s => s.fechaFin);
    isLoggedIn = false;
    isExpert = false;
    userPoints = 0;

    constructor(private authService: AuthService, private expertAuthService: AuthExpertService, private sorteoService: SorteoService) {}

    ngOnInit() {
        var storedUser;
        this.authService.isLoggedIn$.subscribe((estado) => {
            this.isLoggedIn = estado;
            if(estado) storedUser = localStorage.getItem('authUser');
        });
        
        //Si no hay sesion de usuario se comprueba si hay sesion de experto
        if(!this.isLoggedIn) {
            this.expertAuthService.isLoggedIn$.subscribe((estado) => {
                this.isLoggedIn = estado;
                this.isExpert = estado;
            });
        }

        if(storedUser) {
            const user = JSON.parse(storedUser);
            this.userPoints = user.puntos;
        }
        
    }

    handleFilterApplied(filtro: Filtro) {
        this.listado.cargar(filtro);
    }
}
