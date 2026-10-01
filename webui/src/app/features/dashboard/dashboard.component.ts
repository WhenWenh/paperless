import { Component, inject, OnInit, signal } from '@angular/core';
import {
  TagService,
  TagResponse
} from '../../core/services/tag.service';

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private readonly tagService = inject(TagService);

  readonly tags = signal<TagResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.tagService.getTags().subscribe({
      next: tags => {
        this.tags.set(tags);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Die Tags konnten nicht geladen werden.');
        this.loading.set(false);
      }
    });
  }
}
