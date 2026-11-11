import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';

import { ProductDetailsComponent } from './product-details.component';
import { ProductoService, SubastaResponseDto } from '../../services/product.service';
import { EstadoSubasta, SubastaDirectoService } from '../../services/subasta-directo.service';

describe('ProductDetailsComponent', () => {
  let component: ProductDetailsComponent;
  let fixture: ComponentFixture<ProductDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductDetailsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('ProductDetailsComponent en directo', () => {
  const futuro = new Date(Date.now() + 3_600_000).toISOString();
  const subasta = {
    subastaID: 9, nombreArticulo: 'Reloj', descripcion: 'Descripción', fechaInicial: '2026-01-01T00:00:00Z',
    fechaFinal: futuro, precioInicial: 10, precioFinal: 0, imagen: 'reloj.jpg', creadorId: 1,
    comentarios: [], pujas: [], privacidad_anonimo: false, subastaNormal: true
  } as SubastaResponseDto;

  let estados: Subject<EstadoSubasta>;
  let seguidas: number[];

  function crear(producto: SubastaResponseDto): ComponentFixture<ProductDetailsComponent> {
    estados = new Subject<EstadoSubasta>();
    seguidas = [];
    TestBed.configureTestingModule({
      imports: [ProductDetailsComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '9' }, data: { tipo: 'subasta' } } } },
        {
          provide: ProductoService,
          useValue: {
            getSubastaPorId: () => of(producto),
            getPerfilPublicoPorId: () => of({ nombreUsuario: 'Ana', privacidadAnonimoPujas: false })
          }
        },
        { provide: SubastaDirectoService, useValue: { seguir: (id: number) => { seguidas.push(id); return estados; } } }
      ]
    });
    const f = TestBed.createComponent(ProductDetailsComponent);
    f.detectChanges();
    return f;
  }

  it('sigue en directo una subasta normal abierta y refleja la puja líder', () => {
    const f = crear(subasta);
    expect(seguidas).toEqual([9]);
    expect(f.nativeElement.textContent).toContain('Precio Base');

    estados.next({ precioFinal: 75, fechaFinal: futuro });
    f.detectChanges();

    expect(f.componentInstance.precioActual).toBe(75);
    expect(f.nativeElement.textContent).toContain('Puja actual');
    expect(f.componentInstance.cierreAmpliado).toBeFalse();
  });

  it('avisa cuando el anti-sniping amplía el cierre', () => {
    const f = crear(subasta);
    const masTarde = new Date(Date.parse(futuro) + 300_000).toISOString();

    estados.next({ precioFinal: 80, fechaFinal: masTarde });
    f.detectChanges();

    expect(f.componentInstance.cierre).toBe(masTarde);
    expect(f.componentInstance.cierreAmpliado).toBeTrue();
    expect(f.nativeElement.textContent).toContain('ampliado');
  });

  it('no se conecta si es a ciegas ni si ya venció', () => {
    crear({ ...subasta, subastaNormal: false });
    expect(seguidas).toEqual([]);

    TestBed.resetTestingModule();
    crear({ ...subasta, fechaFinal: '2020-01-01T00:00:00Z' });
    expect(seguidas).toEqual([]);
  });

  it('cierra la conexión al destruir la ficha', () => {
    const f = crear(subasta);
    expect(estados.observed).toBeTrue();

    f.destroy();
    expect(estados.observed).toBeFalse();
  });
});
