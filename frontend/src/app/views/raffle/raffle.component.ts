import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, of, switchMap, tap } from 'rxjs';
import { RouterLink } from '@angular/router';

import { HttpParams } from '@angular/common/http';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
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
    FooterComponent
],
  templateUrl: './raffle.component.html',
  styleUrl: './raffle.component.css'
})
export class RaffleComponent {
    products: SorteoResponseDto[] = [];
    cargando = true;
    errorCarga = false;
    private filtros$ = new Subject<HttpParams>();
    private destroyRef = inject(DestroyRef);
    isLoggedIn = false;
    isExpert = false;
    userPoints = 0;

    constructor(private authService: AuthService, private expertAuthService: AuthExpertService, private sorteoService: SorteoService) {}

    ngOnInit() {
        var storedUser;
        // Todas las cargas pasan por un único flujo: la petición nueva cancela a la anterior
        // y nunca se muestra un resultado desfasado.
        this.filtros$.pipe(
            tap(() => { this.cargando = true; this.errorCarga = false; }),
            switchMap(params => this.sorteoService.getSorteosPorFiltro(params).pipe(
                catchError(err => {
                    console.error('Error al obtener el listado: ', err);
                    this.errorCarga = true;
                    return of([] as SorteoResponseDto[]);
                })
            )),
            takeUntilDestroyed(this.destroyRef)
        ).subscribe(data => {
            this.products = data;
            this.cargando = false;
        });

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
        var camposFiltro = new HttpParams()
        camposFiltro = camposFiltro.append("minPrice", filtro.minPrice);
        camposFiltro = camposFiltro.append("maxPrice", filtro.maxPrice);
        filtro.categories.forEach(categoria => {
            camposFiltro = camposFiltro.append("categorias", categoria);
        })
        camposFiltro = camposFiltro.append("dateOrder", filtro.dateOrder)
        
        this.filtros$.next(camposFiltro);
    }
}
