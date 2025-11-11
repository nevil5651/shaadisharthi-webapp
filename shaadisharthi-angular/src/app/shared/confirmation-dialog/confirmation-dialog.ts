import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

// This component is for a confirmation dialog, can optionally show an input for reason
@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './confirmation-dialog.html',
  styleUrl: './confirmation-dialog.scss'
})
export class ConfirmationDialogComponent {
  // Variable to hold the reason if input is shown
  reason = '';

  // Constructor injects dialog reference and data
  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { title: string; message: string; showInput?: boolean }
  ) {}

  // Function to close dialog without confirmation
  onNoClick(): void { this.dialogRef.close(); }
  // Function to close dialog with confirmation and reason if provided
  onYesClick(): void { this.dialogRef.close({ confirm: true, reason: this.reason }); }
}