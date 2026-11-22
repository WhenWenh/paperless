import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { DocumentResponse } from '@core/services/document.service';
import { DocumentComponent } from './document.component';

const DOCUMENT_ID = '11111111-1111-1111-1111-111111111111';
const DOCUMENT_URL = `http://localhost:8081/api/v1/documents/${DOCUMENT_ID}`;
const TAGS_URL = 'http://localhost:8081/api/v1/tags';

function documentWith(summary: string | null): DocumentResponse {
  return {
    id: DOCUMENT_ID,
    title: 'Invoice',
    originalFilename: 'invoice.pdf',
    contentType: 'application/pdf',
    fileSize: 1234,
    summary,
    createdAt: '2026-11-21T10:00:00Z',
    updatedAt: '2026-11-21T10:00:00Z',
    tagId: null,
    tagName: null
  };
}

describe('DocumentComponent', () => {
  let component: DocumentComponent;
  let fixture: ComponentFixture<DocumentComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => DOCUMENT_ID } }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  /** Answers the two requests made while the detail page loads. */
  function load(summary: string | null): void {
    fixture.detectChanges();
    httpMock.expectOne(TAGS_URL).flush([]);
    httpMock.expectOne(DOCUMENT_URL).flush(documentWith(summary));
    fixture.detectChanges();
  }

  it('should create', () => {
    load(null);
    expect(component).toBeTruthy();
  });

  it('shows the summary when the document has one', () => {
    load('This invoice covers March 2026.');

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Summary');
    expect(text).toContain('This invoice covers March 2026.');
  });

  it('explains that no summary exists yet and offers to check again', () => {
    load(null);

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('No summary available yet');

    const button = fixture.nativeElement.querySelector('.refresh-summary');
    expect(button).toBeTruthy();
  });

  it('picks up a summary that was generated in the meantime', () => {
    load(null);

    component.refreshSummary();

    httpMock.expectOne(DOCUMENT_URL)
      .flush(documentWith('Generated in the meantime.'));
    fixture.detectChanges();

    expect(component.document()?.summary)
      .toBe('Generated in the meantime.');
    expect(fixture.nativeElement.textContent)
      .toContain('Generated in the meantime.');
    expect(component.refreshingSummary()).toBe(false);
  });

  it('reports a failed refresh without losing the document', () => {
    load(null);

    component.refreshSummary();

    httpMock.expectOne(DOCUMENT_URL).flush(
      { message: 'boom' },
      { status: 500, statusText: 'Server Error' }
    );
    fixture.detectChanges();

    expect(component.summaryError()).toBe('Could not refresh the summary.');
    expect(component.document()).not.toBeNull();
    expect(component.refreshingSummary()).toBe(false);
  });
});
