import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { ConfirmedBookingsComponent } from './confirmed-bookings';

describe('ConfirmedBookingsComponent', () => {
  let component: ConfirmedBookingsComponent;
  let fixture: ComponentFixture<ConfirmedBookingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmedBookingsComponent, HttpClientTestingModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfirmedBookingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
