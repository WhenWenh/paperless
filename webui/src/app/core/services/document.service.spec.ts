import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '@env/environment';
import { DocumentService } from './document.service';

describe('DocumentService', () => {
  let service: DocumentService;
  let httpTesting: HttpTestingController;

  const searchUrl = `${environment.apiUrl}/api/v1/documents/search`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(DocumentService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should search using the query parameter', () => {
    service.searchDocuments('Hello').subscribe((documents) => {
      expect(documents).toEqual([]);
    });

    const request = httpTesting.expectOne((req) => req.url === searchUrl);

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('query')).toBe('Hello');
    expect(request.request.params.has('tag')).toBe(false);
    expect(request.request.params.has('untagged')).toBe(false);

    request.flush([]);
  });

  it('should send the selected tag', () => {
    service.searchDocuments('Hello', 'Finance').subscribe();

    const request = httpTesting.expectOne((req) => req.url === searchUrl);

    expect(request.request.params.get('query')).toBe('Hello');
    expect(request.request.params.get('tag')).toBe('Finance');
    expect(request.request.params.has('untagged')).toBe(false);

    request.flush([]);
  });

  it('should send the untagged filter', () => {
    service.searchDocuments('Hello', undefined, true).subscribe();

    const request = httpTesting.expectOne((req) => req.url === searchUrl);

    expect(request.request.params.get('untagged')).toBe('true');
    expect(request.request.params.has('tag')).toBe(false);

    request.flush([]);
  });

  it('should omit an empty query', () => {
    service.searchDocuments('').subscribe();

    const request = httpTesting.expectOne((req) => req.url === searchUrl);

    expect(request.request.params.has('query')).toBe(false);

    request.flush([]);
  });

  it('should pass search errors to the component', () => {
    let receivedStatus: number | undefined;

    service.searchDocuments('Hello').subscribe({
      error: (error) => {
        receivedStatus = error.status;
      },
    });

    const request = httpTesting.expectOne((req) => req.url === searchUrl);

    request.flush(
      { message: 'Search failed' },
      { status: 500, statusText: 'Internal Server Error' },
    );

    expect(receivedStatus).toBe(500);
  });
});
