import { Component } from '@angular/core';

// This is a simple standalone Angular component for the footer
// It has no logic — just displays static HTML from footer.html
@Component({
  selector: 'app-footer',        // Use <app-footer></app-footer> in templates
  standalone: true,              // Can be used without a module
  imports: [],                   // No other components or directives needed
  templateUrl: './footer.html',   // HTML template file
  styleUrl: './footer.scss'      // SCSS styles for this footer
})
export class Footer {
  // No properties or methods — it's a static footer
}