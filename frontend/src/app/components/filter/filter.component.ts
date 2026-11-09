import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { Filtro, SubastaService } from '../../services/auction.service';

@Component({
  selector: 'app-filter',
  imports: [FormsModule, CommonModule],
  templateUrl: './filter.component.html',
  styleUrls: ['./filter.component.css']
})
export class FilterComponent {
    minPossible: number = 0;
    maxPossible: number = 10000;
    minPrice: number = 0;  // Valor inicial, ajustable según necesidad
    maxPrice: number = 10000;  // Valor inicial, ajustable según necesidad

    categories: string[] = ['Tecnología', 'Hogar', 'Moda', 'Deportes', 'Juguetes', 'Otros'];
    selectedCategories: string[] = [];
    dateOrder: string = '';
    mostrarVencidas: boolean = false;

    /** Solo los listados públicos ofrecen ver los vencidos; los historiales ya los muestran. */
    @Input() permiteVencidas: boolean = false;

    constructor(private subastaService: SubastaService) {}

    @Output() filterChanged = new EventEmitter<Filtro>();

    filtro: Filtro = {
        minPrice: 0, 
        maxPrice: 0, 
        categories: [], 
        dateOrder: "",
        mostrarVencidas: false
    };

    ngOnInit() {
        this.applyFilter();
    }

    updateCategoryFilter(event: any) {
        const category = event.target.value;
        if (event.target.checked) {
            this.selectedCategories.push(category);
        } else {
            this.selectedCategories = this.selectedCategories.filter(c => c !== category);
        }
        this.applyFilter();
    }

    /**
     * Método de validación: Se pasa 'min' o 'max' según cuál valor fue modificado.
     * Si el usuario cambia el mínimo y este supera el máximo, se ajusta el máximo.
     * Si cambia el máximo y este es menor que el mínimo, se ajusta el mínimo.
     */
    applyFilter(changedField?: 'min' | 'max') {
        if (changedField === 'min' && this.minPrice > this.maxPrice) {
            this.maxPrice = this.minPrice;
        } else if (changedField === 'max' && this.maxPrice < this.minPrice) {
            this.minPrice = this.maxPrice;
        }

        //Que no se salga de los limites
        if (this.minPrice < this.minPossible) this.minPrice = this.minPossible;
        if (this.maxPrice > this.maxPossible) this.maxPrice = this.maxPossible;
        if (this.minPrice > this.maxPossible) this.minPrice = this.maxPossible; 
        if (this.maxPrice < this.minPossible) this.maxPrice = this.minPossible;
        
        this.filterChanged.emit({
            minPrice: this.minPrice,
            maxPrice: this.maxPrice,
            categories: [...this.selectedCategories],
            dateOrder: this.dateOrder,
            mostrarVencidas: this.permiteVencidas && this.mostrarVencidas
        });
    }
}
