import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthService } from '../../services/auth.service';

import { MyAuctionsComponent } from './my-auctions.component';

describe('MyAuctionsComponent', () => {
  let component: MyAuctionsComponent;
  let fixture: ComponentFixture<MyAuctionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyAuctionsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MyAuctionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  const esPagina = (r: { url: string; params: { get(n: string): string | null } }, id: string) =>
    r.url.endsWith('/filtro-mis-subastas') && r.params.get('id') === id && r.params.get('page') === '0' && r.params.get('size') === '12';

  it('no pide nada mientras no hay usuario', () => {
    TestBed.inject(HttpTestingController).expectNone(r => r.url.endsWith('/filtro-mis-subastas'));
  });

  it('pide la primera página del filtro cuando llega el usuario', () => {
    TestBed.inject(AuthService).currentUser.next({ usuarioID: 7 } as never);
    TestBed.inject(HttpTestingController).expectOne(r => esPagina(r, '7'));
  });
});
