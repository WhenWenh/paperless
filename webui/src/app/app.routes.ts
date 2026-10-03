import { Routes } from '@angular/router';
import { MainLayoutComponent } from '@layout/main-layout/main-layout.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { DocumentComponent } from './features/document/document.component';
import { TaggingComponent } from '@features/tagging/tagging.component';
import { UploadComponent } from '@features/upload/upload.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'document/:id', component: DocumentComponent },
      { path: 'tagging', component: TaggingComponent },
      { path: 'upload', component: UploadComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
