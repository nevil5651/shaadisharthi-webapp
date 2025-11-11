// Component for listing services, using state service for data.

import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { Service } from '../../models/service.model';
import { ServiceStateService } from '../../services/service-state.service';

// Defining the component as standalone.
@Component({
  selector: 'app-service-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './service-list.html',
  styleUrls: ['./service-list.scss'],
})
export class ServiceListComponent implements OnInit {
  // Public observables for the template to bind to
  public readonly services$: Observable<Service[]>;
  public readonly isLoading$: Observable<boolean>;

  // Injecting state service, router, and route.
  constructor(
    private serviceState: ServiceStateService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Initialize observables from the state service
    this.services$ = this.serviceState.services$;
    this.isLoading$ = this.serviceState.isLoading$;
  }

  // Lifecycle hook to load services.
  ngOnInit(): void {
    // Trigger the load. The component doesn't manage the state,
    // it just initiates actions and consumes observables.
    this.serviceState.loadServices();
  }

  // Navigates to edit page for a service.
  editService(serviceId: number): void {
    // Set the selected service in the state so the edit component can easily access it.
    this.serviceState.selectService(serviceId);
    // Programmatic navigation to the edit page
    this.router.navigate(['./edit', serviceId], { relativeTo: this.route });
  }
}