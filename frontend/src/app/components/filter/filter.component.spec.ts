import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FilterComponent } from './filter.component';

describe('FilterComponent', () => {
  let component: FilterComponent;
  let fixture: ComponentFixture<FilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilterComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra una etiqueta por filtro activo y la quita al pulsarla', () => {
    component.selectedCategories = ['Moda'];
    component.dateOrder = 'asc';
    expect(component.chips.map(c => c.texto)).toEqual(['Moda', 'Termina antes']);

    component.quitarFiltro(component.chips[0]);
    expect(component.selectedCategories).toEqual([]);
    expect(component.chips.length).toBe(1);
  });

  it('limpia todos los filtros y avisa del cambio', () => {
    let emitido = false;
    component.filterChanged.subscribe(() => emitido = true);
    component.selectedCategories = ['Hogar'];
    component.mostrarVencidas = true;

    component.limpiarFiltros();

    expect(component.chips.length).toBe(0);
    expect(emitido).toBeTrue();
  });
});
