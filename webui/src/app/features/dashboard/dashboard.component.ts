import { Component, inject, OnInit, signal } from '@angular/core';
import {
  TagService,
  TagResponse
} from '../../core/services/tag.service';

import {
  TaggingComponent
} from '@features/tagging/tagging.component';

@Component({
  selector: 'app-dashboard',
  imports: [ TaggingComponent ],
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
        this.error.set('Tags could not be found.');
        this.loading.set(false);
      }
    });
  }
}
