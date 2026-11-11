import { TestBed } from '@angular/core/testing';

import { SesionStore } from './sesion.store';

describe('SesionStore', () => {
  let store: SesionStore;

  beforeEach(() => {
    localStorage.clear();
    store = TestBed.inject(SesionStore);
  });

  afterEach(() => localStorage.clear());

  it('sin tokens no hay sesión ni rol', () => {
    expect(store.token()).toBeNull();
    expect(store.rol()).toBeNull();
  });

  it('el token de usuario tiene prioridad sobre el de experto', () => {
    store.guardarTokenExperto('t-experto');
    expect(store.token()).toBe('t-experto');
    expect(store.rol()).toBe('expert');

    store.guardarTokenUsuario('t-usuario');
    expect(store.token()).toBe('t-usuario');
    expect(store.rol()).toBe('user');
  });

  it('guarda y recupera el perfil de cada rol por separado', () => {
    store.guardarPerfilUsuario({ usuarioID: 1 });
    store.guardarPerfilExperto({ usuarioID: 2 });

    expect(JSON.parse(store.perfilUsuario()!)).toEqual({ usuarioID: 1 });
    expect(JSON.parse(store.perfilExperto()!)).toEqual({ usuarioID: 2 });
  });

  it('limpiar un rol no toca la sesión del otro', () => {
    store.guardarTokenUsuario('t-usuario');
    store.guardarPerfilUsuario({ usuarioID: 1 });
    store.guardarTokenExperto('t-experto');

    store.limpiarUsuario();

    expect(store.tokenUsuario()).toBeNull();
    expect(store.perfilUsuario()).toBeNull();
    expect(store.tokenExperto()).toBe('t-experto');
  });
});
