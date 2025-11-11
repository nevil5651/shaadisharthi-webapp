import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth';
import { environment } from '../../../environments/environment';

// Shown after business details are submitted — waiting for admin approval
@Component({
  selector: 'app-waiting-approval',
  standalone: true,
  imports: [],
  templateUrl: './waiting-approval.html',
  styleUrl: './waiting-approval.scss'
})
export class WaitingApproval {
  public env = environment;  // Access support email from env

  constructor(private authService: AuthService, private router: Router) {}

  // Log out and go to login page
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}