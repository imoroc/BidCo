import { TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';

import { AuthExpertService } from './auth.expert.service';
import { AuthService } from './auth.service';
import { EstadoSesion, SesionActivaService } from './sesion-activa.service';

describe('SesionActivaService', () => {
  let usuario: BehaviorSubject<boolean>;
  let experto: BehaviorSubject<boolean>;
  let servicio: SesionActivaService;
  let emitidos: EstadoSesion[];

  beforeEach(() => {
    usuario = new BehaviorSubject<boolean>(false);
    experto = new BehaviorSubject<boolean>(false);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isLoggedIn$: usuario.asObservable() } },
        { provide: AuthExpertService, useValue: { isLoggedIn$: experto.asObservable() } }
      ]
    });
    servicio = TestBed.inject(SesionActivaService);
    emitidos = [];
    servicio.estado$.subscribe((e) => emitidos.push(e));
  });

  it('emite al suscribirse y sin sesión no está conectado', () => {
    expect(emitidos).toEqual([{ conectado: false, esExperto: false }]);
  });

  it('una sesión de usuario no es de experto', () => {
    usuario.next(true);
    expect(emitidos.at(-1)).toEqual({ conectado: true, esExperto: false });
  });

  it('una sesión de experto está conectada y es de experto', () => {
    experto.next(true);
    expect(emitidos.at(-1)).toEqual({ conectado: true, esExperto: true });
  });

  it('si hay las dos sesiones manda la de usuario', () => {
    experto.next(true);
    usuario.next(true);
    expect(emitidos.at(-1)).toEqual({ conectado: true, esExperto: false });
  });

  it('no repite emisiones cuando el estado no cambia', () => {
    usuario.next(true);
    const antes = emitidos.length;
    experto.next(false);
    expect(emitidos.length).toBe(antes);
  });

  it('al cerrar la sesión vuelve a no estar conectado', () => {
    usuario.next(true);
    usuario.next(false);
    expect(emitidos.at(-1)).toEqual({ conectado: false, esExperto: false });
  });
});
