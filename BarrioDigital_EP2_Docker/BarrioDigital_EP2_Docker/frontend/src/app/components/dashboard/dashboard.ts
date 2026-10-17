import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { BarrioService } from '../../services/barrio.service';

import {
  ETIQUETAS_ESTADO,
  Tramite,
} from '../../models/barrio.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe],

  template: `
    <section class="page-heading">
      <div>
        <h1>Hola, {{ auth.username }}</h1>
        <p>Este es el resumen de tu actividad en BarrioDigital.</p>
      </div>

      @if (auth.puedeCrear) {
        <a class="button" routerLink="/requests">Gestionar trámites</a>
      }
    </section>

    @if (barrio.cargando()) {
      <div class="loading-bar">
        <span class="spinner spinner-dark"></span>
        <span>Cargando resumen...</span>
      </div>
    }

    @if (barrio.errorHttp()) { <div class="notice error">{{ barrio.errorHttp() }}</div> }
    @if (auth.roles.length === 0) {
      <div class="notice" role="status">
        Tu sesión de Microsoft está activa, pero no tienes un rol
        reconocido para BarrioDigital. Revisa la asignación de roles
        de la aplicación en Entra ID.
      </div>
    } @else {
      <section class="stats">
        <article class="card">
          <span>Trámites visibles</span>
          <strong>{{ tramites.length }}</strong>
        </article>

        <article class="card">
          <span>Por admitir</span>
          <strong>{{ contar('INGRESADO') }}</strong>
        </article>

        <article class="card">
          <span>En proceso</span>
          <strong>{{ enProceso }}</strong>
        </article>

        <article class="card">
          <span>Resueltos</span>
          <strong>{{ contar('RESUELTO') }}</strong>
        </article>
      </section>

      <section class="card">
        <h2>
          {{ auth.puedeVerTodos ? 'Cola operativa' : 'Mis trámites pendientes' }}
        </h2>

        @for (tramite of pendientes; track tramite.id) {
          <article class="list-row">
            <div>
              <strong>{{ tramite.asunto }}</strong>
              <p class="muted">
                {{ tramite.tipoNombre }} ·
                {{ tramite.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}
              </p>
            </div>

            <span class="badge">
              {{ etiquetas[tramite.estado] }}
            </span>
          </article>
        } @empty {
          <p class="muted">No hay trámites pendientes.</p>
        }
      </section>
    }
  `,
})
export class DashboardComponent {
  readonly auth = inject(AuthService);
  readonly barrio = inject(BarrioService);

  readonly etiquetas = ETIQUETAS_ESTADO;

  get tramites(): Tramite[] {
    return this.barrio.tramitesVisibles();
  }

  contar(estado: Tramite['estado']): number {
    return this.tramites.filter((item) => item.estado === estado).length;
  }

  get enProceso(): number {
    return this.tramites.filter((item) =>
      ['ADMITIDO', 'EN_GESTION', 'EN_TERRENO'].includes(item.estado),
    ).length;
  }

  get pendientes(): Tramite[] {
    return this.tramites
      .filter((item) =>
        !['RESUELTO', 'RECHAZADO'].includes(item.estado),
      )
      .sort((a, b) => a.fechaCreacion.localeCompare(b.fechaCreacion))
      .slice(0, 5);
  }

  constructor() {
    this.auth.mostrarRolesAzure();
    this.barrio.cargarDesdeBackend();
  }
}