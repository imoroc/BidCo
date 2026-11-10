import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { EstadoListadoComponent } from '../../components/estado-listado/estado-listado.component';
import { CargarMasComponent } from '../../components/cargar-mas/cargar-mas.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { AuthService } from '../../services/auth.service';
import { SubastaService, SubastaResponseDTO, Filtro } from '../../services/auction.service';
import { ListadoFiltrado, TAMANO_PAGINA } from '../../utils/listado-filtrado';

/**
 * Historial paginado del usuario con sesión. La ruta indica cuál (`data.historial`): sus pujas o sus
 * subastas. Se muestran también las vencidas.
 */
@Component({
  selector: 'app-historial',
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
  templateUrl: './historial.component.html',
  styleUrl: './historial.component.css'
})
export class HistorialComponent implements OnInit {
    private readonly authService = inject(AuthService);
    private readonly subastaService = inject(SubastaService);
    private readonly deSubastas = inject(ActivatedRoute).snapshot.data['historial'] === 'subastas';
    private filtro?: Filtro;
    userId: number | null = null;

    readonly titulo = this.deSubastas ? 'Tus Subastas' : 'Tus Pujas';
    readonly listado = new ListadoFiltrado<SubastaResponseDTO>(
        params => this.deSubastas
            ? this.subastaService.getSubastasFiltradasMisSubastas(params, this.userId!)
            : this.subastaService.getSubastasFiltradasMisPujas(params, this.userId!),
        inject(DestroyRef), undefined, TAMANO_PAGINA);

    ngOnInit() {
        this.authService.currentUser$.subscribe(user => {
            if (user && user.usuarioID !== this.userId) {
                this.userId = user.usuarioID;
                // El filtro puede haber llegado antes que el usuario.
                if (this.filtro) this.listado.cargar(this.filtro);
            }
        });
    }

    handleFilterApplied(filtro: Filtro) {
        this.filtro = filtro;
        if (this.userId !== null) this.listado.cargar(filtro);
    }
}
