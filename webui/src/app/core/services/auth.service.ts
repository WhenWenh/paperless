import { Injectable, inject } from '@angular/core';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import {tap} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly oidc =
    inject(OidcSecurityService);

  isAuthenticated$ =
    this.oidc.isAuthenticated$.pipe(
      tap(value => {
        console.log('OIDC authenticated state:', value);
      })
    );

  userData$ =
    this.oidc.userData$;


  login(): void {
    this.oidc.authorize();
  }


  logout(): void {
    this.oidc.logoff().subscribe();
  }
}
