// This is the main NgModule for the Services feature module.
// It declares and imports all necessary components, modules, and configurations
// for the services functionality, including routing and forms.

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
// Importing the routing module specific to this feature.
import { ServicesRoutingModule } from './services-routing-module';
// Aliasing component imports for clarity in the module declaration.
import { AddServiceComponent as AddService } from './components/add-service/add-service';
import { EditService } from './components/edit-service/edit-service';
import { ServiceListComponent as ServiceList } from './components/service-list/service-list';
import { ViewServiceComponent as ViewService } from './components/view-service/view-service';
// Defining the NgModule for this feature.

@NgModule({
  // Listing all modules that this feature depends on.
  imports: [
    CommonModule,
    ServicesRoutingModule,
    ReactiveFormsModule,
    ServiceList,
    AddService,
    EditService,
    ViewService,
  ],
})

export class ServicesModule {}