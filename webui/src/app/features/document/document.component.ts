import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  DocumentService,
  DocumentResponse
} from '@core/services/document.service';
import {TaggingComponent} from '@features/tagging/tagging.component';

@Component({
  selector: 'app-document',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './document.component.html',
  styleUrl: './document.component.css'
})
export class DocumentComponent implements OnInit {
  readonly document = signal<DocumentResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(
    private readonly route: ActivatedRoute,
    private readonly documentService: DocumentService
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
}
