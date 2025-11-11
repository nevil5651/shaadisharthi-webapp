import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { Dashboard } from './dashboard';  // Import the Dashboard component

// Test suite for Dashboard component
describe('Dashboard', () => {
  let component: Dashboard;                  // Component instance
  let fixture: ComponentFixture<Dashboard>;  // Test fixture

  // Setup before each test
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        Dashboard,                  // Standalone Dashboard component
        HttpClientTestingModule,    // Mock HTTP requests (for ApiService)
        RouterTestingModule         // Mock routing (if needed for links)
      ]
    })
    .compileComponents();  // Compile the component

    fixture = TestBed.createComponent(Dashboard);  // Create component
    component = fixture.componentInstance;         // Get instance
    fixture.detectChanges();                       // Trigger initial render
  });

  // Basic test to check if component creates successfully
  it('should create', () => {
    expect(component).toBeTruthy();  // Should not be null/undefined
  });
});