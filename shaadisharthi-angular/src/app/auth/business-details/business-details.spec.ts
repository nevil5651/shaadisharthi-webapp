import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ToastrModule } from 'ngx-toastr';
import { ReactiveFormsModule } from '@angular/forms';

import { BusinessDetailsComponent } from './business-details';

describe('BusinessDetailsComponent', () => {
  let component: BusinessDetailsComponent;
  let fixture: ComponentFixture<BusinessDetailsComponent>;

  // Setup test environment before each test
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BusinessDetailsComponent,      // Standalone component
        HttpClientTestingModule,       // Mock HTTP requests
        RouterTestingModule,           // Mock routing
        ToastrModule.forRoot(),        // Mock toast notifications
        ReactiveFormsModule,           // For form testing
      ],
    }).compileComponents();

    // Create component instance
    fixture = TestBed.createComponent(BusinessDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();  // Trigger change detection
  });

  // Basic test: component should be created
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});