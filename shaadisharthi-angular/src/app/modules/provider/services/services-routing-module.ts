// This module handles the routing configuration for the Services feature.
// It defines child routes for listing, adding, viewing, and editing services.
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
// Aliasing component imports for use in route definitions.
import { AddServiceComponent as AddService } from './components/add-service/add-service';
import { EditService } from './components/edit-service/edit-service';
import { ServiceListComponent as ServiceList } from './components/service-list/service-list';
import { ViewServiceComponent as ViewService } from './components/view-service/view-service';
// Defining the routes array with paths, components, and page titles.

const routes: Routes = [
  { path: '', component: ServiceList, title: 'My Services' }, // Default route shows the service list.
  { path: 'add', component: AddService, title: 'Add New Service' }, // Route for adding a new service.
  { path: 'view/:id', component: ViewService, title: 'View Service' }, // Route for viewing a specific service by ID.
  { path: 'edit/:id', component: EditService, title: 'Edit Service' }, // Route for editing a specific service by ID.
];

// Configuring the routing module as a child module.
@NgModule({
  imports: [RouterModule.forChild(routes)], // Using forChild() since this is a feature module.
  exports: [RouterModule], // Exporting RouterModule for use in the parent module.
})

export class ServicesRoutingModule {}