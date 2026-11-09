import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SorteoResponseDto {
    sorteoID: number;
    nombreArticulo: string;
    descripcion: string;
    fechaInicio: string;
    fechaFin: string;
    puntosNecesarios: number;
    creadorId: number; 
}

export type { Filtro } from './auction.service';

@Injectable({
    providedIn: 'root'
})
export class SorteoService {
    private apiUrl = `${environment.apiUrl}/sorteos`;

    constructor(private http: HttpClient) {}

    getSorteosPorFiltro(filtro: HttpParams): Observable<SorteoResponseDto[]> {
        return this.http.get<SorteoResponseDto[]>(`${this.apiUrl}/filtro`, {params: filtro});
    }
}