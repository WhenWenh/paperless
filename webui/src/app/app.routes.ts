import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { DocumentComponent } from './features/document/document.component';
import { TaggingComponent } from '@features/tagging/tagging.component';

export const routes: Routes = [
  { path: 'dashboard', component: DashboardComponent },
  { path: 'document/:id', component: DocumentComponent },
  { path: 'tagging', component: TaggingComponent },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'dashboard' }
];
