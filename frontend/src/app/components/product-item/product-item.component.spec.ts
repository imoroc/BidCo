import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductItemComponent } from './product-item.component';

describe('ProductItemComponent', () => {
  let component: ProductItemComponent;
  let fixture: ComponentFixture<ProductItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductItemComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductItemComponent);
    component = fixture.componentInstance;
    component.product = {
      subastaID: 1,
      nombreArticulo: 'Artículo de prueba',
      imagen: 'prueba.jpg',
      precioInicial: 10,
      precioFinal: 10,
      subastaNormal: true,
      fechaFinal: '2099-01-01T00:00:00'
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra la cuenta atrás en días, horas y minutos', () => {
    expect(component.countdown()).toMatch(/^\d+d \d+h \d+m$/);
  });
});
