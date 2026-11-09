import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExpertLoginComponent } from './expert-login.component';

describe('ExpertLoginComponent', () => {
  let component: ExpertLoginComponent;
  let fixture: ComponentFixture<ExpertLoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpertLoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExpertLoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
