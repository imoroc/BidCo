import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../../services/auth.service';

import { HistorialComponent } from './historial.component';

describe('HistorialComponent', () => {
  function crear(historial: 'pujas' | 'subastas') {
    TestBed.configureTestingModule({
      imports: [HistorialComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { data: { historial } } } }
      ]
    });
    const fixture = TestBed.createComponent(HistorialComponent);
    fixture.detectChanges();
    return fixture;
  }

  const entrar = (id: number) => TestBed.inject(AuthService).currentUser.next({ usuarioID: id } as never);
  const http = () => TestBed.inject(HttpTestingController);

  it('no pide nada mientras no hay usuario', () => {
    crear('pujas');
    http().expectNone(r => r.url.endsWith('/filtro-mis-pujas'));
  });

  it('pide la primera página del filtro cuando llega el usuario', () => {
    crear('pujas');
    entrar(7);
    http().expectOne(r => r.url.endsWith('/filtro-mis-pujas') && r.params.get('id') === '7'
      && r.params.get('page') === '0' && r.params.get('size') === '12');
  });

  it('en el historial de subastas consulta las del usuario y cambia el título', () => {
    const fixture = crear('subastas');
    entrar(3);
    http().expectOne(r => r.url.endsWith('/filtro-mis-subastas') && r.params.get('id') === '3');
    expect(fixture.componentInstance.titulo).toBe('Tus Subastas');
  });
});
