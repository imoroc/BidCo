import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { Filtro, SubastaService } from '../../services/auction.service';

@Component({
  selector: 'app-filter',
  changeDetection: ChangeDetectionStrategy.Eager,
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
    /** Solo afecta a pantallas estrechas: en escritorio el panel está siempre visible. */
    abierto = false;

    get filtrosActivos(): number {
        return this.selectedCategories.length + (this.dateOrder ? 1 : 0) + (this.mostrarVencidas ? 1 : 0);
    }

    /** Filtros activos como etiquetas que se pueden quitar de una en una. */
    get chips(): { tipo: 'categoria' | 'fecha' | 'vencidas'; valor: string; texto: string }[] {
        const chips: { tipo: 'categoria' | 'fecha' | 'vencidas'; valor: string; texto: string }[] =
            this.selectedCategories.map(c => ({ tipo: 'categoria' as const, valor: c, texto: c }));
        if (this.dateOrder) {
            chips.push({ tipo: 'fecha', valor: this.dateOrder, texto: this.dateOrder === 'asc' ? 'Termina antes' : 'Termina después' });
        }
        if (this.mostrarVencidas) {
            chips.push({ tipo: 'vencidas', valor: '', texto: 'Con vencidas' });
        }
        return chips;
    }

    quitarFiltro(chip: { tipo: string; valor: string }) {
        if (chip.tipo === 'categoria') {
            this.selectedCategories = this.selectedCategories.filter(c => c !== chip.valor);
        } else if (chip.tipo === 'fecha') {
            this.dateOrder = '';
        } else {
            this.mostrarVencidas = false;
        }
        this.applyFilter();
    }

    /** Activa "Mostrar vencidas" desde fuera, por ejemplo desde el aviso de un listado vacío. */
    mostrarFinalizadas() {
        this.mostrarVencidas = true;
        this.applyFilter();
    }

    limpiarFiltros() {
        this.selectedCategories = [];
        this.dateOrder = '';
        this.mostrarVencidas = false;
        this.applyFilter();
    }

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
