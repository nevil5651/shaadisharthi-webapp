import { Component, Inject, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { AuthService } from '../../core/services/auth';
import { Subscription } from 'rxjs';

// Main header component — appears on all pages
@Component({
  selector: 'app-header',
  imports: [],                    // No child components imported here
  standalone: true,               // Standalone component (Angular 14+)
  templateUrl: './header.html',   // HTML template
  styleUrl: './header.scss',      // SCSS styles
})
export class Header implements OnInit, OnDestroy {
  public accountData: any | null = null;  // Holds current user info (name, etc.)
  private accountDataSubscription: Subscription | undefined;  // Tracks subscription

  // Inject DOM document, auth service, and change detector
  constructor(
    @Inject(DOCUMENT) private document: Document,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  // Runs when component loads
  ngOnInit(): void {
    // Subscribe to current user data from AuthService
    // Whenever user logs in or updates profile, this updates automatically
    this.accountDataSubscription = this.authService.currentUser$.subscribe((data: any) => {
      this.accountData = data;        // Update local copy
      this.cdr.markForCheck();        // Force Angular to refresh view
    });
  }

  // Toggles the sidebar (adds/removes 'toggle-sidebar' class on body)
  toggleSidebar(): void {
    this.document.body.classList.toggle('toggle-sidebar');
  }

  // Logs out the user by calling AuthService
  logout(): void {
    this.authService.logoutFromServer();
  }

  // Cleanup: unsubscribe when component is destroyed
  ngOnDestroy(): void {
    if (this.accountDataSubscription) {
      this.accountDataSubscription.unsubscribe();  // Prevent memory leaks
    }
  }
}