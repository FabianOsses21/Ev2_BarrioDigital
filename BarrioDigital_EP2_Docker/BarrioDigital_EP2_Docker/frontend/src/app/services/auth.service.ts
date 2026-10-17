import { Router } from '@angular/router';
import { Injectable, inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';

import { environment } from '../../environments/environment';
import { Rol } from '../models/barrio.models';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private router = inject(Router);
  readonly demo = environment.authMode === 'demo';
  demoLogin(role: Rol): void {
    if (!this.demo) return;
    sessionStorage.setItem('bd-demo-role', role);
    sessionStorage.setItem('bd-demo-user', 'demo-' + role.toLowerCase());
    window.location.assign('/dashboard');
  }
  private msalService = inject(MsalService);

    login(): void {
  this.msalService.loginRedirect({
    scopes: [
      'openid',
      'profile',
      environment.azure.backendScope,
    ],
    prompt: 'select_account',
    redirectStartPage: `${window.location.origin}/dashboard`,
  });
}
    registrar(): void {
    this.msalService.loginRedirect({
      scopes: ['openid', 'profile', environment.azure.backendScope],
      prompt: 'select_account',
      redirectStartPage: window.location.origin + '/',
    });
  }

  logout(): void {
    if (this.demo) { sessionStorage.removeItem('bd-demo-role'); sessionStorage.removeItem('bd-demo-user'); window.location.assign('/'); return; }
    this.msalService.logoutRedirect({
      account: this.getAccount() ?? undefined,
      postLogoutRedirectUri: environment.azure.redirectUri,
    });
  }

  getAccount() {
    return this.msalService.instance.getActiveAccount();
  }

  isLoggedIn(): boolean {
    return this.demo ? !!sessionStorage.getItem('bd-demo-role') : !!this.getAccount();
  }

  get userId(): string {
    if (this.demo) return sessionStorage.getItem('bd-demo-user') ?? '';
    const account=this.getAccount();
    return String(account?.idTokenClaims?.['oid'] ?? account?.localAccountId ?? '');
  }

  get username(): string {
    if (this.demo) return 'Cuenta de prueba (' + this.roles.join(', ') + ')';
    const account = this.getAccount();

    return account?.name ?? account?.username ?? '';
  }

  get roles(): Rol[] {
    if (this.demo) { const role=sessionStorage.getItem('bd-demo-role'); return ['Admin','Funcionario','Vecino'].includes(role??'') ? [role as Rol] : []; }
    const claims = this.getAccount()?.idTokenClaims as
      | { roles?: unknown }
      | undefined;

    const valores = Array.isArray(claims?.roles)
      ? claims.roles.filter(
          (valor): valor is string => typeof valor === 'string',
        )
      : [];

    const equivalencias: Record<string, Rol> = {
      Admin: 'Admin',
      Administrador: 'Admin',
      Funcionario: 'Funcionario',
      Vecino: 'Vecino',

      ROLE_ADMINISTRADOR: 'Admin',
      ROLE_OPERADOR: 'Funcionario',
      ROLE_CLIENTE: 'Vecino',
    };

    return [...new Set(
      valores
        .map((valor) => equivalencias[valor])
        .filter((rol): rol is Rol => !!rol),
    )];
  }

  hasAnyRole(...roles: Rol[]): boolean {
    return roles.some((rol) => this.roles.includes(rol));
  }

  get puedeCrear(): boolean {
    return this.hasAnyRole('Admin', 'Funcionario', 'Vecino');
  }

  get puedeVerTodos(): boolean {
    return this.hasAnyRole('Admin', 'Funcionario');
  }

  get puedeCambiarEstado(): boolean {
    return this.hasAnyRole('Admin', 'Funcionario');
  }

  get puedeVerCatalogo(): boolean {
    return this.hasAnyRole('Admin', 'Funcionario');
  }

  get puedeGestionarCatalogo(): boolean {
    return this.hasAnyRole('Admin');
  }
  mostrarRolesAzure(): void {
  const account = this.getAccount();

  console.log(
    'Roles recibidos de Azure:',
    account?.idTokenClaims?.['roles'] ?? 'Sin claim roles',
  );

  console.log('Roles reconocidos por BarrioDigital:', this.roles);
}
}