import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { ConfirmationDialogComponent } from './confirmation-dialog';

// Test suite for the ConfirmationDialogComponent
describe('ConfirmationDialogComponent', () => {
  let component: ConfirmationDialogComponent;
  let fixture: ComponentFixture<ConfirmationDialogComponent>;

  // Setup before each test
  beforeEach(async () => {
    await TestBed.configureTestingModule({ // Changed from ConfirmationDialog to ConfirmationDialogComponent
      imports: [ConfirmationDialogComponent],
      providers: [
        // Provide mock dependencies for the dialog
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: {} }
      ]
    })
    .compileComponents(); // Changed from ConfirmationDialog to ConfirmationDialogComponent

    // Create the component instance
    fixture = TestBed.createComponent(ConfirmationDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Basic test to check if component creates successfully
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});