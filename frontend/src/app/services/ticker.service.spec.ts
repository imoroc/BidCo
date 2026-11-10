import { fakeAsync, discardPeriodicTasks, TestBed, tick } from '@angular/core/testing';

import { INTERVALO_TICKER_MS, TickerService } from './ticker.service';

describe('TickerService', () => {
  function cambiarVisibilidad(oculta: boolean) {
    spyOnProperty(document, 'hidden', 'get').and.returnValue(oculta);
    document.dispatchEvent(new Event('visibilitychange'));
  }

  it('avanza un pulso por cada intervalo', fakeAsync(() => {
    const ticker = TestBed.inject(TickerService);
    const inicial = ticker.tick();

    tick(INTERVALO_TICKER_MS);
    expect(ticker.tick()).toBe(inicial + 1);

    tick(INTERVALO_TICKER_MS * 2);
    expect(ticker.tick()).toBe(inicial + 3);
    discardPeriodicTasks();
  }));

  it('se detiene con la pestaña oculta y refresca al volver a verse', fakeAsync(() => {
    const ticker = TestBed.inject(TickerService);
    const inicial = ticker.tick();

    cambiarVisibilidad(true);
    tick(INTERVALO_TICKER_MS * 3);
    expect(ticker.tick()).toBe(inicial);

    (Object.getOwnPropertyDescriptor(document, 'hidden')?.get as jasmine.Spy).and.returnValue(false);
    document.dispatchEvent(new Event('visibilitychange'));
    expect(ticker.tick()).toBe(inicial + 1);

    tick(INTERVALO_TICKER_MS);
    expect(ticker.tick()).toBe(inicial + 2);
    discardPeriodicTasks();
  }));
});
