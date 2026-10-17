import { TestBed } from '@angular/core/testing';
import { of,throwError,Subject } from 'rxjs';
import { BarrioService } from './barrio.service';
import { AuthService } from './auth.service';
import { OrderService } from './order.service';
import { Tramite } from '../models/barrio.models';
describe('Persistencia de BarrioService',()=>{
  let service:BarrioService;
  const row:Tramite={id:'server-id',tipoId:'luminarias',tipoNombre:'Luminarias',asunto:'Poste sin luz',descripcion:'Poste apagado en la calle',propietarioId:'user-1',propietarioNombre:'Vecino',estado:'INGRESADO',fechaCreacion:'2026-10-17T00:00:00',fechaActualizacion:'2026-10-17T00:00:00'};
  const api={getOrders:vi.fn(),getCatalog:vi.fn(),createOrder:vi.fn(),editOrder:vi.fn(),deleteOrder:vi.fn()};
  beforeEach(()=>{
    vi.resetAllMocks();api.getOrders.mockReturnValue(of([]));api.getCatalog.mockReturnValue(of([]));
    TestBed.configureTestingModule({providers:[BarrioService,{provide:AuthService,useValue:{isLoggedIn:()=>true,userId:'user-1',puedeVerTodos:false}},{provide:OrderService,useValue:api}]});service=TestBed.inject(BarrioService);
  });
  it('no muestra un trámite antes de la confirmación HTTP',async()=>{
    const reply=new Subject<Tramite>();api.createOrder.mockReturnValue(reply);
    const promise=service.crearTramite('luminarias','Poste sin luz','Poste apagado','ok@example.test');
    expect(service.tramitesVisibles()).toEqual([]);reply.next(row);reply.complete();await promise;
    expect(service.tramitesVisibles()[0].id).toBe('server-id');
  });
  it('el error de guardado se muestra y no crea un registro ficticio',async()=>{
    api.createOrder.mockReturnValue(throwError(()=>new Error('Servidor caído')));
    await expect(service.crearTramite('luminarias','Poste sin luz','Poste apagado','ok@example.test')).rejects.toThrow('Servidor caído');
    expect(service.tramitesVisibles()).toEqual([]);expect(service.errorHttp()).toBe('Servidor caído');
  });
  it('acepta una lista vacía del servidor después de una eliminación',()=>{
    api.getOrders.mockReturnValue(of([row]));service.cargarDesdeBackend();expect(service.tramitesVisibles()).toHaveLength(1);
    api.getOrders.mockReturnValue(of([]));service.cargarDesdeBackend();expect(service.tramitesVisibles()).toEqual([]);
  });
  it('una eliminación fallida conserva el trámite',async()=>{
    api.getOrders.mockReturnValue(of([row]));service.cargarDesdeBackend();api.deleteOrder.mockReturnValue(throwError(()=>new Error('Conflicto')));
    await expect(service.eliminarTramite(row.id)).rejects.toThrow();expect(service.tramitesVisibles()).toHaveLength(1);
  });
});
