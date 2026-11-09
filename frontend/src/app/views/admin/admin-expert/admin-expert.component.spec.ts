import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminExpertComponent } from './admin-expert.component';

describe('AdminExpertComponent', () => {
  let component: AdminExpertComponent;
  let fixture: ComponentFixture<AdminExpertComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminExpertComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminExpertComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
