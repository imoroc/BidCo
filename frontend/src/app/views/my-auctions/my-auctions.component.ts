import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { HttpParams } from '@angular/common/http';

import { HeaderComponent } from '../../components/header/header.component';
import { FilterComponent } from '../../components/filter/filter.component';
import { ProductItemComponent } from '../../components/product-item/product-item.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { AuthService } from '../../services/auth.service';
import { SubastaService, Filtro } from '../../services/auction.service';

@Component({
  selector: 'app-my-auctions',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    RouterLink,
    HeaderComponent,
    FilterComponent,
    ProductItemComponent,
    FooterComponent
],
  templateUrl: './my-auctions.component.html',
  styleUrl: './my-auctions.component.css'
})
export class MyAuctionsComponent {
    products: any;
    userId: number | null = null;

    constructor(private authService: AuthService, private subastaService: SubastaService) {}

    ngOnInit() {
        this.authService.currentUser$.subscribe((user) => {
            if (user) {
                this.userId = user.usuarioID;
                this.subastaService.getSubastasPorCreador(user.usuarioID).subscribe((lista) => {
                    this.products = lista;
                    console.log(this.products);
                });
            }
        })
    }

    handleFilterApplied(filtro: Filtro) {
        var camposFiltro = new HttpParams()
        camposFiltro = camposFiltro.append("minPrice", filtro.minPrice);
        camposFiltro = camposFiltro.append("maxPrice", filtro.maxPrice);
        filtro.categories.forEach(categoria => {
            camposFiltro = camposFiltro.append("categorias", categoria);
        })
        camposFiltro = camposFiltro.append("dateOrder", filtro.dateOrder)
        
        this.subastaService.getSubastasFiltradasMisSubastas(camposFiltro, this.userId!).subscribe({
            next: data => this.products = data,
            error: err => console.error("Error al obtener subastas filtradas: ", err)
        });
    }
}
