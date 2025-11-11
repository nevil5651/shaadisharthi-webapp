import { Routes } from '@angular/router';
import { Dashboard } from './dashboard/dashboard';
import { LoginComponent } from './auth/login/login';
import { MainLayout } from './core/layouts/main-layout/main-layout';
import { EmptyLayout } from './core/layouts/empty-layout/empty-layout';
import { AuthGuard } from './core/guards/auth-guard';
import { Account } from './features/account/account';
import { RegisterComponent } from './auth/register-provider/register-provider';
import { BusinessDetailsComponent } from './auth/business-details/business-details';
import { ForgotPasswordComponent } from './auth/forgot-password/forgot-password';
import { ResetPasswordComponent } from './auth/reset-password/reset-password';
import { EmailVerificationComponent } from './auth/email-verification/email-verification';
import { VerifyEmailMessageComponent } from './auth/verify-email-message/verify-email-message';
import { RoleGuard } from './core/guards/role-guard';
import { WaitingApproval } from './auth/waiting-approval/waiting-approval';
import { FaqsComponent } from './pages/faqs/faqs';

// This file defines ALL routes in Councils the app
// Uses two layouts:
//   1. MainLayout → for logged-in users (sidebar, header)
//   2. EmptyLayout → for public/auth pages (no layout)
export const routes: Routes = [

  // ===================== PROTECTED ROUTES (Logged-in + Approved) =====================
  {
    path: '',
    component: MainLayout,  // Layout with sidebar, navbar, etc.
    canActivate: [AuthGuard, RoleGuard],  // Must be logged in + correct role/status
    data: { roles: ['SERVICE_PROVIDER'], status: 'APPROVED' },  // Only approved providers
    children: [
      { path: 'dashboard', component: Dashboard },  // Home dashboard
      {
        path: 'services',
        loadChildren: () => import('./modules/provider/services/services-module').then(m => m.ServicesModule)
        // Lazy-loaded module for managing services
      },
      {
        path: 'bookings',
        loadChildren: () => import('./features/bookings/bookings.routes').then(r => r.BOOKINGS_ROUTES)
        // Lazy-loaded bookings module
      },
      { path: 'account', component: Account },  // Profile settings
      { path: 'faqs', component: FaqsComponent, title: 'FAQs' },  // Help page
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }  // Default redirect
    ]
  },

  // ===================== PUBLIC / AUTH ROUTES (No layout) =====================
  {
    path: '',
    component: EmptyLayout,  // Clean, full-screen layout
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent, title: 'Register Provider' },
      
      // Business details — only for partially registered users
      {
        path: 'business-details',
        component: BusinessDetailsComponent,
        title: 'Business Details',
        canActivate: [AuthGuard, RoleGuard],
        data: { roles: ['SERVICE_PROVIDER'], status: 'BASIC_REGISTERED' }  // After email + password
      },
      
      // Waiting page — only for pending users
      {
        path: 'waiting-approval',
        component: WaitingApproval,
        title: 'Waiting for Approval',
        canActivate: [AuthGuard, RoleGuard],
        data: { roles: ['SERVICE_PROVIDER'], status: 'PENDING_APPROVAL' }
      },

      // Password recovery
      { path: 'forgot-password', component: ForgotPasswordComponent, title: 'Forgot Password' },
      { path: 'reset-password', component: ResetPasswordComponent, title: 'Reset Password' },

      // Email verification flow
      { path: 'email-verification', component: EmailVerificationComponent, title: 'Email Verification' },
      { path: 'verify-email-message', component: VerifyEmailMessageComponent, title: 'Verify Email Message' },
    ]
  },

  // Catch-all: redirect unknown paths to login
  { path: '**', redirectTo: 'login' }
];