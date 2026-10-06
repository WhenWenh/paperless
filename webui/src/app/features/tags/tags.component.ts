import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { finalize } from 'rxjs';

import { TagService, TagResponse } from '@core/services/tag.service';

@Component({
  selector: 'app-tags',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './tags.component.html',
  styleUrls: ['./tags.component.css']
})
export class TagsComponent implements OnInit {

  tags = signal<TagResponse[]>([]);
  newTagName = signal('');

  loading = signal(false);
  creating = signal(false);
  deletingId = signal<string | null>(null);

  error = signal<string | null>(null);
  validationError = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  constructor(
    private readonly tagService: TagService
  ) {}

  ngOnInit(): void {
    this.loadTags();
  }

  private loadTags(): void {
    this.loading.set(true);
    this.error.set(null);

    this.tagService.getTags()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: tags => {
          this.tags.set(tags);
        },
        error: () => {
          this.error.set('Failed to load tags.');
        }
      });
  }

  addTag(): void {
    if (this.creating()) {
      return;
    }

    const name = this.newTagName().trim();

    this.error.set(null);
    this.successMessage.set(null);
    this.validationError.set(null);

    if (!name) {
      this.validationError.set('Tag name is required.');
      return;
    }

    if (this.hasTagNamed(name)) {
      this.validationError.set(`Tag "${name}" already exists.`);
      return;
    }

    this.creating.set(true);

    this.tagService.createTag(name)
      .pipe(finalize(() => this.creating.set(false)))
      .subscribe({
        next: tag => {
          this.tags.update(current =>
            [...current, tag].sort((first, second) =>
              first.name.localeCompare(second.name)
            )
          );

          this.newTagName.set('');
          this.successMessage.set(`Tag "${tag.name}" created.`);
        },
        error: err => {
          this.error.set(
            err.error?.message ?? 'Failed to create tag.'
          );
        }
      });
  }

  deleteTag(tag: TagResponse): void {
    if (this.deletingId()) {
      return;
    }

    this.error.set(null);
    this.successMessage.set(null);
    this.validationError.set(null);
    this.deletingId.set(tag.id);

    this.tagService.deleteTag(tag.id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => {
          this.tags.update(current =>
            current.filter(current => current.id !== tag.id)
          );

          this.successMessage.set(`Tag "${tag.name}" deleted.`);
        },
        error: err => {
          this.error.set(
            err.error?.message ??
            `Failed to delete tag "${tag.name}".`
          );
        }
      });
  }

  private hasTagNamed(name: string): boolean {
    return this.tags().some(tag =>
      tag.name.toLowerCase() === name.toLowerCase()
    );
  }
}
