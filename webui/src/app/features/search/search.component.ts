import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { DocumentService, DocumentResponse } from '@core/services/document.service';

import { TagService, TagResponse } from '@core/services/tag.service';

interface TagSelection {
  id: string | null;
  name: string;
  type: 'all' | 'none' | 'tag';
}

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css'],
})
export class SearchComponent implements OnInit {
  tags = signal<TagResponse[]>([]);
  documents = signal<DocumentResponse[]>([]);
  searchQuery = signal('');

  selected = signal<TagSelection>({
    id: null,
    name: 'All Documents',
    type: 'all',
  });

  loading = signal(false);
  error = signal<string | null>(null);
  selections = signal<TagSelection[]>([]);

  constructor(
    private readonly tagService: TagService,
    private readonly documentService: DocumentService,
  ) {}

  ngOnInit(): void {
    this.loadSelections();
  }

  private loadSelections(): void {
    this.loading.set(true);
    this.error.set(null);

    this.tagService.getTags().subscribe({
      next: (tags) => {
        this.tags.set(tags);

        this.selections.set([
          {
            id: null,
            name: 'All Documents',
            type: 'all',
          },
          {
            id: null,
            name: 'No Tag',
            type: 'none',
          },
          ...tags.map((tag) => ({
            id: tag.id,
            name: tag.name,
            type: 'tag' as const,
          })),
        ]);

        this.refreshDocuments();
      },
      error: () => {
        this.error.set('Failed to load search filters.');
        this.loading.set(false);
      },
    });
  }

  onSearchChange(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.searchQuery.set(input.value);
    this.refreshDocuments();
  }

  select(selection: TagSelection): void {
    this.selected.set(selection);
    this.refreshDocuments();
  }

  private refreshDocuments(): void {
    this.loading.set(true);
    this.error.set(null);
    this.documents.set([]);

    const selection = this.selected();

    const tag = selection.type === 'tag' ? selection.name : undefined;

    const untagged = selection.type === 'none';

    this.documentService.searchDocuments(this.searchQuery().trim(), tag, untagged).subscribe({
      next: (documents) => {
        this.documents.set(documents);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Search failed. Please try again.');
        this.loading.set(false);
      },
    });
  }

  showTag(): boolean {
    return this.selected().type === 'all';
  }
}
