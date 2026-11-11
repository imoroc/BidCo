import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SubastaResponseDTO } from '../../../services/auction.service';
import { AdminModeracionComponent } from './admin-moderacion.component';

describe('AdminModeracionComponent', () => {
  const subasta: SubastaResponseDTO = {
    subastaID: 9, nombreArticulo: 'Reloj', precioFinal: '10', categoria: 'moda', subastaNormal: true, fechaFinal: '2999-01-01T10:00:00Z'
  };

  function crear() {
    TestBed.configureTestingModule({
      imports: [AdminModeracionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    const fixture = TestBed.createComponent(AdminModeracionComponent);
    fixture.detectChanges();
    return fixture;
  }

  const http = () => TestBed.inject(HttpTestingController);

  function buscar(fixture: ReturnType<typeof crear>) {
    fixture.componentInstance.termino = ' reloj ';
    fixture.componentInstance.buscar();
    http().expectOne(r => r.method === 'GET' && r.url.endsWith('/subastas/filtro-nombre') && r.params.get('searchTerm') === 'reloj')
      .flush([subasta]);
    fixture.detectChanges();
  }

  it('busca por nombre y lista los resultados', () => {
    const fixture = crear();
    buscar(fixture);
    expect(fixture.componentInstance.resultados.length).toBe(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Reloj');
  });

  it('no busca con el campo vacío', () => {
    const fixture = crear();
    fixture.componentInstance.termino = '  ';
    fixture.componentInstance.buscar();
    http().expectNone(() => true);
  });

  it('no retira sin motivo', () => {
    const fixture = crear();
    buscar(fixture);
    fixture.componentInstance.elegir(subasta);
    fixture.componentInstance.motivo = '  ';
    fixture.componentInstance.retirar();
    http().expectNone(r => r.method === 'POST');
  });

  it('retira con el motivo, quita la subasta del listado y lo confirma', () => {
    const fixture = crear();
    buscar(fixture);
    fixture.componentInstance.elegir(subasta);
    fixture.componentInstance.motivo = ' Contenido no permitido ';
    fixture.componentInstance.retirar();

    const peticion = http().expectOne(r => r.method === 'POST' && r.url.endsWith('/admin/subastas/9/retirar'));
    expect(peticion.request.body).toEqual({ motivo: 'Contenido no permitido' });
    expect(peticion.request.headers.get('X-Requested-With')).toBe('BidCo');
    peticion.flush(null, { status: 204, statusText: 'No Content' });

    expect(fixture.componentInstance.resultados).toEqual([]);
    expect(fixture.componentInstance.mensaje).toContain('Reloj');
  });

  it('explica que una subasta con ganador no se puede retirar', () => {
    const fixture = crear();
    buscar(fixture);
    fixture.componentInstance.elegir(subasta);
    fixture.componentInstance.motivo = 'x';
    fixture.componentInstance.retirar();
    http().expectOne(r => r.method === 'POST').flush({ message: 'x' }, { status: 409, statusText: 'Conflict' });

    expect(fixture.componentInstance.error).toContain('ganador');
    expect(fixture.componentInstance.enviando).toBeFalse();
    expect(fixture.componentInstance.resultados.length).toBe(1);
  });
});
