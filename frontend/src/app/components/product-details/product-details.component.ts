import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductoService, SorteoResponseDto, SubastaResponseDto } from '../../services/product.service';
import { SeoService } from '../../services/seo.service';
import { EstadoSubasta, SubastaDirectoService } from '../../services/subasta-directo.service';
import { cloudinaryLoader } from '../../utils/cloudinary-loader';
import { jsonLdProducto, resumirDescripcion } from '../../utils/datos-estructurados';
import { ZONA_HORARIA, cierreMs, estaVencida } from '../../utils/fechas';
import { urlAbsoluta, urlCanonica } from '../../utils/seo';

@Component({
  selector: 'app-product-details',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './product-details.component.html',
  styleUrl: './product-details.component.css'
})
export class ProductDetailsComponent implements OnInit {
    productoSubasta!: SubastaResponseDto;
    productoSorteo!: SorteoResponseDto;
    nombreUsuario = '';
    tipo = '';
    /** Zona en la que el servidor fija el cierre (para mostrarlo igual en cualquier dispositivo). */
    readonly zona = ZONA_HORARIA;
    /** Puja líder y cierre vigentes: se actualizan en directo mientras la subasta sigue abierta. */
    precioActual = 0;
    cierre = '';
    /** true si el cierre se ha ampliado (anti-sniping) desde que se abrió la ficha. */
    cierreAmpliado = false;

    private readonly destroyRef = inject(DestroyRef);
    private readonly directo = inject(SubastaDirectoService);

    constructor(private route: ActivatedRoute, private productoService: ProductoService, private seo: SeoService, private router: Router) {}

    ngOnInit(): void {
        const productId = Number(this.route.snapshot.paramMap.get('id'));
        this.tipo = this.route.snapshot.data['tipo'];

        switch (this.tipo) {
            case 'subasta':
                this.productoService.getSubastaPorId(productId).subscribe({
                    next: (product) => {
                        this.productoSubasta = product;
                        this.precioActual = product.precioFinal;
                        this.cierre = product.fechaFinal;
                        this.publicarDatosSeo(product);
                        this.seguirEnDirecto(product);
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

    /** Solo las subastas normales y abiertas emiten; las ciegas no revelan el precio y las vencidas ya no cambian. */
    private seguirEnDirecto(product: SubastaResponseDto): void {
        if (product.subastaNormal === false || estaVencida(product.fechaFinal)) {
            return;
        }
        this.directo.seguir(product.subastaID)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(estado => this.aplicarEstado(estado));
    }

    private aplicarEstado(estado: EstadoSubasta): void {
        if (estado.precioFinal !== null) {
            this.precioActual = estado.precioFinal;
        }
        if (estado.fechaFinal !== null) {
            if (cierreMs(estado.fechaFinal) > cierreMs(this.cierre)) {
                this.cierreAmpliado = true;
            }
            this.cierre = estado.fechaFinal;
        }
    }
}
