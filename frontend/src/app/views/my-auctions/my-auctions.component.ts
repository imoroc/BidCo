import { HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { EstadoListadoComponent } from '../../components/estado-listado/estado-listado.component';
import { CargarMasComponent } from '../../components/cargar-mas/cargar-mas.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { SubastaService } from '../../services/auction.service';
import { HistorialSubastas } from '../../utils/historial-subastas';

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
export class MyAuctionsComponent extends HistorialSubastas {
    private readonly subastaService = inject(SubastaService);

    protected pedir(params: HttpParams, usuarioId: number) {
        return this.subastaService.getSubastasFiltradasMisSubastas(params, usuarioId);
    }
}
