import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { of } from 'rxjs';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  const authServiceMock = {
    getAccount: () => ({ username: 'testuser', name: 'Test User' }),
    logout: () => {},
  };

  const apiServiceMock = {
    obtenerPublico: () => of('API pública OK'),
    obtenerOrdenes: () => of('API protegida OK'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: ApiService, useValue: apiServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe retornar el nombre de usuario de authService', () => {
    expect(component.username).toBe('testuser');
  });

  it('debe llamar a obtenerPublico y actualizar mensaje', () => {
    component.probarPublico();
    expect(component.mensajePublico).toBe('API pública OK');
  });

  it('debe llamar a obtenerOrdenes y actualizar mensaje', () => {
    component.probarProtegido();
    expect(component.mensajeProtegido).toBe('API protegida OK');
  });
});
