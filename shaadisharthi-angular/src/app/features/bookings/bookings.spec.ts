// Import testing utilities from Angular
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

// Import the component we're testing
import { BookingsComponent } from './bookings';

// Start test suite for BookingsComponent
describe('BookingsComponent', () => {
  let component: BookingsComponent;
  let fixture: ComponentFixture<BookingsComponent>;

  // Setup before each test
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BookingsComponent, HttpClientTestingModule]  // Include component + mock HTTP
    })
    .compileComponents();  // Compile the component

    // Create component instance
    fixture = TestBed.createComponent(BookingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();  // Trigger initial change detection
  });

  // Basic test: ensure component is created successfully
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});