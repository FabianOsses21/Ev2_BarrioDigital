import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../services/auth.service';
import { BarrioService } from '../../services/barrio.service';
import { TipoTramite } from '../../models/barrio.models';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [FormsModule],

  template: `
    <section class="page-heading">
      <div>
        <h1>Catálogo</h1>
        <p>Tipos de trámite y disponibilidad diaria.</p>
      </div>
    </section>

    @if (barrio.cargando()) {
      <div class="loading-bar">
        <span class="spinner spinner-dark"></span>
        <span>Cargando catálogo...</span>
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

    @if (auth.puedeGestionarCatalogo) {
      <section class="card">
        <h2>{{ editandoId ? 'Editar tipo' : 'Nuevo tipo de trámite' }}</h2>

        <form (ngSubmit)="guardar()">
          <div class="form-grid">
            <label>
              Nombre
              <input
                name="nombre"
                [(ngModel)]="nombre"
                maxlength="100"
                required
              />
            </label>

            <label>
              Cupo diario
              <input
                type="number"
                name="cupoDiario"
                [(ngModel)]="cupoDiario"
                min="0"
                step="1"
                required
              />
            </label>

            <label class="full-width">
              Descripción
              <textarea
                name="descripcion"
                [(ngModel)]="descripcion"
                rows="3"
                maxlength="500"
                required
              ></textarea>
            </label>

            <label class="checkbox-label">
              <input
                type="checkbox"
                name="activo"
                [(ngModel)]="activo"
              />
              Tipo activo
            </label>
          </div>

          <div class="actions">
            <button type="submit" [disabled]="barrio.cargando()">
              @if (barrio.cargando()) {
                <span class="spinner"></span>
              }
              Guardar tipo
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
    }

    <section class="catalog-grid">
      @for (tipo of barrio.tipos(); track tipo.id) {
        <article class="card">
          <span class="badge">
            {{ tipo.activo ? 'Activo' : 'Inactivo' }}
          </span>

          <h2>{{ tipo.nombre }}</h2>

          <p>{{ tipo.descripcion }}</p>

          <p><strong>Cupo diario:</strong> {{ tipo.cupoDiario }}</p>

          <p>
            <strong>Disponibles hoy:</strong>
            {{ barrio.cuposDisponibles(tipo.id) }}
          </p>

          @if (auth.puedeGestionarCatalogo) {
            <div class="actions">
              <button
                type="button"
                class="secondary"
                (click)="editar(tipo)"
              >
                Editar
              </button>

              <button
                type="button"
                class="danger"
                (click)="eliminar(tipo.id)"
              >
                Desactivar
              </button>
            </div>
          }
        </article>
      } @empty {
        <div class="card">No hay tipos de trámite registrados.</div>
      }
    </section>
  `,
})
export class CatalogComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly barrio = inject(BarrioService);

  ngOnInit(): void {
    this.barrio.cargarDesdeBackend();
  }

  editandoId = '';

  nombre = '';
  descripcion = '';
  cupoDiario = 1;
  activo = true;

  error = '';
  mensaje = '';

  guardar(): void {
    this.ejecutar(async () => {
      await this.barrio.guardarTipo(
        {
          nombre: this.nombre,
          descripcion: this.descripcion,
          cupoDiario: this.cupoDiario,
          activo: this.activo,
        },
        this.editandoId || undefined,
      );

      this.limpiarFormulario();
    }, 'Tipo de trámite guardado.');
  }

  editar(tipo: TipoTramite): void {
    this.error = '';
    this.mensaje = '';

    this.editandoId = tipo.id;
    this.nombre = tipo.nombre;
    this.descripcion = tipo.descripcion;
    this.cupoDiario = tipo.cupoDiario;
    this.activo = tipo.activo;

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  eliminar(id: string): void {
    if (!window.confirm('¿Desactivar este tipo de trámite? Se conservará su historial.')) {
      return;
    }

    this.ejecutar(async () => {
      await this.barrio.eliminarTipo(id);

      if (this.editandoId === id) {
        this.limpiarFormulario();
      }
    }, 'Tipo de trámite desactivado.');
  }

  limpiarFormulario(): void {
    this.editandoId = '';
    this.nombre = '';
    this.descripcion = '';
    this.cupoDiario = 1;
    this.activo = true;
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
