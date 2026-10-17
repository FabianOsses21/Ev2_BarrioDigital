export type Rol = 'Admin' | 'Funcionario' | 'Vecino';

export type EstadoTramite =
  | 'INGRESADO'
  | 'ADMITIDO'
  | 'EN_GESTION'
  | 'EN_TERRENO'
  | 'RESUELTO'
  | 'RECHAZADO';

export interface TipoTramite {
  id: string;
  nombre: string;
  descripcion: string;
  cupoDiario: number;
  activo: boolean;
}

export interface Tramite {
  clienteEmail?: string;
  id: string;

  tipoId: string;
  tipoNombre: string;

  asunto: string;
  descripcion: string;

  propietarioId: string;
  propietarioNombre: string;

  estado: EstadoTramite;

  fechaCreacion: string;
  fechaActualizacion: string;
  fechaAdmision?: string;
}

export const TRANSICIONES: Record<EstadoTramite, EstadoTramite[]> = {
  INGRESADO: ['ADMITIDO', 'RECHAZADO'],
  ADMITIDO: ['EN_GESTION', 'RECHAZADO'],
  EN_GESTION: ['EN_TERRENO', 'RECHAZADO'],
  EN_TERRENO: ['RESUELTO', 'RECHAZADO'],
  RESUELTO: [],
  RECHAZADO: [],
};

export const ETIQUETAS_ESTADO: Record<EstadoTramite, string> = {
  INGRESADO: 'Ingresado',
  ADMITIDO: 'Admitido',
  EN_GESTION: 'En gestión',
  EN_TERRENO: 'En terreno',
  RESUELTO: 'Resuelto',
  RECHAZADO: 'Rechazado',
};