import { TestBed } from '@angular/core/testing';

import { AvisoSinConexionComponent } from './aviso-sin-conexion.component';

describe('AvisoSinConexionComponent', () => {
  function crear() {
    const fixture = TestBed.createComponent(AvisoSinConexionComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('aparece al perder la conexión y desaparece al recuperarla', () => {
    const fixture = crear();
    fixture.componentInstance.sinConexion.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.aviso-sin-conexion')).toBeNull();

    window.dispatchEvent(new Event('offline'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.aviso-sin-conexion')).not.toBeNull();

    window.dispatchEvent(new Event('online'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.aviso-sin-conexion')).toBeNull();
  });
});
