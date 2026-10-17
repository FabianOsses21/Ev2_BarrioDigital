import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './services/auth.service';

@Component({
  standalone: true,
  template: '<div>Dashboard Mock</div>',
})
class MockDashboardComponent {}

describe('App', () => {
  const authServiceMock = {
    isLoggedIn: () => true,
    username: 'Axel',
    roles: ['Admin'],
    puedeCrear: true,
    puedeVerCatalogo: true,
    logout: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([{ path: 'dashboard', component: MockDashboardComponent }]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();
  });

  it('debe crear el componente principal', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('debe renderizar el nombre de la plataforma BarrioDigital cuando está autenticado', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand')?.textContent).toContain('BarrioDigital');
  });

  it('debe ejecutar logout al accionar el botón de salir', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    app.logout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});
