import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SubastaResponseDTO {
    subastaID: number;
    nombreArticulo: string;
    precioFinal: string;
    categoria: string;
    subastaNormal: boolean;
}

export interface Filtro {
    minPrice: number, 
    maxPrice: number, 
    categories: string[], 
    dateOrder: string 
}

@Injectable({
    providedIn: 'root'
})
export class SubastaService {
    private subastasUrl = `${environment.apiUrl}/subastas`;
    private pujasSubastasUrl = `${environment.apiUrl}/pujas`;

    constructor(private http: HttpClient) {}

    getSubastasPorCreador(id: number): Observable<SubastaResponseDTO[]> {
        return this.http.get<SubastaResponseDTO[]>(`${this.subastasUrl}/filtrar-creador`, {params: { id: id }});
    }   

    getSubastasPujasUsuaroId(id: number): Observable<SubastaResponseDTO[]> {
        return this.http.get<SubastaResponseDTO[]>(`${this.pujasSubastasUrl}/subastas-usuario/${id}`);
    }

    getSubastasFiltradasNormal(filtro: HttpParams): Observable<SubastaResponseDTO[]> {
        return this.http.get<SubastaResponseDTO[]>(`${this.subastasUrl}/filtro-normal`, {params: filtro});
    }

    getSubastasFiltradasCiega(filtro: HttpParams): Observable<SubastaResponseDTO[]> {
        return this.http.get<SubastaResponseDTO[]>(`${this.subastasUrl}/filtro-ciega`, {params: filtro});
    }

    getSubastasFiltradasMisSubastas(filtro: HttpParams, id: number): Observable<SubastaResponseDTO[]> {
        var params = new HttpParams();
        params = filtro.append("id", id);
        return this.http.get<SubastaResponseDTO[]>(`${this.subastasUrl}/filtro-mis-subastas`, {params: params});
    }

    getSubastasFiltradasMisPujas(filtro: HttpParams, id: number): Observable<SubastaResponseDTO[]> {
        var params = new HttpParams();
        params = filtro.append("id", id);
        return this.http.get<SubastaResponseDTO[]>(`${this.subastasUrl}/filtro-mis-pujas`, {params: params});
    }

    getSubastasFiltradasPorNombre(filtro: HttpParams, searchTerm: string): Observable<SubastaResponseDTO[]> {
        var params = new HttpParams();
        params = filtro.append("searchTerm", searchTerm);
        return this.http.get<SubastaResponseDTO[]>(`${this.subastasUrl}/filtro-nombre`, {params: params});
    }
}
