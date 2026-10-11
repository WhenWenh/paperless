import { Component, OnInit, inject, signal } from '@angular/core';
import { HealthService } from '@core/services/health.service';
import { AuthService } from '@core/services/auth.service';
import {toSignal} from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-status-bar',
  standalone: true,
  imports: [],
  templateUrl: './status-bar.component.html',
  styleUrl: './status-bar.component.css',
})
export class StatusBarComponent implements OnInit {

  private readonly authService = inject(AuthService);

  status = signal<'UP' | 'DOWN'>('DOWN');

  user = toSignal(this.authService.userData$);

  constructor(
    private readonly healthService: HealthService
  ) {}


  ngOnInit(): void {

    this.healthService.getHealth()
      .subscribe({
        next: result => {
          this.status.set(
            result.status === 'UP'
              ? 'UP'
              : 'DOWN'
          );
        },

        error: () => {
          this.status.set('DOWN');
        }
      });

  }
}
