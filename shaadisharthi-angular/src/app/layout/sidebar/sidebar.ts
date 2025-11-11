import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

interface SidebarItem {
  path: string;
  title: string;
  icon: string;
  children?: SidebarItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.scss'],
  imports: [CommonModule, RouterModule],
})
export class SidebarComponent {
  // Define your sidebar items in a type-safe way
  sidebarItems: SidebarItem[] = [
    {
      path: '/dashboard',
      title: 'Dashboard',
      icon: 'bi bi-grid'
    },
    {
      path: '/services',
      title: 'Services',
      icon: 'bi bi-box-arrow-in-right'
    },
    {
      path: '/bookings/pending',
      title: 'Pending Bookings',
      icon: 'bi bi-box-arrow-in-right'
    },
    {
      path: '/bookings/confirmed',
      title: 'Confirmed Bookings',
      icon: 'bi bi-box-arrow-in-right'
    },
    {
      path: '/account',
      title: 'Account',
      icon: 'bi bi-person',
    },
    {
      path: '/faqs',
      title: 'F.A.Q',
      icon: 'bi bi-question-circle'
    }
  ];

  onSidebarItemClick(): void {
    // Close sidebar on item click on mobile view
    // We check if the screen is small and if the sidebar is open.
    if (window.innerWidth < 1200 && document.body.classList.contains('toggle-sidebar')) {
      document.body.classList.remove('toggle-sidebar');
    }
  }
}