import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProductoService, SorteoResponseDto, SubastaResponseDto } from '../../services/product.service';

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [],
  templateUrl: './product-details.component.html',
  styleUrl: './product-details.component.css'
})
export class ProductDetailsComponent implements OnInit {
    productoSubasta!: SubastaResponseDto;
    productoSorteo!: SorteoResponseDto;
    nombreUsuario = '';
    tipo = '';

    constructor(private route: ActivatedRoute, private productoService: ProductoService) {}

    ngOnInit(): void {
        const productId = Number(this.route.snapshot.paramMap.get('id'));
        this.tipo = this.route.snapshot.data['tipo'];

        switch (this.tipo) {
            case 'subasta':
                this.productoService.getSubastaPorId(productId).subscribe({
                    next: (product) => {
                        this.productoSubasta = product;
                        this.productoService.getPerfilPublicoPorId(product.creadorId).subscribe({
                            next: (profile) => {
                                this.nombreUsuario = profile.privacidadAnonimoPujas
                                    ? 'Anónimo'
                                    : profile.nombreUsuario;
                            },
                            error: () => this.nombreUsuario = 'Usuario'
                        });
                    }
                });
                break;
            case 'sorteo':
                this.productoService.getSorteoPorId(productId).subscribe({
                    next: (product) => this.productoSorteo = product
                });
                break;
        }
    }
}
