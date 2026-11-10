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

  it('consulta el historial de subastas propias del usuario', () => {
    TestBed.inject(AuthService).currentUser.next({ usuarioID: 3 } as never);
    TestBed.inject(HttpTestingController).expectOne(r => r.url.endsWith('/filtro-mis-subastas') && r.params.get('id') === '3');
  });
});
