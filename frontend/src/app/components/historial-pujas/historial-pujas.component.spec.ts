import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { HistorialPujasComponent } from './historial-pujas.component';
import { ProductoService, PujaHistorialDto } from '../../services/product.service';

describe('HistorialPujasComponent', () => {
  let pedidas: number[];
  let respuesta: Observable<PujaHistorialDto[]>;

  function crear(): ComponentFixture<HistorialPujasComponent> {
    TestBed.configureTestingModule({
      imports: [HistorialPujasComponent],
      providers: [{
        provide: ProductoService,
        useValue: { obtenerHistorialPujas: (id: number) => { pedidas.push(id); return respuesta; } }
      }]
    });
    const f = TestBed.createComponent(HistorialPujasComponent);
    f.componentRef.setInput('subastaId', 9);
    f.detectChanges();
    return f;
  }

  beforeEach(() => {
    pedidas = [];
    respuesta = of([]);
  });

  it('muestra las pujas con el pujador enmascarado', () => {
    respuesta = of([
      { pujador: 'M***', importe: 30, fecha: '2026-11-11' },
      { pujador: 'Anónimo', importe: 20, fecha: '2026-11-10' }
    ]);
    const f = crear();

    const filas = f.nativeElement.querySelectorAll('.historial-fila');
    expect(pedidas).toEqual([9]);
    expect(filas.length).toBe(2);
    expect(filas[0].textContent).toContain('M***');
    expect(filas[0].textContent).toContain('30');
  });

  it('invita a pujar cuando todavía no hay pujas', () => {
    const f = crear();

    expect(f.nativeElement.textContent).toContain('Todavía no hay pujas');
  });

  it('recarga el historial cuando cambia el precio actual', () => {
    const f = crear();

    f.componentRef.setInput('precioActual', 75);
    f.detectChanges();

    expect(pedidas).toEqual([9, 9]);
  });

  it('avisa si no se puede cargar y no hay nada que mostrar', () => {
    respuesta = throwError(() => new Error('fallo'));
    const f = crear();

    expect(f.nativeElement.querySelector('[role="status"]').textContent).toContain('No se ha podido cargar');
  });
});
