import { Location } from '@angular/common';
import { provideLocationMocks } from '@angular/common/testing';
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

describe('FilterComponent con la URL', () => {
  let location: Location;

  function crear(urlInicial: string, sincronizar: boolean): FilterComponent {
    TestBed.configureTestingModule({
      imports: [FilterComponent],
      providers: [provideRouter([]), provideLocationMocks(), provideHttpClient(), provideHttpClientTesting()]
    });
    location = TestBed.inject(Location);
    location.go(urlInicial);
    const fixture = TestBed.createComponent(FilterComponent);
    fixture.componentRef.setInput('permiteVencidas', true);
    fixture.componentRef.setInput('sincronizarUrl', sincronizar);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('lee el filtro de la URL al abrir la página', () => {
    const componente = crear('/subastas?cat=Moda&orden=asc&min=20', true);
    expect(componente.selectedCategories).toEqual(['Moda']);
    expect(componente.dateOrder).toBe('asc');
    expect(componente.minPrice).toBe(20);
  });

  it('escribe los cambios en la URL y conserva los parámetros ajenos', () => {
    const componente = crear('/subastas?q=reloj', true);
    componente.selectedCategories = ['Hogar'];
    componente.applyFilter();
    expect(location.path()).toBe('/subastas?q=reloj&cat=Hogar');

    componente.limpiarFiltros();
    expect(location.path()).toBe('/subastas?q=reloj');
  });

  it('sin sincronización ni lee ni escribe la URL', () => {
    const componente = crear('/subastas?cat=Moda', false);
    expect(componente.selectedCategories).toEqual([]);
    componente.dateOrder = 'desc';
    componente.applyFilter();
    expect(location.path()).toBe('/subastas?cat=Moda');
  });
});
