import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BlindAuctionComponent } from './blind-auction.component';

describe('BlindAuctionComponent', () => {
  let component: BlindAuctionComponent;
  let fixture: ComponentFixture<BlindAuctionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BlindAuctionComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BlindAuctionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
