import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ListadoFiltrado, TAMANO_PAGINA } from '../../utils/listado-filtrado';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { EstadoListadoComponent } from '../../components/estado-listado/estado-listado.component';
import { CargarMasComponent } from '../../components/cargar-mas/cargar-mas.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { AuthService } from '../../services/auth.service';
import { SubastaResponseDTO, SubastaService, Filtro } from '../../services/auction.service';
import { SesionActivaService } from '../../services/sesion-activa.service';

@Component({
  selector: 'app-blind-auction',
  changeDetection: ChangeDetectionStrategy.Eager,
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
  templateUrl: './blind-auction.component.html',
  styleUrl: './blind-auction.component.css'
})
export class BlindAuctionComponent {
    private readonly sesionActiva = inject(SesionActivaService);
    listado = new ListadoFiltrado<SubastaResponseDTO>(params => this.subastaService.getSubastasFiltradasCiega(params), inject(DestroyRef), s => s.fechaFinal, TAMANO_PAGINA);
    isLoggedIn = false;
    isExpert = false;
    saldoUser = 0;

    constructor(private authService: AuthService, private subastaService: SubastaService) {}

    ngOnInit() {
        this.sesionActiva.estado$.subscribe(({ conectado, esExperto }) => {
            this.isLoggedIn = conectado;
            this.isExpert = esExperto;
        });

        this.authService.currentUser$.subscribe((user) => {
            if (user) this.saldoUser = user.saldo ?? 0;
        })

    }

    handleFilterApplied(filtro: Filtro) {
        this.listado.cargar(filtro);
    }
}
