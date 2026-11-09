import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

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
});
