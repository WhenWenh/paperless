import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

// export interface HealthCheck {
//   status: 'UP' | 'DOWN';
//   timestamp: string;
// }

export interface HealthCheck {
  status: string;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class HealthService {
  private apiUrl = `${environment.apiUrl}/health`;

  constructor(private http: HttpClient) {}

  getHealth(): Observable<HealthCheck> {
    return this.http.get<HealthCheck>(this.apiUrl);
  }
}
