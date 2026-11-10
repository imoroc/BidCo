import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthService, UsuarioResponse } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { AvisoVerificacionComponent } from './aviso-verificacion.component';

describe('AvisoVerificacionComponent', () => {
  let fixture: ComponentFixture<AvisoVerificacionComponent>;
  let auth: AuthService;
  let http: HttpTestingController;

  const usuario = (extra: Partial<UsuarioResponse>): UsuarioResponse => ({
    usuarioID: 5, nombreUsuario: 'ana', correoElectronico: 'ana@example.com', ciudad: 'Madrid',
    codigoPostal: '28001', calle: 'Mayor', numeroPiso: 1, pais: 'España', ...extra
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AvisoVerificacionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AvisoVerificacionComponent);
    fixture.detectChanges();
  });

  const aviso = () => fixture.nativeElement.querySelector('.aviso-verificacion');

  it('no se muestra sin sesión', () => {
    expect(aviso()).toBeNull();
  });

  it('no se muestra si el correo está verificado o el dato falta (sesión antigua)', () => {
    auth.currentUser.next(usuario({ correoVerificado: true }));
    fixture.detectChanges();
    expect(aviso()).toBeNull();

    auth.currentUser.next(usuario({}));
    fixture.detectChanges();
    expect(aviso()).toBeNull();
  });

  it('se muestra con el correo cuando la cuenta no está verificada', () => {
    auth.currentUser.next(usuario({ correoVerificado: false }));
    fixture.detectChanges();

    expect(aviso()).not.toBeNull();
    expect(aviso().textContent).toContain('ana@example.com');
  });

  it('reenviar llama a la API y confirma el envío', () => {
    auth.currentUser.next(usuario({ correoVerificado: false }));
    fixture.detectChanges();

    fixture.componentInstance.reenviar();
    http.expectOne(r => r.method === 'POST' && r.url.endsWith('/usuarios/5/reenviar-verificacion'))
      .flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();

    expect(fixture.componentInstance.enviado()).toBeTrue();
    expect(aviso().textContent).toContain('Enlace enviado');
  });

  it('si el servidor lo rechaza muestra el motivo y permite reintentar', () => {
    const error = spyOn(TestBed.inject(ToastService), 'error');
    auth.currentUser.next(usuario({ correoVerificado: false }));
    fixture.detectChanges();

    fixture.componentInstance.reenviar();
    http.expectOne(r => r.method === 'POST').flush(
      { message: 'Demasiadas peticiones' }, { status: 429, statusText: 'Too Many Requests' });

    expect(error).toHaveBeenCalledWith('Demasiadas peticiones');
    expect(fixture.componentInstance.enviado()).toBeFalse();
    expect(fixture.componentInstance.enviando()).toBeFalse();
  });
});
