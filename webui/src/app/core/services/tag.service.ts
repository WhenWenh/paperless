import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface TagResponse {
  id: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class TagService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/tags`;

  constructor(
    private readonly http: HttpClient
  ) {}

  getTags(): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(this.apiUrl);
  }

  createTag(
    name: string
  ): Observable<TagResponse> {
    return this.http.post<TagResponse>(this.apiUrl, { name });
  }

  deleteTag(
    id: string
  ): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
