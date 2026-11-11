// src/app/services/admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface AdminLoginDTO {
  username: string;
  password: string;
  rol: boolean;
}

export interface SorteoDTO {
  nombreArticulo:     string;
  descripcion:        string;
  fechaInicio:        string;         // Formato "YYYY-MM-DD"
  fechaFin:           string;         // Formato "YYYY-MM-DD"
  puntosNecesarios:   number;
  imagen:             File | null;    // Para adjuntar la imagen en FormData
  categoria:          string;
}

export interface ExpertDTO {
  nombreUsuario:     string;
  correoElectronico: string;
  contrasena:        string;
  experto:           boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private api = `${environment.apiUrl}/admin`;

  // Cabecera exigida por el backend en las peticiones de administración (protección CSRF).
  private readonly adminHeaders = new HttpHeaders({ 'X-Requested-With': 'BidCo' });

  constructor(private http: HttpClient) {}

  loginAdmin(dto: AdminLoginDTO): Observable<void> {
    return this.http.post<void>(
      `${this.api}/login`,
      dto, 
      { withCredentials: true }
    ).pipe(
      catchError(err => throwError(() => new Error(err.error?.message || 'Login admin fallido')))
    );
  }

  createSorteo(sorteo: any, imagen: File | null) {
    const formData = new FormData();
    formData.append('sorteo', new Blob([JSON.stringify({
      nombreArticulo: sorteo.nombreArticulo,
      descripcion: sorteo.descripcion,
      fechaInicio: sorteo.fechaInicio,
      fechaFin: sorteo.fechaFin,
      puntosNecesarios: sorteo.puntosNecesarios,
      categoria: sorteo.categoria
    })], { type: 'application/json' }));
  
    if (imagen) {
      formData.append('imagen', imagen, imagen.name);
    }

    return this.http.post<void>(`${this.api}/sorteos`, formData, { withCredentials: true, headers: this.adminHeaders });
  }
  

  /** Retira una subasta por moderación. Solo la administración; el motivo llega al creador por correo. */
  retirarSubasta(id: number, motivo: string): Observable<void> {
    return this.http.post<void>(
      `${this.api}/subastas/${id}/retirar`,
      { motivo },
      { withCredentials: true, headers: this.adminHeaders }
    );
  }

  createExpert(dto: ExpertDTO): Observable<void> {
    return this.http.post<void>(
      `${environment.apiUrl}/trabajadores`, // Corregido: antes era relativo
      dto,
      { withCredentials: true, headers: this.adminHeaders }
    );
  }
}