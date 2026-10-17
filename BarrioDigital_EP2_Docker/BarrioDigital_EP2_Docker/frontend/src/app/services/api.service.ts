import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../environments/environment';
import { Observable, switchMap } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ApiService {

  private http = inject(HttpClient);
  private msalService = inject(MsalService);


  // =========================
  // ENDPOINT PÚBLICO
  // =========================

  obtenerPublico(): Observable<string> {

    return this.http.get(
      `${environment.apiUrl}/api/public`,
      {
        responseType: 'text'
      }
    );
  }


  // =========================
  // ENDPOINT PROTEGIDO
  // =========================

  obtenerOrdenes(): Observable<string> {

    const account =
      this.msalService.instance.getActiveAccount();

    if (!account) {
      throw new Error('No existe una cuenta autenticada');
    }

    return this.msalService.acquireTokenSilent({

      scopes: [
        environment.azure.backendScope
      ],

      account: account,

    }).pipe(

      switchMap((tokenResponse) => {

        const headers = new HttpHeaders({

          Authorization:
            `Bearer ${tokenResponse.accessToken}`

        });

        return this.http.get(
          `${environment.apiUrl}/api/ordenes`,
          {
            headers,
            responseType: 'text',
          }
        );

      })
    );
  }
}