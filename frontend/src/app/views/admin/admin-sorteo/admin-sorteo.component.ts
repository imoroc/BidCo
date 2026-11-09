import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AdminService, SorteoDTO } from '../../../services/admin.service';

import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-sorteo',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule],
  standalone: true,
  templateUrl: './admin-sorteo.component.html',
  styleUrl: './admin-sorteo.component.css'
})
export class AdminSorteoComponent {
  sorteo: Omit<SorteoDTO, 'imagen'> = {
    nombreArticulo: '',
    descripcion: '',
    fechaInicio: '',
    fechaFin: '',
    puntosNecesarios: 0,
    categoria: ''
  };

  selectedFile: File | null = null;
  message = '';

  constructor(private admin: AdminService) {}

  onFile(evt: Event) {
    const f = (evt.target as HTMLInputElement).files?.[0];
    this.selectedFile = f ?? null;
  }

  submit() {
    this.admin.createSorteo(this.sorteo, this.selectedFile!).subscribe({
      next: () => this.message = 'Sorteo creado',
      error: e => this.message = 'Error: ' + e.message
    });
  }
}