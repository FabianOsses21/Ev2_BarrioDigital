import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  readonly auth = inject(AuthService);

  login(): void {
    this.auth.login();
  }

  registrar(): void {
    this.auth.registrar();
  }

  logout(): void {
    this.auth.logout();
  }
}