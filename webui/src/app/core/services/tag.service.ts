import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface TagResponse {
  id: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class TagService {
  private readonly http = inject(HttpClient);

  getTags() {
    return this.http.get<TagResponse[]>('/api/v1/tags');
  }
}
