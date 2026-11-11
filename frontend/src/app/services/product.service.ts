import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SubastaResponseDto {
    subastaID: number;
    nombreArticulo: string;
    descripcion: string;
    fechaInicial: string;
    fechaFinal: string;
    precioInicial: number;
    precioFinal: number;
    imagen: string;
    creadorId: number;
    comentarios: any[];
    pujas: any[];
    privacidad_anonimo: boolean;
    /** false en las subastas a ciegas (el precio final no se muestra). */
    subastaNormal?: boolean;
}

export interface SorteoResponseDto {
    sorteoID: number;
    nombreArticulo: string;
    descripcion: string;
    fechaInicio: string;
    fechaFin: string;
    puntosNecesarios: number;
    imagen: string;
    ganador: number;
}

export interface PerfilPublicoDto {
    nombreUsuario: string;
    privacidadAnonimoPujas: boolean;
}

export interface PujaCreateDTO {
    importe: number;
    fecha: string;
    subastaId: number;
    pujadorId: number;
}

export interface PujaResponseDTO {
    pujaID: number;
    importe: number;
    fecha: string;
    ganadora: boolean;
    subastaID: number;
    pujadorID: number;
}

export interface PujaSorteoCreateDTO {
    puntos: number;
    fecha: string;
    sorteoId: number;
    pujadorId: number;
}

export interface PujaSorteoResponseDTO {
    pujaID: number;
    puntos: number;
    fecha: string;
    sorteoID: number;
    pujadorID: number;
}

@Injectable({ providedIn: 'root' })
export class ProductoService {
    private subastaUrl = `${environment.apiUrl}/subastas`;
    private pujaSubastaUrl = `${environment.apiUrl}/pujas`;
    private sorteoUrl = `${environment.apiUrl}/sorteos`;
    private pujaSorteoUrl = `${environment.apiUrl}/pujas-sorteo`;
    private usuarioUrl = `${environment.apiUrl}/usuarios`;

    constructor(private http: HttpClient) {}

    getSubastaPorId(id: number): Observable<any> {
        return this.http.get(`${this.subastaUrl}/${id}`);
    }

    getSorteoPorId(id: number): Observable<any> {
        return this.http.get(`${this.sorteoUrl}/${id}`);
    }

    getPerfilPublicoPorId(id: number): Observable<PerfilPublicoDto> {
        return this.http.get<PerfilPublicoDto>(`${this.usuarioUrl}/${id}/publico`);
    }

    crearPuja(puja: PujaCreateDTO): Observable<PujaResponseDTO> {
        return this.http.post<PujaResponseDTO>(this.pujaSubastaUrl, puja);
    }

    obtenerPujaMaximaPorSubasta(subastaId: number): Observable<PujaResponseDTO> {
        return this.http.get<PujaResponseDTO>(`${this.pujaSubastaUrl}/mayor/${subastaId}`);
    }

    finalizarSubasta(id: number) {
        return this.http.put<void>(`${this.subastaUrl}/final/${id}`, {});
    }

    crearPujaSorteo(puja: PujaSorteoCreateDTO) {
        return this.http.post<PujaSorteoResponseDTO>(this.pujaSorteoUrl, puja);
    }

    finalizarSorteo(id: number) {
        return this.http.put<SorteoResponseDto>(`${this.sorteoUrl}/final/${id}`, {});
    }
}
