import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductComponent } from './product.component';

describe('ProductComponent', () => {
  let component: ProductComponent;
  let fixture: ComponentFixture<ProductComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('tiene una región aria-live educada para los avisos', () => {
    const region = fixture.nativeElement.querySelector('[aria-live="polite"]');
    expect(region).not.toBeNull();
  });

  it('no anuncia el precio en la primera carga', () => {
    component.anunciarCambioDePrecio(undefined, 100);
    expect(component.anuncio).toBe('');
  });

  it('no anuncia nada si el precio no ha cambiado', () => {
    component.anunciarCambioDePrecio(100, 100);
    expect(component.anuncio).toBe('');
  });

  it('anuncia el nuevo precio cuando cambia', () => {
    component.anunciarCambioDePrecio(100, 112.5);
    expect(component.anuncio).toContain('El precio actual es');
    expect(component.anuncio).toContain('112,50');
  });

  it('no anuncia el precio en una subasta a ciegas', () => {
    component.isBlindAuction = true;
    component.anunciarCambioDePrecio(100, 120);
    expect(component.anuncio).toBe('');
  });
});
