import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';

import { HttpParams } from '@angular/common/http';

import { map, distinctUntilChanged, tap } from 'rxjs/operators';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { SubastaService, SubastaResponseDTO, Filtro } from '../../services/auction.service';
import { ocultarVencidas } from '../../utils/fechas';
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
    FooterComponent
],
  templateUrl: './search.component.html',
  styleUrl: './search.component.css'
})

export class SearchComponent {
    products: SubastaResponseDTO[] = [];
    isLoggedIn = false;
    isExpert = false;
    saldoUser = 0;
    filtro: Filtro = {
        minPrice: 0, 
        maxPrice: 0, 
        categories: [], 
        dateOrder: ""
    };
    searchTerm: string|null = "";
    cargaDeProductos = false;
    noCoincidencias = false;
    primeraCarga = false;

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
            if (this.primeraCarga) this.buscarSubastasPorFiltroYNombre();
        })
    }

    handleFilterApplied(filtro: Filtro) {
        this.filtro = filtro;
        this.buscarSubastasPorFiltroYNombre();
    }

    buscarSubastasPorFiltroYNombre() {
        this.cargaDeProductos = true;
        this.noCoincidencias = false;
        this.primeraCarga = true;

        var camposFiltro = new HttpParams()
        camposFiltro = camposFiltro.append("minPrice", this.filtro.minPrice);
        camposFiltro = camposFiltro.append("maxPrice", this.filtro.maxPrice);
        this.filtro.categories.forEach(categoria => {
            camposFiltro = camposFiltro.append("categorias", categoria);
        })
        camposFiltro = camposFiltro.append("dateOrder", this.filtro.dateOrder)
        
        this.subastaService.getSubastasFiltradasPorNombre(camposFiltro, this.searchTerm || '').subscribe({
            next: data => {
                this.products = ocultarVencidas(data ?? [], p => p.fechaFinal, this.filtro.mostrarVencidas ?? false);
                if (this.products.length === 0) {
                    this.noCoincidencias = true;
                }
                this.cargaDeProductos = false;
            },
            error: err => {
                this.noCoincidencias = true;
                this.cargaDeProductos = false;
                console.error("Error al obtener subastas filtradas: ", err);
            }
        });
    }
}