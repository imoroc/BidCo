import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfileEstadisticasComponent } from './profile-estadisticas.component';

describe('ProfileEstadisticasComponent', () => {
  let component: ProfileEstadisticasComponent;
  let fixture: ComponentFixture<ProfileEstadisticasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileEstadisticasComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProfileEstadisticasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
