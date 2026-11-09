import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
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
});
