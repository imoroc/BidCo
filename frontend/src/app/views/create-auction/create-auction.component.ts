import { ChangeDetectionStrategy, Component, ElementRef, ViewChild, inject } from '@angular/core';
import { HeaderComponent } from '../../components/header/header.component';
import { FooterComponent } from "../../components/footer/footer.component";
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';


import { CreateSubastaService, SubastaCreateDTO } from '../../services/create-auction.service';
import { AuthService } from '../../services/auth.service';
import { estaVencida, hoyISO } from '../../utils/fechas';
import { SesionStore } from '../../services/sesion.store';

const TIPOS_IMAGEN_PERMITIDOS = ['image/jpeg', 'image/png', 'image/webp'];
const TAMANO_MAXIMO_IMAGEN = 8 * 1024 * 1024;

@Component({
  selector: 'app-create-auction',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [
    HeaderComponent,
    FooterComponent,
    FormsModule
],
  templateUrl: './create-auction.component.html',
  styleUrls: ['./create-auction.component.css']
})
export class CreateAuctionComponent {
    private readonly sesion = inject(SesionStore);
    @ViewChild('resumenErrores') resumenErrores?: ElementRef<HTMLElement>;

    imagen: File | null = null;
    imagenPreview: string | ArrayBuffer | null = null;
    subasta: SubastaCreateDTO = {
        fechaInicial: hoyISO(),
        fechaFinal: '',
        precioInicial: 0,
        subastaNormal: true,
        nombreArticulo: '',
        descripcion: '',
        creadorId: 1,
        categoria: ''
    };
    errores: string[] = [];
    errorImagen = '';
    enviando = false;
    horaFinal = '';
    fechaMinima = hoyISO();

    constructor(private crearSubastaService: CreateSubastaService, private router: Router, private authService: AuthService) {}

    onSubmit(form: any) {
        if (this.enviando) {
            return;
        }

        this.errores = this.validar(form);
        if (this.errores.length > 0) {
            this.mostrarErrores();
            return;
        }

        const storedUser = this.sesion.perfilUsuario();
        if (!storedUser) {
            this.errores = ['Debes iniciar sesión para publicar una subasta.'];
            this.mostrarErrores();
            return;
        }

        const user = JSON.parse(storedUser);
        this.subasta.creadorId = user.usuarioID;

        const formData = new FormData();
        const datos = { ...this.subasta, fechaFinal: this.cierreElegido() };
        formData.append('subasta', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
        if (this.imagen) {
            formData.append('imagen', this.imagen);
        }

        this.enviando = true;
        this.crearSubastaService.crearSubasta(formData).subscribe({
            next: () => {
                this.authService.loadUserProfile(user.usuarioID).subscribe({
                    next: (userActualizado) => this.authService.setUser(userActualizado)
                });
                this.router.navigate(['/']);
            },
            error: (err) => {
                this.enviando = false;
                this.errores = [this.mensajeDeError(err)];
                this.mostrarErrores();
            }
        });
    }

    onImgSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.errorImagen = '';

        if (!input.files || input.files.length === 0) {
            return;
        }

        const archivo = input.files[0];

        if (!TIPOS_IMAGEN_PERMITIDOS.includes(archivo.type)) {
            this.rechazarImagen(input, 'La imagen debe ser JPG, PNG o WEBP.');
            return;
        }

        if (archivo.size > TAMANO_MAXIMO_IMAGEN) {
            this.rechazarImagen(input, 'La imagen no puede superar los 8 MB.');
            return;
        }

        this.imagen = archivo;

        const reader = new FileReader();
        reader.onload = () => {
            this.imagenPreview = reader.result;
        };
        reader.readAsDataURL(archivo);
    }

    private validar(form: any): string[] {
        const errores: string[] = [];

        if (!this.subasta.nombreArticulo?.trim()) {
            errores.push('Escribe el nombre del producto.');
        }
        if (this.subasta.precioInicial === null || this.subasta.precioInicial === undefined || this.subasta.precioInicial < 0) {
            errores.push('El precio base debe ser un número igual o mayor que 0.');
        }
        if (!this.subasta.descripcion?.trim()) {
            errores.push('Escribe la descripción del artículo.');
        }
        if (!this.subasta.categoria) {
            errores.push('Selecciona una categoría.');
        }
        if (!this.subasta.fechaFinal) {
            errores.push('Indica la fecha de finalización.');
        } else if (estaVencida(this.cierreElegido())) {
            errores.push('La fecha y la hora de finalización deben ser posteriores a este momento.');
        } else if (!this.horaFinal && this.subasta.fechaFinal <= this.fechaMinima) {
            errores.push('Sin hora de cierre, la fecha de finalización debe ser posterior a hoy.');
        }
        if (this.imagenPreview === null) {
            errores.push(this.errorImagen || 'Añade una imagen del artículo.');
        }

        return errores;
    }

    private rechazarImagen(input: HTMLInputElement, mensaje: string): void {
        input.value = '';
        this.imagen = null;
        this.imagenPreview = null;
        this.errorImagen = mensaje;
    }

    private mensajeDeError(err: any): string {
        if (err?.status === 0) {
            return 'No se ha podido conectar con el servidor. Inténtalo de nuevo en unos minutos.';
        }
        if (err?.status === 413) {
            return 'La imagen es demasiado grande.';
        }
        if (err?.status === 403) {
            return 'No tienes permiso para publicar subastas con esta cuenta.';
        }
        const detalle = typeof err?.error === 'string' ? err.error : err?.error?.message;
        if (err?.status === 400 && detalle) {
            return detalle;
        }
        return 'Ha ocurrido un error al publicar la subasta. Inténtalo de nuevo.';
    }

    private mostrarErrores(): void {
        setTimeout(() => this.resumenErrores?.nativeElement.focus());
    }

    /** Día elegido más la hora de cierre, si se indicó; sin hora cierra a la medianoche de ese día. */
    private cierreElegido(): string {
        return this.horaFinal ? `${this.subasta.fechaFinal}T${this.horaFinal}` : this.subasta.fechaFinal;
    }
}
