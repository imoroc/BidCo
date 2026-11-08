import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { UserService } from './user.service';
import { ExpertoResponse, ExpertService } from './expert.service';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../environments/environment';

export interface UsuarioResponse {
  usuarioID: number;
  nombreUsuario: string;
  correoElectronico: string;
  contrasena?: string;
  ciudad: string;
  codigoPostal: string;
  calle: string;
  numeroPiso: number;
  letraPiso?: string;
  pais: string;
  saldo?: number;
  puntos?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private loggedIn = new BehaviorSubject<boolean>(false);
  public currentUser = new BehaviorSubject<UsuarioResponse | null>(null);
  private userRole = new BehaviorSubject<string | null>(null);

  public isLoggedIn$ = this.loggedIn.asObservable();
  public currentUser$ = this.currentUser.asObservable();
  public userRole$ = this.userRole.asObservable();

  private userApiUrl: string = `${environment.apiUrl}/usuarios`;

  constructor(
    private http: HttpClient,
    private userService: UserService,
    private expertService: ExpertService
  ) {
    const token = localStorage.getItem('authToken');
    const userData = localStorage.getItem('authUser');

    if (token && userData) {
      this.loggedIn.next(true);
      this.currentUser.next(JSON.parse(userData));

      this.loadUserProfile(this.currentUser.value!.usuarioID).subscribe({
        next: (updatedUser) => {
          localStorage.setItem('authUser', JSON.stringify(updatedUser));
          this.currentUser.next(updatedUser);
        },
        error: () => {
          // Si falla la recarga del perfil, no cerramos sesión
        }
      });
    }
  }

  loginUser(loginData: { identificador: string; contrasena: string }) {
    return this.http.post<{ token: string }>(
      `${this.userApiUrl}/login`,
      loginData,
      { withCredentials: true }
    );
  }

  setUserFromToken(token: string): void {
    localStorage.setItem('authToken', token);
    this.loggedIn.next(true);
    this.userService
      .getUserById(+(jwtDecode(token).sub ?? 0))
      .subscribe({
        next: (user) => this.setUser(user),
        error: () => this.logout()
      });
  }

  loadUserProfile(userID: number): Observable<UsuarioResponse> {
    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.get<UsuarioResponse>(`${this.userApiUrl}/${userID}`, { headers });
  }

  setUser(user: UsuarioResponse): void {
    this.currentUser.next(user);
    localStorage.setItem('authUser', JSON.stringify(user));
    this.setUserRole('user');
  }

  setUserRole(role: string | null): void {
    this.userRole.next(role);
  }

  /** Lectura síncrona de la sesión de usuario (usada por userGuard). */
  isLoggedIn(): boolean {
    return this.loggedIn.getValue();
  }

  // Método público para leer el rol de forma síncrona (usado por AdminGuard)
  getCurrentRole(): string | null {
    return this.userRole.getValue();
  }

  logout(): void {
    this.loggedIn.next(false);
    this.currentUser.next(null);
    this.setUserRole(null);
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUser');
  }
}