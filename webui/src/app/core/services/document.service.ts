import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface DocumentResponse {
  id: string;
  title: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
  updatedAt: string;
  tagId: string | null;
  tagName: string | null;
}

export interface UpdateDocumentTitleRequest {
  title: string;
}

export interface UpdateDocumentTagRequest {
  tagId: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentService {

  private readonly apiUrl =
    `${environment.apiUrl}/api/v1/documents`;

  constructor(
    private readonly http: HttpClient
  ) {}


  uploadDocument(
    formData: FormData
  ): Observable<DocumentResponse> {

    return this.http.post<DocumentResponse>(
      this.apiUrl,
      formData
    );
  }


  getAllDocuments(
    tag?: string | null
  ): Observable<DocumentResponse[]> {

    if (tag !== undefined) {

      return this.http.get<DocumentResponse[]>(
        this.apiUrl,
        {
          params: {
            tag: tag ?? ''
          }
        }
      );
    }

    return this.http.get<DocumentResponse[]>(
      this.apiUrl
    );
  }


  getDocument(
    id: string
  ): Observable<DocumentResponse> {

    return this.http.get<DocumentResponse>(
      `${this.apiUrl}/${id}`
    );
  }

  updateDocumentTitle(
    id: string,
    title: string
  ): Observable<DocumentResponse> {
    const request: UpdateDocumentTitleRequest = {
      title
    };
    return this.http.put<DocumentResponse>(
      `${this.apiUrl}/${id}`,
      request
    );
  }

  updateDocumentTag(
    id: string,
    tagId: string | null
  ): Observable<DocumentResponse> {

    const request: UpdateDocumentTagRequest = {
      tagId
    };

    return this.http.put<DocumentResponse>(
      `${this.apiUrl}/${id}/tag`,
      request
    );
  }

  downloadDocument(
    id: string
  ): Observable<Blob> {

    return this.http.get(
      `${this.apiUrl}/${id}/content`,
      {
        responseType: 'blob'
      }
    );
  }

  replaceDocumentContent(
    id: string,
    file: File
  ): Observable<DocumentResponse> {

    const formData = new FormData();

    formData.append(
      'file',
      file
    );

    return this.http.put<DocumentResponse>(
      `${this.apiUrl}/${id}/content`,
      formData
    );
  }

  deleteDocument(
    id: string
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }
}
