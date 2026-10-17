import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../environments/environment';
export const demoInterceptor:HttpInterceptorFn=(req,next)=>{
  if(environment.authMode==='demo' && req.url.startsWith(environment.apiUrl+'/api/')) {
    req=req.clone({setHeaders:{'X-Demo-Role':sessionStorage.getItem('bd-demo-role')??'',
      'X-Demo-User':sessionStorage.getItem('bd-demo-user')??'demo-vecino'}});
  }
  return next(req);
};
