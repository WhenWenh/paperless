import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { UploadComponent } from './upload.component';
import { DocumentService } from '@core/services/document.service';
import { TagService } from '@core/services/tag.service';
import { ValidationService } from '@core/services/validation.service';
import { provideHttpClient } from '@angular/common/http';

describe('UploadComponent validation', () => {
  const uploadDocument = vi.fn();

  beforeEach(() => {
    uploadDocument.mockReset();
    TestBed.configureTestingModule({
      imports: [UploadComponent],
      providers: [
        provideHttpClient(),
        { provide: DocumentService, useValue: { uploadDocument } },
        { provide: TagService, useValue: { getTags: () => of([]) } }
      ]
    });
    vi.spyOn(TestBed.inject(ValidationService), 'getRules').mockReturnValue({
      maxTitleLength: 20, maxFileSize: 100, allowedContentTypes: ['application/pdf']
    });
  });

  it('shows a required error only after leaving the title field', async () => {
    const fixture = TestBed.createComponent(UploadComponent);
    await fixture.whenStable();
    const title = fixture.nativeElement.querySelector('#title') as HTMLInputElement;
    expect(fixture.nativeElement.querySelector('#title-error')).toBeNull();
    title.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    expect(title.classList.contains('invalid')).toBe(true);
    expect(fixture.nativeElement.querySelector('#title-error').textContent).toContain('Please enter');
    title.value = 'Invoice';
    title.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#title-error')).toBeNull();
  });

  it('keeps upload clickable and shows both missing fields without a request', async () => {
    const fixture = TestBed.createComponent(UploadComponent);
    await fixture.whenStable();
    const button = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(button.disabled).toBe(false);
    button.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#title-error')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#file-error')).not.toBeNull();
    expect(uploadDocument).not.toHaveBeenCalled();
  });

  it('rejects whitespace titles and unsupported files at their fields', () => {
    const fixture = TestBed.createComponent(UploadComponent);
    const component = fixture.componentInstance;
    component.form.controls.title.setValue('   ');
    component.form.controls.file.setValue(new File(['abc'], 'bad.exe', { type: 'application/x-msdownload' }));
    component.upload();
    expect(component.fieldError('title')).toContain('Please enter');
    expect(component.fieldError('file')).toContain('not allowed');
    expect(uploadDocument).not.toHaveBeenCalled();
  });

  it('uploads valid data once and resets after success', () => {
    const result = new Subject<unknown>();
    uploadDocument.mockReturnValue(result);
    const component = TestBed.createComponent(UploadComponent).componentInstance;
    component.form.controls.title.setValue('Invoice');
    component.form.controls.file.setValue(new File(['abc'], 'invoice.pdf', { type: 'application/pdf' }));
    component.upload();
    component.upload();
    expect(uploadDocument).toHaveBeenCalledTimes(1);
    expect(uploadDocument.mock.calls[0][0].get('file').name).toBe('invoice.pdf');
    result.next({});
    result.complete();
    expect(component.form.controls.title.value).toBe('');
    expect(component.form.touched).toBe(false);
    expect(component.isUploading()).toBe(false);
  });
});
