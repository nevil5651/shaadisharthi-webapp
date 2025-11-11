import { Component } from '@angular/core';
import { Header } from "../../../layout/header/header";
import { SidebarComponent } from "../../../layout/sidebar/sidebar";
import { RouterOutlet } from "@angular/router";
import { Footer } from "../../../layout/footer/footer";

@Component({
  selector: 'app-main-layout',
  imports: [Header, SidebarComponent, RouterOutlet, Footer],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss'
})
export class MainLayout {

}
