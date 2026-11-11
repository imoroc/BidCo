import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfileDatosComponent } from './profile-datos.component';

describe('ProfileDatosComponent', () => {
  let component: ProfileDatosComponent;
  let fixture: ComponentFixture<ProfileDatosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileDatosComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProfileDatosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('cambio de contraseña', () => {
    let tokenAnterior: string | null;

    beforeEach(() => {
      tokenAnterior = localStorage.getItem('authToken');
      localStorage.setItem('authToken', 'token-viejo');
      component.user = { usuarioID: 5 } as never;
      component.currentPassword = 'a'.repeat(12);
      component.newPassword = 'b'.repeat(12);
      component.confirmPassword = 'b'.repeat(12);
    });

    afterEach(() => {
      if (tokenAnterior === null) {
        localStorage.removeItem('authToken');
      } else {
        localStorage.setItem('authToken', tokenAnterior);
      }
    });

    it('guarda el token nuevo para no perder la sesión de este dispositivo', () => {
      component.updatePassword();

      const req = TestBed.inject(HttpTestingController)
        .expectOne(r => r.method === 'PATCH' && r.url.endsWith('/usuarios/5/password'));
      req.flush({ token: 'token-nuevo' });

      expect(localStorage.getItem('authToken')).toBe('token-nuevo');
      expect(component.updatePasswdMessage).toContain('actualizada');
      expect(component.newPassword).toBe('');
    });

    it('conserva el token si el servidor rechaza el cambio', () => {
      component.updatePassword();

      TestBed.inject(HttpTestingController).expectOne(r => r.method === 'PATCH')
        .flush({ message: 'Contraseña actual incorrecta' }, { status: 400, statusText: 'Bad Request' });

      expect(localStorage.getItem('authToken')).toBe('token-viejo');
    });
  });
});
