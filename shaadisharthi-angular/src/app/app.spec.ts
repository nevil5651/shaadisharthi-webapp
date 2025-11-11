import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],  // Standalone component
    }).compileComponents();
  });

  // Test 1: App should load without crashing
  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  // Test 2: Title should match expected value
  it(`should have as title 'shaadisharthi-provider'`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('shaadisharthi-provider');
  });
});