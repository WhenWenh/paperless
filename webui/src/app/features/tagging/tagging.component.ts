import {
  Component,
  OnInit,
  signal
} from '@angular/core';

import { CommonModule} from '@angular/common';
import { Router } from '@angular/router';

import {
  DocumentService,
  DocumentResponse
} from '@core/services/document.service';

import {
  TagService,
  TagResponse
} from '@core/services/tag.service';

interface TagSelection {
  id: string | null;
  name: string;
  type: 'all' | 'none' | 'tag';
}

@Component({
  selector: 'app-tagging',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './tagging.component.html',
  styleUrls: ['./tagging.component.css']
})
export class TaggingComponent implements OnInit {
  tags = signal<TagResponse[]>([]);
  documents = signal<DocumentResponse[]>([]);

  selected = signal<TagSelection>({
    id: null,
    name: 'All Documents',
    type: 'all'
  });

  loading = signal(false);
  error = signal<string | null>(null);

  selections = signal<TagSelection[]>([]);

  constructor(
    private readonly tagService: TagService,
    private readonly documentService: DocumentService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadSelections();
    this.loadDocuments();
  }

  private loadSelections(): void {
    this.tagService
      .getTags()
      .subscribe({
        next: tags => {
          this.tags.set(tags);

          this.selections.set([
            {
              id: null,
              name: 'All Documents',
              type: 'all'
            },
            {
              id: null,
              name: 'No Tag',
              type: 'none'
            },
            ...tags.map(tag => ({
              id: tag.id,
              name: tag.name,
              type: 'tag' as const
            }))
          ]);
        }
      });
  }

  select(selection: TagSelection): void {
    this.selected.set(selection);
    this.loadDocuments();
  }

  private loadDocuments(): void {
    this.loading.set(true);
    this.error.set(null);

    const selection = this.selected();

    if (selection.type === 'all') {
      this.documentService
        .getAllDocuments()
        .subscribe({
          next: docs => {
            this.documents.set(docs);
            this.loading.set(false);
          },
          error: () => {
            this.error.set(
              'Failed to load documents.'
            );
            this.loading.set(false);
          }
        });

      return;
    }

    if (selection.type === 'none') {
      this.documentService
        .getAllDocuments('none')
        .subscribe({
          next: docs => {
            this.documents.set(docs);
            this.loading.set(false);
          },
          error: () => {
            this.error.set(
              'Failed to load documents.'
            );
            this.loading.set(false);
          }
        });

      return;
    }

    this.documentService
      .getAllDocuments(selection.name)
      .subscribe({
        next: docs => {
          this.documents.set(docs);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(
            'Failed to load documents.'
          );
          this.loading.set(false);
        }
      });
  }

  openDocument(document: DocumentResponse): void {
    this.router.navigate([
      '/document',
      document.id
    ]);
  }

  showTag(): boolean {
    return this.selected().type === 'all';
  }
}
