import { CurrencyPipe } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { environment } from '../../../environments/environment'; // Añade la importación
import { finDeDiaMs } from '../../utils/fechas';

@Component({
  selector: 'app-product-item',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './product-item.component.html',
  styleUrl: './product-item.component.css'
})
export class ProductItemComponent implements OnInit, OnDestroy {
    @Input() product: any;
    isBlindAuction: boolean = false;
    isRaffle: boolean = false;
    countdown: string = '';
    private intervalId: ReturnType<typeof setInterval> | null = null;

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
            this.setupCountdown(fechaFin);
        }
    }

    ngOnDestroy() {
        this.detenerCuentaAtras();
    }

    private detenerCuentaAtras() {
        if (this.intervalId !== null) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
    }

    setupCountdown(endDateString: string) {
        const endDate = finDeDiaMs(endDateString);

        const actualizar = () => {
            const distance = endDate - new Date().getTime();

            if (distance < 0) {
                this.countdown = "Finalizada";
                this.detenerCuentaAtras();
                return;
            }

            const days = Math.floor(distance / (1000 * 60 * 60 * 24));
            const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));

            this.countdown = `${days}d ${hours}h ${minutes}m`;
        };

        // Se calcula al instante para que la tarjeta no aparezca con el contador vacío.
        this.detenerCuentaAtras();
        actualizar();
        if (this.countdown !== "Finalizada") {
            this.intervalId = setInterval(actualizar, 1000);
        }
    }
}
