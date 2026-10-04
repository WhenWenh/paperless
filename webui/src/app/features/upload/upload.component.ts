import { Component, ElementRef, OnInit, signal, ViewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { DocumentService } from '@core/services/document.service';
import { TagService, TagResponse } from '@core/services/tag.service';
import { ValidationService } from '@core/services/validation.service';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.css']
})
export class UploadComponent implements OnInit {
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  @ViewChild('fileButton') fileButton?: ElementRef<HTMLButtonElement>;
  @ViewChild('titleInput') titleInput?: ElementRef<HTMLInputElement>;

  readonly tags = signal<TagResponse[]>([]);
  readonly isUploading = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [
      control => {
        const title = (control.value as string).trim();
        if (!title) return { message: 'Please enter a document title.' };
        if (!this.validationService.getRules()) return null;
        const message = this.validationService.validateTitle(title);
        return message ? { message } : null;
      }
    ] }),
    file: new FormControl<File | null>(null, { validators: [
      control => {
        const file = control.value as File | null;
        if (!file) return { message: 'Please select a file.' };
        if (file.size === 0) return { message: 'The selected file is empty.' };
        if (!this.validationService.getRules()) return null;
        const message = this.validationService.validateFile(file);
        return message ? { message } : null;
      }
    ] }),
    tagId: new FormControl<string | null>(null)
  });

  constructor(
    private readonly documentService: DocumentService,
    private readonly tagService: TagService,
    private readonly validationService: ValidationService
  ) {}

  ngOnInit(): void {
    this.tagService.getTags().subscribe({
      next: tags => this.tags.set(tags),
      error: () => this.serverError.set('Tags could not be loaded. You can upload without a tag.')
    });
    if (!this.validationService.getRules()) {
      this.validationService.loadRules().subscribe({
        next: () => this.revalidate(),
        error: () => this.serverError.set('Validation rules could not be loaded. Please reload the page.')
      });
    }
  }

  fieldError(name: 'title' | 'file'): string | null {
    const control = this.form.controls[name];
    return control.touched && control.invalid ? control.getError('message') : null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.form.controls.file.setValue(input.files?.[0] ?? null);
    this.form.controls.file.markAsTouched();
    this.form.controls.file.markAsDirty();
  }

  upload(): void {
    // Keep the button clickable, but never start a second concurrent upload.
    if (this.isUploading()) return;
    this.serverError.set(null);
    this.successMessage.set(null);
    this.revalidate();
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      if (this.form.controls.title.invalid) this.titleInput?.nativeElement.focus();
      else this.fileButton?.nativeElement.focus();
      return;
    }
    if (!this.validationService.getRules()) {
      this.serverError.set('Validation rules are not available yet. Please reload the page and try again.');
      return;
    }

    const { title, file, tagId } = this.form.getRawValue();
    if (!file) return;

    const formData = new FormData();
    formData.append('metadata', new Blob(
      [JSON.stringify({ title: title.trim(), tagId })],
      { type: 'application/json' }
    ));
    formData.append('file', file);
    this.isUploading.set(true);
    this.form.disable();

    this.documentService.uploadDocument(formData).pipe(
      finalize(() => {
        this.isUploading.set(false);
        if (this.form.disabled) this.form.enable();
      })
    ).subscribe({
      next: () => {
        this.form.reset();
        if (this.fileInput) this.fileInput.nativeElement.value = '';
        this.successMessage.set('Document uploaded successfully.');
      },
      error: err => {
        this.form.enable();
        const errors = err.error?.details;
        let fieldErrors = false;
        for (const name of ['title', 'file'] as const) {
          if (typeof errors?.[name] === 'string') {
            this.form.controls[name].setErrors({ message: errors[name] });
            this.form.controls[name].markAsTouched();
            fieldErrors = true;
          }
        }
        if (!fieldErrors) this.serverError.set(err.error?.message ?? 'Upload failed. Please try again.');
      }
    });
  }

  private revalidate(): void {
    this.form.controls.title.updateValueAndValidity();
    this.form.controls.file.updateValueAndValidity();
  }
}
