import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class HomeComponent {

  private authService = inject(AuthService);
  private apiService = inject(ApiService);

  mensajePublico = '';
  mensajeProtegido = '';

  get username(): string {
    return this.authService.getAccount()?.username ?? '';
  }


  probarPublico(): void {

    this.apiService.obtenerPublico().subscribe({

      next: (respuesta) => {
        this.mensajePublico = respuesta;
      },

      error: (error) => {
        this.mensajePublico =
          'Error al acceder al endpoint público';

        console.error(error);
      }

    });
  }


  probarProtegido(): void {

    this.apiService.obtenerOrdenes().subscribe({

      next: (respuesta) => {
        this.mensajeProtegido = respuesta;
      },

      error: (error) => {
        this.mensajeProtegido =
          'Error al acceder al endpoint protegido';

        console.error(error);
      }

    });
  }


  logout(): void {
    this.authService.logout();
  }
}