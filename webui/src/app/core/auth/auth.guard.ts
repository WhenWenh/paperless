import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '@core/services/auth.service';

export const authGuard = () => {

  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAuthenticated$.pipe(

    map(result => {

      console.log('AUTH GUARD RESULT', result);

      if (result.isAuthenticated) {
        return true;
      }

      auth.login();

      return false;
    })
  );
};
