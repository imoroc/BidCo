import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthService } from '../../../services/auth.service';
import { ProfilePrivacidadComponent } from './profile-privacidad.component';

describe('ProfilePrivacidadComponent', () => {
  let component: ProfilePrivacidadComponent;
  let fixture: ComponentFixture<ProfilePrivacidadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfilePrivacidadComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProfilePrivacidadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('eliminar cuenta', () => {
    const clave = 'x'.repeat(12);
    let http: HttpTestingController;
    let auth: AuthService;

    beforeEach(() => {
      http = TestBed.inject(HttpTestingController);
      auth = TestBed.inject(AuthService);
      auth.currentUser.next({ usuarioID: 5 } as never);
    });

    it('no llama a la API si falta la contraseña', () => {
      component.abrirEliminar();
      component.confirmarEliminar();

      http.expectNone(req => req.method === 'DELETE');
      expect(component.eliminando).toBeFalse();
    });

    it('envía la contraseña en el cuerpo, cierra la sesión y vuelve al inicio', () => {
      const navigate = spyOn(TestBed.inject(Router), 'navigate');
      const logout = spyOn(auth, 'logout');
      component.abrirEliminar();
      component.contrasenaEliminar = clave;

      component.confirmarEliminar();

      const req = http.expectOne(r => r.method === 'DELETE' && r.url.endsWith('/usuarios/5'));
      expect(req.request.body).toEqual({ contrasena: clave });
      req.flush(null, { status: 204, statusText: 'No Content' });

      expect(logout).toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/']);
    });

    it('conserva la sesión y permite reintentar si el servidor lo rechaza', () => {
      const logout = spyOn(auth, 'logout');
      component.abrirEliminar();
      component.contrasenaEliminar = clave;

      component.confirmarEliminar();
      http.expectOne(r => r.method === 'DELETE').flush(
        { message: 'No puedes eliminar la cuenta mientras tengas subastas o sorteos sin cerrar' },
        { status: 409, statusText: 'Conflict' });

      expect(logout).not.toHaveBeenCalled();
      expect(component.eliminando).toBeFalse();
      expect(component.mostrandoEliminar).toBeTrue();
    });

    it('cancelar limpia el formulario', () => {
      component.abrirEliminar();
      component.contrasenaEliminar = clave;

      component.cancelarEliminar();

      expect(component.mostrandoEliminar).toBeFalse();
      expect(component.contrasenaEliminar).toBe('');
    });
  });
});
