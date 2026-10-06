import { Component, ElementRef, OnInit, signal, ViewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {
  DocumentService,
  DocumentResponse
} from '@core/services/document.service';

import { finalize } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ValidationService } from '@core/services/validation.service';
import { TagService, TagResponse } from '@core/services/tag.service';

@Component({
  selector: 'app-document',
  standalone: true,
  imports: [RouterLink, DatePipe, FormsModule],
  templateUrl: './document.component.html',
  styleUrl: './document.component.css'
})
export class DocumentComponent implements OnInit {
  readonly document = signal<DocumentResponse | null>(null);
  readonly loading = signal(true);
  editedTitle = signal('');
  readonly editingTitle = signal(false);
  readonly savingTitle = signal(false);
  readonly successMessage = signal<string | null>(null);

  startEditingTitle(): void {
    this.editedTitle.set(this.document()?.title ?? '');
    this.validationError.set(null);
    this.serverError.set(null);
    this.successMessage.set(null);
    this.editingTitle.set(true);
  }

  cancelEditingTitle(): void {
    if (this.savingTitle()) return;
    this.editedTitle.set(this.document()?.title ?? '');
    this.validationError.set(null);
    this.serverError.set(null);
    this.editingTitle.set(false);
  }
  readonly tags = signal<TagResponse[]>([]);
  selectedTagId = signal<string | null>(null);
  newTagName = signal('');
  readonly editingTag = signal(false);
  readonly savingTag = signal(false);
  readonly creatingTag = signal(false);
  readonly tagError = signal<string | null>(null);
  readonly tagValidationError = signal<string | null>(null);

  validationError = signal<string | null>(null);
  serverError = signal<string | null>(null);
  readonly error = signal('');
  readonly deleteError = signal<string | null>(null);
  readonly deleting = signal(false);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly documentService: DocumentService,
    private readonly validationService: ValidationService,
    private readonly tagService: TagService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.error.set('Document-ID is missing');
      this.loading.set(false);
      return;
    }

    this.loadTags();

    this.documentService.getDocument(id).subscribe({
      next: document => {
        this.document.set(document);
        this.editedTitle.set(document.title);
        this.selectedTagId.set(document.tagId);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(
          err.status === 404
            ? 'Could not find document'
            : 'Failed to load documents.'
        );
        this.loading.set(false);
      }
    });
  }

  private loadTags(): void {
    this.tagService
      .getTags()
      .subscribe({
        next: tags => {
          this.tags.set(tags);
        },
        error: () => {
          this.tagError.set('Failed to load tags.');
        }
      });
  }

  startEditingTag(): void {
    this.selectedTagId.set(this.document()?.tagId ?? null);
    this.newTagName.set('');
    this.tagError.set(null);
    this.tagValidationError.set(null);
    this.successMessage.set(null);
    this.editingTag.set(true);
  }

  cancelEditingTag(): void {
    if (this.savingTag() || this.creatingTag()) return;
    this.selectedTagId.set(this.document()?.tagId ?? null);
    this.newTagName.set('');
    this.tagError.set(null);
    this.tagValidationError.set(null);
    this.editingTag.set(false);
  }

  updateTag(): void {
    if (this.savingTag() || this.creatingTag()) return;

    const doc = this.document();

    if (!doc) {
      return;
    }

    this.tagError.set(null);
    this.tagValidationError.set(null);
    this.savingTag.set(true);

    this.documentService
      .updateDocumentTag(
        doc.id,
        this.selectedTagId()
      )
      .pipe(finalize(() => this.savingTag.set(false)))
      .subscribe({
        next: updated => {
          this.document.set(updated);
          this.selectedTagId.set(updated.tagId);
          this.editingTag.set(false);
          this.successMessage.set(
            updated.tagName
              ? `Tag "${updated.tagName}" assigned.`
              : 'Tag removed.'
          );
        },
        error: err => {
          this.tagError.set(
            err.error?.message ?? 'Failed to update tag.'
          );
        }
      });
  }

  createAndSelectTag(): void {
    if (this.creatingTag() || this.savingTag()) return;

    const name = this.newTagName().trim();

    this.tagError.set(null);
    this.tagValidationError.set(null);

    if (!name) {
      this.tagValidationError.set('Tag name is required.');
      return;
    }

    const existing = this.tags().find(tag =>
      tag.name.toLowerCase() === name.toLowerCase()
    );

    if (existing) {
      this.selectedTagId.set(existing.id);
      this.newTagName.set('');
      return;
    }

    this.creatingTag.set(true);

    this.tagService
      .createTag(name)
      .pipe(finalize(() => this.creatingTag.set(false)))
      .subscribe({
        next: tag => {
          this.tags.update(current =>
            [...current, tag].sort((first, second) =>
              first.name.localeCompare(second.name)
            )
          );
          this.selectedTagId.set(tag.id);
          this.newTagName.set('');
        },
        error: err => {
          this.tagError.set(
            err.error?.message ?? 'Failed to create tag.'
          );
        }
      });
  }

  updateTitle(): void {
    if (this.savingTitle()) return;
    const doc = this.document();
    const title = this.editedTitle().trim();
    this.validationError.set(null);
    this.serverError.set(null);

    if (!doc) {
      return;
    }

    if (!title) {
      this.validationError.set('Title is required.');
      return;
    }

    const validation =
      this.validationService.validateTitle(title);
    if (validation) {
      this.validationError.set(validation);
      return;
    }

    this.savingTitle.set(true);
    this.successMessage.set(null);
    this.documentService
      .updateDocumentTitle(
        doc.id,
        title
      )
      .pipe(finalize(() => this.savingTitle.set(false)))
      .subscribe({
        next: updated => {
          this.document.set(updated);
          this.editedTitle.set(updated.title);
          this.editingTitle.set(false);
          this.successMessage.set('Title updated successfully.');
        },
        error: err => {

          this.serverError.set(
            err.error?.message ??
            'Failed to update title.'
          );
        }
      });
  }

  @ViewChild('deleteDialog') deleteDialog?: ElementRef<HTMLDialogElement>;

  openDeleteDialog(): void {
    if (this.deleting() || this.savingTitle()) return;
    this.deleteError.set(null);
    this.deleteDialog?.nativeElement.showModal();
  }

  closeDeleteDialog(): void {
    if (!this.deleting()) this.deleteDialog?.nativeElement.close();
  }

  onDeleteDialogCancel(event: Event): void {
    if (this.deleting()) event.preventDefault();
  }

  deleteDocument(): void {
    const doc = this.document();

    if (!doc || this.deleting() || this.savingTitle()) {
      return;
    }

    if (!this.deleteDialog?.nativeElement.open) {
      return;
    }

    this.deleteError.set(null);
    this.deleting.set(true);

    this.documentService.deleteDocument(doc.id)
      .pipe(finalize(() => this.deleting.set(false)))
      .subscribe({
        next: () => {
          this.deleteDialog?.nativeElement.close();
          this.router.navigate(['/tagging']);
        },
        error: err => {
          this.deleteError.set(
            err.error?.message ?? 'Failed to delete document.'
          );
        }
      });
  }
}
