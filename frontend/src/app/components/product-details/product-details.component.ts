import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductoService, SorteoResponseDto, SubastaResponseDto } from '../../services/product.service';
import { SeoService } from '../../services/seo.service';
import { cloudinaryLoader } from '../../utils/cloudinary-loader';
import { jsonLdProducto, resumirDescripcion } from '../../utils/datos-estructurados';
import { cierreMs, estaVencida } from '../../utils/fechas';
import { urlAbsoluta, urlCanonica } from '../../utils/seo';

@Component({
  selector: 'app-product-details',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './product-details.component.html',
  styleUrl: './product-details.component.css'
})
export class ProductDetailsComponent implements OnInit {
    productoSubasta!: SubastaResponseDto;
    productoSorteo!: SorteoResponseDto;
    nombreUsuario = '';
    tipo = '';

    constructor(private route: ActivatedRoute, private productoService: ProductoService, private seo: SeoService, private router: Router) {}

    ngOnInit(): void {
        const productId = Number(this.route.snapshot.paramMap.get('id'));
        this.tipo = this.route.snapshot.data['tipo'];

        switch (this.tipo) {
            case 'subasta':
                this.productoService.getSubastaPorId(productId).subscribe({
                    next: (product) => {
                        this.productoSubasta = product;
                        this.publicarDatosSeo(product);
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

    /** Título, imagen y JSON-LD propios de la subasta (en una ciega solo se publica el precio base). */
    private publicarDatosSeo(product: SubastaResponseDto): void {
        const cierre = cierreMs(product.fechaFinal);
        const imagen = urlAbsoluta(cloudinaryLoader({ src: product.imagen, width: 1200 }));
        const precio = product.subastaNormal === false || !(product.precioFinal > 0)
            ? product.precioInicial
            : product.precioFinal;
        this.seo.fijarProducto({
            titulo: `${product.nombreArticulo} · BidCo`,
            descripcion: resumirDescripcion(product.descripcion) || `Subasta de ${product.nombreArticulo} en BidCo.`,
            imagen,
            datosEstructurados: jsonLdProducto({
                nombre: product.nombreArticulo,
                descripcion: product.descripcion,
                imagen,
                url: urlCanonica(this.router.url),
                precio,
                validaHasta: Number.isNaN(cierre) ? undefined : new Date(cierre).toISOString(),
                vencida: estaVencida(product.fechaFinal)
            })
        });
    }
}
