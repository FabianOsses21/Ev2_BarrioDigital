import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../services/auth.service';
import { BarrioService } from '../../services/barrio.service';

import {
  EstadoTramite,
  ETIQUETAS_ESTADO,
  Tramite,
  TRANSICIONES,
} from '../../models/barrio.models';

@Component({
  selector: 'app-requests',
  standalone: true,
  imports: [FormsModule, DatePipe],

  template: `
    <section class="page-heading">
      <div>
        <h1>Trámites</h1>
        <p>
          {{ auth.puedeVerTodos
            ? 'Consulta y gestiona las solicitudes de la comunidad.'
            : 'Crea solicitudes y revisa su avance.' }}
        </p>
      </div>
    </section>

    @if (barrio.cargando()) {
      <div class="loading-bar">
        <span class="spinner spinner-dark"></span>
        <span>Cargando trámites...</span>
      </div>
    }

    @if (barrio.errorHttp() && !error) {
      <div class="notice error" role="alert">{{ barrio.errorHttp() }}</div>
    }
    @if (error) {
      <div class="notice error" role="alert">{{ error }}</div>
    }

    @if (mensaje) {
      <div class="notice success" role="status">{{ mensaje }}</div>
    }

    @if (auth.puedeCrear) {
      <section class="card">
        <h2>{{ editandoId ? 'Editar trámite' : 'Nuevo trámite' }}</h2>

        <form (ngSubmit)="guardar()">
          <div class="form-grid">
            <label>
              Tipo de trámite

              <select
                name="tipoId"
                [(ngModel)]="tipoId"
                [disabled]="!!editandoId"
                required
              >
                <option value="">Selecciona un tipo</option>

                @for (tipo of tiposFormulario; track tipo.id) {
                  <option [value]="tipo.id">
                    {{ tipo.nombre }}
                  </option>
                }
              </select>
            </label>

            <label>
              Asunto

              <input
                name="asunto"
                [(ngModel)]="asunto"
                minlength="5"
                maxlength="100"
                required
              />
            </label>

            @if (!editandoId) {
              <label>Correo de notificación
                <input type="email" name="clienteEmail" [(ngModel)]="clienteEmail" maxlength="150" required />
              </label>
            }
            <label class="full-width">
              Descripción

              <textarea
                name="descripcion"
                [(ngModel)]="descripcion"
                rows="4"
                minlength="10"
                maxlength="1000"
                required
              ></textarea>
            </label>
          </div>

          <div class="actions">
            <button type="submit" [disabled]="barrio.cargando()">
              @if (barrio.cargando()) {
                <span class="spinner"></span>
              }
              {{ editandoId ? 'Guardar cambios' : 'Ingresar trámite' }}
            </button>

            @if (editandoId) {
              <button
                type="button"
                class="secondary"
                (click)="limpiarFormulario()"
              >
                Cancelar edición
              </button>
            }
          </div>
        </form>
      </section>
    } @else {
      <div class="notice">
        Tu cuenta necesita un rol de BarrioDigital para operar.
      </div>
    }

    <section class="card">
      <h2>{{ auth.puedeVerTodos ? 'Solicitudes' : 'Mis solicitudes' }}</h2>

      <div class="form-grid filters">
        <label>
          Buscar
          <input
            [(ngModel)]="busqueda"
            placeholder="Asunto, tipo o solicitante"
          />
        </label>

        <label>
          Estado
          <select [(ngModel)]="filtroEstado">
            <option value="">Todos</option>

            @for (estado of estados; track estado) {
              <option [value]="estado">{{ etiquetas[estado] }}</option>
            }
          </select>
        </label>
      </div>

      @for (tramite of filtrados; track tramite.id) {
        <article class="request-item">
          <div class="list-row">
            <div>
              <h3>{{ tramite.asunto }}</h3>
              <p class="muted">{{ tramite.tipoNombre }}</p>
            </div>

            <span class="badge">{{ etiquetas[tramite.estado] }}</span>
          </div>

          <p class="description">{{ tramite.descripcion }}</p>

          <p class="muted">
            Solicitante: {{ tramite.propietarioNombre }}
          </p>

          <p class="muted">
            Ingresado:
            {{ tramite.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}
            · Actualizado:
            {{ tramite.fechaActualizacion | date:'dd/MM/yyyy HH:mm' }}
          </p>

          <div class="actions">
            @if (barrio.puedeEditar(tramite)) {
              <button
                type="button"
                class="secondary"
                (click)="editar(tramite)"
              >
                Editar
              </button>

              <button
                type="button"
                class="danger"
                (click)="eliminar(tramite)"
              >
                Eliminar
              </button>
            }

            @if (auth.puedeCambiarEstado) {
              @for (estado of transiciones[tramite.estado]; track estado) {
                <button
                  type="button"
                  [class.danger]="estado === 'RECHAZADO'"
                  (click)="cambiarEstado(tramite.id, estado)"
                >
                  {{ estado === 'RECHAZADO'
                    ? 'Rechazar'
                    : 'Pasar a ' + etiquetas[estado] }}
                </button>
              }
            }
          </div>
        </article>
      } @empty {
        <p class="muted">No hay trámites para mostrar con estos filtros.</p>
      }
    </section>
  `,
})
export class RequestsComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly barrio = inject(BarrioService);

  readonly etiquetas = ETIQUETAS_ESTADO;
  readonly transiciones = TRANSICIONES;
  readonly estados = Object.keys(ETIQUETAS_ESTADO) as EstadoTramite[];

  tipoId = '';
  clienteEmail = '';
  asunto = '';
  descripcion = '';
  editandoId = '';

  busqueda = '';
  filtroEstado = '';

  error = '';
  mensaje = '';

  ngOnInit(): void {
    this.barrio.cargarDesdeBackend();
  }

  get tiposFormulario() {
    return this.barrio.tipos().filter(
      (tipo) => tipo.activo || tipo.id === this.tipoId,
    );
  }

  get filtrados(): Tramite[] {
    const texto = this.busqueda.trim().toLowerCase();

    return this.barrio.tramitesVisibles().filter((tramite) => {
      const coincideEstado =
        !this.filtroEstado || tramite.estado === this.filtroEstado;

      const contenido =
        `${tramite.asunto} ${tramite.tipoNombre} ${tramite.propietarioNombre}`
          .toLowerCase();

      return coincideEstado && contenido.includes(texto);
    });
  }

  guardar(): void {
    this.ejecutar(async () => {
      if (this.editandoId) {
        await this.barrio.editarTramite(
          this.editandoId,
          this.asunto,
          this.descripcion,
        );
      } else {
        await this.barrio.crearTramite(
          this.tipoId,
          this.asunto,
          this.descripcion,
          this.clienteEmail,
        );
      }

      this.limpiarFormulario();
    }, 'Trámite guardado correctamente.');
  }

  editar(tramite: Tramite): void {
    this.error = '';
    this.mensaje = '';

    this.editandoId = tramite.id;
    this.tipoId = tramite.tipoId;
    this.asunto = tramite.asunto;
    this.descripcion = tramite.descripcion;

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  eliminar(tramite: Tramite): void {
    if (!window.confirm('¿Eliminar este trámite?')) {
      return;
    }

    this.ejecutar(async () => {
      await this.barrio.eliminarTramite(tramite.id);

      if (this.editandoId === tramite.id) {
        this.limpiarFormulario();
      }
    }, 'Trámite eliminado.');
  }

  cambiarEstado(id: string, estado: EstadoTramite): void {
    if (
      estado === 'RECHAZADO' &&
      !window.confirm('¿Rechazar este trámite?')
    ) {
      return;
    }

    this.ejecutar(async () => {
      await this.barrio.cambiarEstado(id, estado);

      if (this.editandoId === id) {
        this.limpiarFormulario();
      }
    }, 'Estado actualizado.');
  }

  limpiarFormulario(): void {
    this.editandoId = '';
    this.tipoId = '';
    this.asunto = '';
    this.descripcion = '';
  }

  private async ejecutar(accion: () => Promise<void>, mensaje: string): Promise<void> {
    this.error = '';
    this.mensaje = '';

    try {
      await accion();
      this.mensaje = mensaje;
    } catch (error) {
      this.error = error instanceof Error
        ? error.message
        : 'No se pudo completar la operación.';
    }
  }
}