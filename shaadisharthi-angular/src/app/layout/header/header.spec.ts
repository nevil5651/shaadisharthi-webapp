import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ToastrModule } from 'ngx-toastr';

import { Header } from './header';  // Import the Header component

// Test suite for HeaderComponent
describe('HeaderComponent', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;

  // Setup test environment before each test
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        Header,                     // The standalone component
        HttpClientTestingModule,    // Mock HTTP calls (used in AuthService)
        RouterTestingModule,        // Mock routing
        ToastrModule.forRoot()      // Mock toast notifications
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    fixture.detectChanges();  // Trigger initial data binding
  });

  // Simple smoke test — ensures component loads
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});