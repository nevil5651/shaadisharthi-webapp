import { Component } from '@angular/core';
import {  RouterModule } from '@angular/router';

@Component({
  selector: 'app-root',
  template: '<router-outlet></router-outlet>'  // Just for initial routing
  ,
  imports: [RouterModule]
})
export class AppComponent {
  title = 'shaadisharthi-provider';
}
