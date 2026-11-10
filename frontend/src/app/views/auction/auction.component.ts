import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';

import { ListadoFiltrado, TAMANO_PAGINA } from '../../utils/listado-filtrado';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { EstadoListadoComponent } from '../../components/estado-listado/estado-listado.component';
import { CargarMasComponent } from '../../components/cargar-mas/cargar-mas.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { SubastaService, SubastaResponseDTO, Filtro } from '../../services/auction.service';
import { AuthService } from '../../services/auth.service';
import { AuthExpertService } from '../../services/auth.expert.service';
;

@Component({
  selector: 'app-auction',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [
    CurrencyPipe,
    RouterLink,
    HeaderComponent,
    FilterComponent,
    ProductItemComponent,
    EstadoListadoComponent,
    CargarMasComponent,
    FooterComponent
],
  templateUrl: './auction.component.html',
  styleUrl: './auction.component.css'
})

export class AuctionComponent {
    listado = new ListadoFiltrado<SubastaResponseDTO>(params => this.subastaService.getSubastasFiltradasNormal(params), inject(DestroyRef), s => s.fechaFinal, TAMANO_PAGINA);
    isLoggedIn = false;
    isExpert = false;
    saldoUser = 0;

    constructor(private authService: AuthService, private expertAuthService: AuthExpertService, private subastaService: SubastaService, private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        //Comprobamos si un usuario tiene la sesion iniciada
        this.authService.isLoggedIn$.subscribe((estado) => {
            this.isLoggedIn = estado;
        });

        this.authService.currentUser$.subscribe((user) => {
            if (user) this.saldoUser = user.saldo ?? 0;
        })

        //Si no hay sesion de usuario se comprueba si hay sesion de experto
        if(!this.isLoggedIn) {
            this.expertAuthService.isLoggedIn$.subscribe((estado) => {
                this.isLoggedIn = estado;
                this.isExpert = estado;
            });
        }
    }

    handleFilterApplied(filtro: Filtro) {
        this.listado.cargar(filtro);
    }
}