import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { environment } from '../../environments/environment';
import { Tramite, TipoTramite } from '../models/barrio.models';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrderService],
    });
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  it('debe obtener órdenes/trámites via GET /api/requests', () => {
    const mockTramites: Tramite[] = [
      {
        id: '1',
        tipoId: 'luminarias',
        tipoNombre: 'Reparación de luminarias',
        asunto: 'Foco quemado',
        descripcion: 'Poste 12 sin luz',
        propietarioId: 'user-123',
        propietarioNombre: 'Juan Pérez',
        estado: 'INGRESADO',
        fechaCreacion: '2026-09-12T10:00:00Z',
        fechaActualizacion: '2026-09-12T10:00:00Z',
      },
    ];

    service.getOrders().subscribe((data) => {
      expect(data.length).toBe(1);
      expect(data[0].asunto).toBe('Foco quemado');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/requests`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTramites);
  });

  it('debe crear un trámite via POST /api/requests', () => {
    const payload = {
      tipoId: 'luminarias',
      asunto: 'Nuevo trámite',
      clienteEmail: 'vecino@example.test',
      descripcion: 'Descripción del trámite',
      propietarioId: 'user-123',
      propietarioNombre: 'Juan Pérez',
    };

    const mockResponse: Tramite = {
      id: 'uuid-1',
      ...payload,
      tipoNombre: 'Reparación de luminarias',
      estado: 'INGRESADO',
      fechaCreacion: new Date().toISOString(),
      fechaActualizacion: new Date().toISOString(),
    };

    service.createOrder(payload).subscribe((data) => {
      expect(data.id).toBe('uuid-1');
      expect(data.asunto).toBe('Nuevo trámite');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/requests`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockResponse);
  });

  it('debe actualizar estado de un trámite via PUT /api/requests/:id/estado', () => {
    const mockResponse: Tramite = {
      id: 'uuid-1',
      tipoId: 'luminarias',
      tipoNombre: 'Reparación de luminarias',
      asunto: 'Trámite',
      descripcion: 'Descripción',
      propietarioId: 'user-123',
      propietarioNombre: 'Juan Pérez',
      estado: 'ADMITIDO',
      fechaCreacion: new Date().toISOString(),
      fechaActualizacion: new Date().toISOString(),
    };

    service.updateOrderStatus('uuid-1', 'ADMITIDO').subscribe((data) => {
      expect(data.estado).toBe('ADMITIDO');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/requests/uuid-1/estado`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ estado: 'ADMITIDO' });
    req.flush(mockResponse);
  });

  it('debe obtener catálogo de trámites via GET /api/catalog', () => {
    const mockCatalogo: TipoTramite[] = [
      {
        id: 'luminarias',
        nombre: 'Reparación de luminarias',
        descripcion: 'Alumbrado público',
        cupoDiario: 5,
        activo: true,
      },
    ];

    service.getCatalog().subscribe((data) => {
      expect(data.length).toBe(1);
      expect(data[0].id).toBe('luminarias');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/catalog?soloActivos=false`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCatalogo);
  });

  it('debe manejar error 401 con mensaje amigable', () => {
    let errorCapturado: Error | null = null;
    service.getOrders().subscribe({
      next: () => {},
      error: (error: Error) => {
        errorCapturado = error;
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/requests`);
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(errorCapturado).toBeTruthy();
    expect(errorCapturado!.message).toContain('sesión');
  });

  it('debe manejar error 403 con mensaje amigable', () => {
    let errorCapturado: Error | null = null;
    service.getOrders().subscribe({
      next: () => {},
      error: (error: Error) => {
        errorCapturado = error;
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/requests`);
    req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });

    expect(errorCapturado).toBeTruthy();
    expect(errorCapturado!.message).toContain('permiso');
  });

  it('debe manejar error 500 de microservicio con mensaje amigable', () => {
    let errorCapturado: Error | null = null;
    service.getOrders().subscribe({
      next: () => {},
      error: (error: Error) => {
        errorCapturado = error;
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/requests`);
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

    expect(errorCapturado).toBeTruthy();
    expect(errorCapturado!.message).toContain('servidor');
  });
});
