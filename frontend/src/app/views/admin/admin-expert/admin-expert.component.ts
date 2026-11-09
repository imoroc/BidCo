// src/app/admin/admin-expert.component.ts
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { AdminService, ExpertDTO } from '../../../services/admin.service';

@Component({
  selector: 'app-admin-expert',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-expert.component.html',
  styleUrls: ['./admin-expert.component.css']
})
export class AdminExpertComponent {
  // Inicializamos con las mismas propiedades que ExpertDTO
  expert: ExpertDTO = {
    nombreUsuario:     '',
    correoElectronico: '',
    contrasena:        '',
    experto:          true
  };
  formularioInvalido = false;
  message: string = '';

  constructor(private adminSvc: AdminService) {}

  submit(form:any): void {
    this.formularioInvalido = false;

    if (!form.valid) {
        this.formularioInvalido = true;
        return;
    }
    // Llamada al servicio para registrar un experto
    this.adminSvc.createExpert(this.expert).subscribe({
      next: () => {
        this.message = 'Experto registrado correctamente';
        // Opcional: limpiar el formulario
        this.expert = { nombreUsuario: '', correoElectronico: '', contrasena: '', experto: true };
      },
      //error: (err: Error) => {
      //  this.message = 'Error: ' + err.message;
      //}
    });
  }
}