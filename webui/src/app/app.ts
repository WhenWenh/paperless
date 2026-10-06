import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ValidationService } from '@core/services/validation.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  constructor(
    private readonly validationService: ValidationService
  ) {}

  ngOnInit(): void {
    this.validationService.loadRules().subscribe({
      error: err => {
        console.error(
          'Failed loading validation rules',
          err
        );
      }
    });
  }
}
