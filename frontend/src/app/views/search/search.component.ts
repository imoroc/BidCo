import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';

import { map, distinctUntilChanged, tap } from 'rxjs/operators';

import { HeaderComponent } from '../../components/header/header.component';
import { EstadoListadoComponent } from '../../components/estado-listado/estado-listado.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { CargarMasComponent } from '../../components/cargar-mas/cargar-mas.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { SubastaService, SubastaResponseDTO, Filtro } from '../../services/auction.service';
import { ListadoFiltrado, TAMANO_PAGINA } from '../../utils/listado-filtrado';
import { AuthService } from '../../services/auth.service';
import { AuthExpertService } from '../../services/auth.expert.service';
;

@Component({
  selector: 'app-search',
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
  templateUrl: './search.component.html',
  styleUrl: './search.component.css'
})

export class SearchComponent {
    listado = new ListadoFiltrado<SubastaResponseDTO>(params => this.subastaService.getSubastasFiltradasPorNombre(params, this.searchTerm || ''), inject(DestroyRef), p => p.fechaFinal, TAMANO_PAGINA);
    isLoggedIn = false;
    isExpert = false;
    saldoUser = 0;
    private filtro?: Filtro;
    searchTerm: string|null = "";

    constructor(private authService: AuthService, private expertAuthService: AuthExpertService, private subastaService: SubastaService, private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
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

        this.activatedRoute.queryParamMap.pipe(
            map(params => params.get('search')),
            distinctUntilChanged(),
            tap(searchTerm => this.searchTerm = searchTerm)
        ).subscribe(() => {
            if (this.filtro) this.listado.cargar(this.filtro);
        })
    }

    handleFilterApplied(filtro: Filtro) {
        this.filtro = filtro;
        this.listado.cargar(filtro);
    }
}
