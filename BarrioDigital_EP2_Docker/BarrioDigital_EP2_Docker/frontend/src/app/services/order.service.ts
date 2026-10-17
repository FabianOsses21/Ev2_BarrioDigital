import { Injectable,inject } from '@angular/core';
import { HttpClient,HttpErrorResponse } from '@angular/common/http';
import { Observable,catchError,throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { EstadoTramite,TipoTramite,Tramite } from '../models/barrio.models';
@Injectable({providedIn:'root'})
export class OrderService {
  private http=inject(HttpClient);
  private base=`${environment.apiUrl}/api`;
  private error=(e:HttpErrorResponse)=>throwError(()=>new Error(e.error?.message??(e.status===403?'No tienes permiso para esta operación.':e.status===401?'Inicia sesión nuevamente.':'No se pudo completar la operación en el servidor.')));
  getOrders(){return this.http.get<Tramite[]>(`${this.base}/requests`).pipe(catchError(this.error));}
  getOrderById(id:string){return this.http.get<Tramite>(`${this.base}/requests/${encodeURIComponent(id)}`).pipe(catchError(this.error));}
  createOrder(payload:{tipoId:string;asunto:string;descripcion:string;clienteEmail:string}){return this.http.post<Tramite>(`${this.base}/requests`,payload).pipe(catchError(this.error));}
  editOrder(id:string,asunto:string,descripcion:string){return this.http.put<Tramite>(`${this.base}/requests/${encodeURIComponent(id)}`,{asunto,descripcion}).pipe(catchError(this.error));}
  deleteOrder(id:string){return this.http.delete<void>(`${this.base}/requests/${encodeURIComponent(id)}`).pipe(catchError(this.error));}
  updateOrderStatus(id:string,estado:EstadoTramite){return this.http.put<Tramite>(`${this.base}/requests/${encodeURIComponent(id)}/estado`,{estado}).pipe(catchError(this.error));}
  getCatalog(){return this.http.get<TipoTramite[]>(`${this.base}/catalog?soloActivos=false`).pipe(catchError(this.error));}
  saveCatalog(payload:Omit<TipoTramite,'id'> & {id?:string}){return this.http.post<TipoTramite>(`${this.base}/catalog`,payload).pipe(catchError(this.error));}
  deleteCatalog(id:string){return this.http.delete<void>(`${this.base}/catalog/${encodeURIComponent(id)}`).pipe(catchError(this.error));}
  checkProtectedAccess(){return this.http.get(`${this.base}/ordenes`,{responseType:'text'}).pipe(catchError(this.error));}
}
