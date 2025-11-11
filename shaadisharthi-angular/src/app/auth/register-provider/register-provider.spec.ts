// Angular testing utilities
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';  // Mocks HTTP calls
import { RouterTestingModule } from '@angular/router/testing';          // Mocks routing
import { ToastrModule } from 'ngx-toastr';                               // Mocks toast notifications
import { ReactiveFormsModule } from '@angular/forms';

// The component we're testing
import { RegisterComponent } from './register-provider';

// Test suite for RegisterComponent
describe('RegisterProvider', () => {
  let component: RegisterComponent;       // Instance of the component
  let fixture: ComponentFixture<RegisterComponent>;  // Test wrapper for component

  // Setup before each test
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RegisterComponent,           // Import standalone component directly
        HttpClientTestingModule,     // Mock HTTP (for AuthService)
        RouterTestingModule,         // Mock Router & ActivatedRoute
        ToastrModule.forRoot(),      // Mock toastr notifications
        ReactiveFormsModule,         // Needed for form testing
      ],
    })
    .compileComponents();  // Compile component and templates

    // Create component instance
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();  // Trigger initial data binding
  });

  // Basic test: ensure component is created successfully
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});