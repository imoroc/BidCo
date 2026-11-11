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
import { SesionActivaService } from '../../services/sesion-activa.service';
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
    private readonly sesionActiva = inject(SesionActivaService);
    listado = new ListadoFiltrado<SubastaResponseDTO>(params => this.subastaService.getSubastasFiltradasNormal(params), inject(DestroyRef), s => s.fechaFinal, TAMANO_PAGINA);
    isLoggedIn = false;
    isExpert = false;
    saldoUser = 0;

    constructor(private authService: AuthService, private subastaService: SubastaService, private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        //Comprobamos si un usuario tiene la sesion iniciada
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