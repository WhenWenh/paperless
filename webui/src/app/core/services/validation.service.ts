import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';

import { environment } from '@env/environment';

export interface ValidationRules {
  maxTitleLength: number;
  maxFileSize: number;
  allowedContentTypes: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ValidationService {
  private readonly apiUrl =
    `${environment.apiUrl}/api/v1/config/validation`;

  private readonly rules =
    new BehaviorSubject<ValidationRules | null>(null);

  constructor(private readonly http: HttpClient) {}

  loadRules(): Observable<ValidationRules> {
    return this.http.get<ValidationRules>(this.apiUrl).pipe(
      tap(rules => {
        this.rules.next(rules);
      })
    );
  }

  getRules(): ValidationRules | null {
    return this.rules.value;
  }

  validateTitle(title: string): string | null {
    const rules = this.getRules();

    if (!rules) {
      return 'Validation rules not loaded';
    }

    if (!title.trim()) {
      return 'Title is required';
    }

    if (title.length > rules.maxTitleLength) {
      return `Title must not exceed ${rules.maxTitleLength} characters`;
    }

    return null;
  }

  validateFile(file: File): string | null {
    const rules = this.getRules();

    if (!rules) {
      return 'Validation rules not loaded';
    }

    if (file.size > rules.maxFileSize) {
      return `File size must not exceed ${rules.maxFileSize} bytes`;
    }

    if (!rules.allowedContentTypes.includes(file.type)) {
      return `File type ${file.type} is not allowed`;
    }

    return null;
  }
}
