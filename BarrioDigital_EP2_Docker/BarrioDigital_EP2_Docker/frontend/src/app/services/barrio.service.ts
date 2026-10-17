import { Injectable,inject,signal } from '@angular/core';
import { firstValueFrom,forkJoin,Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { OrderService } from './order.service';
import { EstadoTramite,TipoTramite,Tramite } from '../models/barrio.models';
@Injectable({providedIn:'root'})
export class BarrioService {
  private auth=inject(AuthService);private api=inject(OrderService);
  private loading=signal(false);readonly cargando=this.loading.asReadonly();
  private error=signal<string|null>(null);readonly errorHttp=this.error.asReadonly();
  private catalog=signal<TipoTramite[]>([]);readonly tipos=this.catalog.asReadonly();
  private requests=signal<Tramite[]>([]);
  cargarDesdeBackend():void {
    if(!this.auth.isLoggedIn())return;
    this.loading.set(true);this.error.set(null);
    forkJoin({requests:this.api.getOrders(),catalog:this.api.getCatalog()}).subscribe({
      next:data=>{this.requests.set(data.requests);this.catalog.set(data.catalog);this.loading.set(false);},
      error:e=>{this.error.set(e.message);this.loading.set(false);}
    });
  }
  limpiarErrorHttp(){this.error.set(null);}
  // La API filtra por el propietario del token; no se confía en nombres enviados desde el navegador.
  tramitesVisibles(){return this.requests();}
  cuposDisponibles(tipoId:string){
    const tipo=this.catalog().find(t=>t.id===tipoId);if(!tipo)return 0;
    const day=new Date().toLocaleDateString('en-CA');
    return Math.max(0,tipo.cupoDiario-this.requests().filter(t=>t.tipoId===tipoId&&t.fechaAdmision&&new Date(t.fechaAdmision).toLocaleDateString('en-CA')===day).length);
  }
  puedeEditar(t:Tramite){return t.estado==='INGRESADO'&&(this.auth.puedeVerTodos||t.propietarioId===this.auth.userId);}
  private async run<T>(request:Observable<T>):Promise<T>{
    this.loading.set(true);this.error.set(null);
    try{return await firstValueFrom(request);}catch(e){const message=e instanceof Error?e.message:'No se pudo guardar';this.error.set(message);throw e;}finally{this.loading.set(false);}
  }
  async crearTramite(tipoId:string,asunto:string,descripcion:string,clienteEmail:string){
    const t=await this.run(this.api.createOrder({tipoId,asunto,descripcion,clienteEmail}));this.requests.update(all=>[t,...all]);
  }
  async editarTramite(id:string,asunto:string,descripcion:string){const t=await this.run(this.api.editOrder(id,asunto,descripcion));this.replace(t);}
  async eliminarTramite(id:string){await this.run(this.api.deleteOrder(id));this.requests.update(all=>all.filter(t=>t.id!==id));}
  async cambiarEstado(id:string,estado:EstadoTramite){const t=await this.run(this.api.updateOrderStatus(id,estado));this.replace(t);}
  async guardarTipo(datos:Omit<TipoTramite,'id'>,id?:string){
    const t=await this.run(this.api.saveCatalog({...datos,id}));this.catalog.update(all=>id?all.map(x=>x.id===id?t:x):[...all,t]);
  }
  async eliminarTipo(id:string){await this.run(this.api.deleteCatalog(id));this.catalog.update(all=>all.map(t=>t.id===id?{...t,activo:false}:t));}
  private replace(t:Tramite){this.requests.update(all=>all.map(x=>x.id===t.id?t:x));}
}
