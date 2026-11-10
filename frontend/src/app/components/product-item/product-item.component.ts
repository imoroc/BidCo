import { CurrencyPipe, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { environment } from '../../../environments/environment'; // Añade la importación
import { TickerService } from '../../services/ticker.service';
import { ahoraMs, cierreMs } from '../../utils/fechas';

@Component({
  selector: 'app-product-item',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe, NgOptimizedImage],
  templateUrl: './product-item.component.html',
  styleUrl: './product-item.component.css'
})
export class ProductItemComponent implements OnInit {
    @Input() product: any;
    isBlindAuction: boolean = false;
    isRaffle: boolean = false;

    private readonly ticker = inject(TickerService);
    private readonly finMs = signal(NaN);

    /** Se recalcula con el reloj compartido del listado, sin un temporizador propio por tarjeta. */
    readonly countdown = computed(() => {
        this.ticker.tick();
        const fin = this.finMs();
        if (Number.isNaN(fin)) {
            return '';
        }
        const distance = fin - ahoraMs();
        if (distance < 0) {
            return 'Finalizada';
        }
        const days = Math.floor(distance / (1000 * 60 * 60 * 24));
        const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
        return `${days}d ${hours}h ${minutes}m`;
    });

    ngOnInit() {
        if (this.product) {
            const esSubasta = this.product.hasOwnProperty('subastaNormal');
            if (esSubasta) {
                this.isBlindAuction = this.product.subastaNormal === false;
            } else {
                this.isRaffle = true;
            }
        }

        const fechaFin = this.isRaffle ? this.product?.fechaFin : this.product?.fechaFinal;
        if (fechaFin) {
            this.finMs.set(cierreMs(fechaFin));
        }
    }
}
