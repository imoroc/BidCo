import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el enlace de salto deja el foco en el contenido principal', () => {
    const principal = document.createElement('main');
    document.body.appendChild(principal);
    try {
      const enlace = fixture.nativeElement.querySelector('.skip-link') as HTMLAnchorElement;
      enlace.click();
      expect(document.activeElement).toBe(principal);
    } finally {
      principal.remove();
    }
  });

  it('el botón de secciones refleja si el menú está abierto', () => {
    const boton = fixture.nativeElement.querySelector('.dropbtn') as HTMLButtonElement;
    expect(boton.getAttribute('aria-expanded')).toBe('false');
    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('true');
  });

  it('Escape cierra el menú y devuelve el foco al botón', () => {
    const boton = fixture.nativeElement.querySelector('.dropbtn') as HTMLButtonElement;
    // El botón puede estar oculto por CSS responsive en la ventana de pruebas: se comprueba la llamada.
    const foco = spyOn(boton, 'focus');
    boton.click();
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('false');
    expect(foco).toHaveBeenCalled();
  });
});
