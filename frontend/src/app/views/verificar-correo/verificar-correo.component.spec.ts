import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../../services/auth.service';
import { VerificarCorreoComponent } from './verificar-correo.component';

describe('VerificarCorreoComponent', () => {
  function crear(token: string | null) {
    TestBed.configureTestingModule({
      imports: [VerificarCorreoComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { token } : {}) } } }
      ]
    });
    const refrescar = spyOn(TestBed.inject(AuthService), 'refrescarUsuario');
    const fixture = TestBed.createComponent(VerificarCorreoComponent);
    return { fixture, refrescar, http: TestBed.inject(HttpTestingController) };
  }

  it('sin token en la URL muestra el error y no llama a la API', () => {
    const { fixture, http } = crear(null);

    http.expectNone(r => r.method === 'POST');
    expect(fixture.componentInstance.estado()).toBe('error');
  });

  it('con un token válido verifica y actualiza la sesión', () => {
    const { fixture, http, refrescar } = crear('abc');

    const req = http.expectOne(r => r.method === 'POST' && r.url.endsWith('/usuarios/verificar-correo'));
    expect(req.request.body).toEqual({ token: 'abc' });
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(fixture.componentInstance.estado()).toBe('verificado');
    expect(refrescar).toHaveBeenCalled();
  });

  it('con un enlace caducado muestra el mensaje del servidor', () => {
    const { fixture, http, refrescar } = crear('abc');

    http.expectOne(r => r.method === 'POST').flush(
      { message: 'El enlace no es válido o ha caducado' }, { status: 400, statusText: 'Bad Request' });

    expect(fixture.componentInstance.estado()).toBe('error');
    expect(fixture.componentInstance.mensajeError()).toBe('El enlace no es válido o ha caducado');
    expect(refrescar).not.toHaveBeenCalled();
  });
});
