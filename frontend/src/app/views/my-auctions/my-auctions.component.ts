import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { EstadoListadoComponent } from '../../components/estado-listado/estado-listado.component';
import { CargarMasComponent } from '../../components/cargar-mas/cargar-mas.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { AuthService } from '../../services/auth.service';
import { SubastaService, SubastaResponseDTO, Filtro } from '../../services/auction.service';
import { ListadoFiltrado, TAMANO_PAGINA } from '../../utils/listado-filtrado';

@Component({
  selector: 'app-my-auctions',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    RouterLink,
    HeaderComponent,
    FilterComponent,
    ProductItemComponent,
    EstadoListadoComponent,
    CargarMasComponent,
    FooterComponent
],
  templateUrl: './my-auctions.component.html',
  styleUrl: './my-auctions.component.css'
})
export class MyAuctionsComponent {
    listado = new ListadoFiltrado<SubastaResponseDTO>(params => this.subastaService.getSubastasFiltradasMisSubastas(params, this.userId!), inject(DestroyRef), undefined, TAMANO_PAGINA);
    userId: number | null = null;
    private filtro?: Filtro;

    constructor(private authService: AuthService, private subastaService: SubastaService) {}

    ngOnInit() {
        this.authService.currentUser$.subscribe((user) => {
            if (user && user.usuarioID !== this.userId) {
                this.userId = user.usuarioID;
                // El filtro puede haber llegado antes que el usuario.
                if (this.filtro) this.listado.cargar(this.filtro);
            }
        })
    }

    handleFilterApplied(filtro: Filtro) {
        this.filtro = filtro;
        if (this.userId !== null) this.listado.cargar(filtro);
    }
}
