import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Footer } from './footer';  // Import the Footer component (note: class name is Footer)

// Test suite for the Footer component
describe('Footer', () => {
  let component: Footer;                    // Reference to the component instance
  let fixture: ComponentFixture<Footer>;    // Wrapper to interact with component

  // Setup before each test
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Footer]  // Declare the standalone Footer component
    })
    .compileComponents();  // Compile the component and its template

    fixture = TestBed.createComponent(Footer);  // Create instance
    component = fixture.componentInstance;      // Get the component class
    fixture.detectChanges();                    // Trigger change detection
  });

  // Basic test: checks if component is created successfully
  it('should create', () => {
    expect(component).toBeTruthy();  // Should exist and not be null
  });
});