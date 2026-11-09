import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExpertCommentsComponent } from './expert-comments.component';

describe('ExpertCommentsComponent', () => {
  let component: ExpertCommentsComponent;
  let fixture: ComponentFixture<ExpertCommentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpertCommentsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExpertCommentsComponent);
    component = fixture.componentInstance;
    component.comment = { trabajadorID: 1, comentario: 'Comentario de prueba', precioEstimado: 100 };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
