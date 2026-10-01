import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { DocumentComponent } from './features/document/document.component';

export const routes: Routes = [
  { path: 'dashboard', component: DashboardComponent },
  { path: 'document/:id', component: DocumentComponent },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'dashboard' }
];
