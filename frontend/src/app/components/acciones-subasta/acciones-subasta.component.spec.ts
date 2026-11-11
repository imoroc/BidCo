import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SubastaResponseDTO } from '../../services/auction.service';
import { AccionesSubastaComponent } from './acciones-subasta.component';

describe('AccionesSubastaComponent', () => {
  const subasta = (fechaFinal: string): SubastaResponseDTO => ({
    subastaID: 7, nombreArticulo: 'Reloj', precioFinal: '10', categoria: 'moda', subastaNormal: true, fechaFinal
  });
  const FUTURO = '2999-01-01T10:00:00Z';

  function crear(datos: SubastaResponseDTO) {
    TestBed.configureTestingModule({
      imports: [AccionesSubastaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    const fixture = TestBed.createComponent(AccionesSubastaComponent);
    fixture.componentInstance.subasta = datos;
    fixture.detectChanges();
    return fixture;
  }

  const http = () => TestBed.inject(HttpTestingController);
  const texto = (fixture: ReturnType<typeof crear>) => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('no muestra acciones en una subasta vencida', () => {
    const fixture = crear(subasta('2000-01-01T10:00:00Z'));
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('muestra editar y cancelar en una subasta abierta', () => {
    const fixture = crear(subasta(FUTURO));
    expect(texto(fixture)).toContain('Editar');
    expect(texto(fixture)).toContain('Cancelar subasta');
  });

  it('pide confirmación antes de cancelar y avisa al listado cuando se retira', () => {
    const fixture = crear(subasta(FUTURO));
    const cancelada: number[] = [];
    fixture.componentInstance.cancelada.subscribe((id: number) => cancelada.push(id));

    fixture.componentInstance.modo = 'confirmando';
    fixture.detectChanges();
    http().expectNone(r => r.method === 'DELETE');

    fixture.componentInstance.cancelar();
    const peticion = http().expectOne(r => r.method === 'DELETE' && r.url.endsWith('/subastas/7'));
    peticion.flush(null, { status: 204, statusText: 'No Content' });

    expect(cancelada).toEqual([7]);
  });

  it('explica el motivo cuando el servidor rechaza la cancelación por tener pujas', () => {
    const fixture = crear(subasta(FUTURO));
    fixture.componentInstance.cancelar();
    http().expectOne(r => r.method === 'DELETE').flush({ message: 'x' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(texto(fixture)).toContain('ya tiene pujas');
    expect(fixture.componentInstance.enviando).toBeFalse();
  });

  it('rellena el formulario con la ficha completa y envía los cambios', () => {
    const fixture = crear(subasta(FUTURO));
    const actualizadas: SubastaResponseDTO[] = [];
    fixture.componentInstance.actualizada.subscribe((s: SubastaResponseDTO) => actualizadas.push(s));

    fixture.componentInstance.abrirEdicion();
    http().expectOne(r => r.method === 'GET' && r.url.endsWith('/subastas/7'))
      .flush({ ...subasta(FUTURO), descripcion: 'Funciona' });
    expect(fixture.componentInstance.modo).toBe('editando');
    expect(fixture.componentInstance.descripcion).toBe('Funciona');

    fixture.componentInstance.nombre = ' Reloj nuevo ';
    fixture.componentInstance.guardar();
    const put = http().expectOne(r => r.method === 'PUT' && r.url.endsWith('/subastas/7'));
    expect(put.request.body).toEqual({ nombreArticulo: 'Reloj nuevo', descripcion: 'Funciona', categoria: 'moda' });
    put.flush({ ...subasta(FUTURO), nombreArticulo: 'Reloj nuevo' });

    expect(actualizadas[0].nombreArticulo).toBe('Reloj nuevo');
    expect(fixture.componentInstance.modo).toBe('ninguno');
  });

  it('no envía el formulario con campos vacíos', () => {
    const fixture = crear(subasta(FUTURO));
    fixture.componentInstance.nombre = ' ';
    fixture.componentInstance.guardar();
    http().expectNone(r => r.method === 'PUT');
  });
});
