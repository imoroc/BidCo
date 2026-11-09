import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  it('añade un aviso con su tipo y mensaje', () => {
    service.exito('Guardado');
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].tipo).toBe('exito');
    expect(service.toasts()[0].mensaje).toBe('Guardado');
  });

  it('cierra un aviso concreto', () => {
    service.error('Fallo');
    service.info('Dato');
    service.cerrar(service.toasts()[0].id);
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].tipo).toBe('info');
  });

  it('retira el aviso al cumplirse su duración', () => {
    jasmine.clock().install();
    try {
      service.info('Temporal', 1000);
      jasmine.clock().tick(1001);
      expect(service.toasts().length).toBe(0);
    } finally {
      jasmine.clock().uninstall();
    }
  });
});
