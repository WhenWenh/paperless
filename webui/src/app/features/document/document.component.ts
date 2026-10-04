import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {
  DocumentService,
  DocumentResponse
} from '@core/services/document.service';

import { finalize } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ValidationService } from '@core/services/validation.service';

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
  validationError = signal<string | null>(null);
  serverError = signal<string | null>(null);
  readonly error = signal('');

  constructor(
    private readonly route: ActivatedRoute,
    private readonly documentService: DocumentService,
    private readonly validationService: ValidationService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.error.set('Document-ID is missing');
      this.loading.set(false);
      return;
    }

    this.documentService.getDocument(id).subscribe({
      next: document => {
        this.document.set(document);
        this.editedTitle.set(document.title);
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
}
