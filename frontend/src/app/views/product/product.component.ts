import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, inject, viewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { HeaderComponent } from '../../components/header/header.component';
import { ProductDetailsComponent } from '../../components/product-details/product-details.component';
import { ExpertCommentsComponent } from '../../components/expert-comments/expert-comments.component';
import { FooterComponent } from "../../components/footer/footer.component";

import { AuthService } from '../../services/auth.service';
import { ProductoService, PujaCreateDTO, PujaResponseDTO, PujaSorteoCreateDTO } from '../../services/product.service';
import { CommentService } from '../../services/comment.service';
import { AuthExpertService } from '../../services/auth.expert.service';
import { ahoraMs, cierreMs, hoyISO } from '../../utils/fechas';
import { SesionStore } from '../../services/sesion.store';

@Component({
  selector: 'app-product',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe, RouterLink, FormsModule, HeaderComponent, ProductDetailsComponent, ExpertCommentsComponent, FooterComponent],
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.css']
})
export class ProductComponent implements OnDestroy {
    private readonly sesion = inject(SesionStore);
    product: any;
    productId = 0;
    tipo = '';
    isBlindAuction = false;
    isRaffle = false;
    countdown = '';
    // Texto para la región aria-live: solo cambia con sucesos relevantes (fin del plazo, nuevo precio).
    anuncio = '';
    private countdownInterval: ReturnType<typeof setInterval> | null = null;

    user: any;
    isLoggedIn = false;
    cantidadPuja: number | null = null;
    mensaje: { tipo: 'error' | 'exito'; texto: string } | null = null;
    errorFormulario = '';
    // Diálogo nativo: el navegador atrapa el foco, cierra con Escape y devuelve el foco al abrirlo.
    private readonly modalExperto = viewChild<ElementRef<HTMLDialogElement>>('modalExperto');
    candidatoGanadorPuja: string | null = null;
    ganadorSorteo: string | null = null;

    expert: any;
    isExpert = false;
    comments: any[] = [];
    isExpertComment = false;
    puedeComentar = true;
    editandoComentario = false;
    comentarioAEditar: any = null;
    estimatedPrice = 0;
    reviewText = '';
    comentarioEditado = { comment: '', estimated_price: 0 };

    constructor(
        private route: ActivatedRoute,
        private authService: AuthService,
        private expertAuthService: AuthExpertService,
        private productoService: ProductoService,
        private commentService: CommentService
    ) {}

    ngOnInit() {
        this.authService.isLoggedIn$.subscribe((estado) => {
            this.isLoggedIn = estado;
            const storedUser = this.sesion.perfilUsuario();
            if (storedUser) this.user = JSON.parse(storedUser);
        });

        if (!this.isLoggedIn) {
            this.expertAuthService.isLoggedIn$.subscribe((estado) => {
                this.isLoggedIn = estado;
                this.isExpert = estado;
                const storedUser = this.sesion.perfilExperto();
                if (storedUser) this.expert = JSON.parse(storedUser);
            });
        }

        this.productId = Number(this.route.snapshot.paramMap.get('id'));
        this.commentService.getComments(this.productId).subscribe((comments) => {
            this.comments = comments;
            if (this.expert) {
                comments.forEach((comment) => {
                    if (comment.trabajadorID === this.expert.trabajadorID) this.puedeComentar = false;
                });
            }
        });

        this.tipo = this.route.snapshot.data['tipo'];
        switch (this.tipo) {
            case 'subasta':
                this.productoService.getSubastaPorId(this.productId).subscribe((product) => {
                    const precioAnterior: number | undefined = this.product?.precioFinal;
                    this.product = product;
                    this.isBlindAuction = !product.subastaNormal;
                    this.setupCountdown(this.product.fechaFinal);
                    this.anunciarCambioDePrecio(precioAnterior, product.precioFinal);
                });
                break;
            case 'sorteo':
                this.productoService.getSorteoPorId(this.productId).subscribe((product) => {
                    this.product = product;
                    this.isRaffle = true;
                    this.setupCountdown(this.product.fechaFin);
                });
                break;
            default:
                console.error('Tipo de producto no válido');
        }

        this.productoService.obtenerPujaMaximaPorSubasta(this.productId).subscribe({
            next: (puja) => {
                if (!puja?.pujadorID) return;
                this.productoService.getPerfilPublicoPorId(puja.pujadorID).subscribe((profile) => {
                    this.candidatoGanadorPuja = profile.privacidadAnonimoPujas ? 'Anónimo' : profile.nombreUsuario;
                });
            },
            // Sin pujas, sorteo o subasta a ciegas en curso: no hay candidato que mostrar.
            error: () => {},
        });
    }

    private mostrarMensaje(tipo: 'error' | 'exito', texto: string) {
        this.mensaje = { tipo, texto };
    }

    private textoDeError(err: any, porDefecto: string): string {
        const texto = err?.error?.message;
        return typeof texto === 'string' && texto.trim() ? texto : porDefecto;
    }

    anunciar(texto: string) {
        this.anuncio = texto;
    }

    // Avisa del nuevo precio solo si ya había uno cargado y ha cambiado (no en la primera carga).
    anunciarCambioDePrecio(anterior: number | undefined, actual: number) {
        if (this.isBlindAuction || anterior === undefined || anterior === actual) return;
        const importe = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(actual);
        this.anunciar(`El precio actual es ${importe}.`);
    }

    limpiarMensaje() {
        this.mensaje = null;
    }

    onSubmit() {
        this.mensaje = null;
        if (this.user.usuarioID == this.product.creadorId) {
            this.mostrarMensaje('error', 'No puedes pujar en una subasta que has creado.');
            return;
        }
        if (this.cantidadPuja == null) {
            this.mostrarMensaje('error', 'Debes ingresar una cantidad.');
            return;
        }

        if (this.isRaffle && this.product.puntosNecesarios > this.cantidadPuja) {
            this.mostrarMensaje('error', 'La cantidad dada debe ser mayor a la mínima para participar.');
            return;
        } else if (this.isBlindAuction && this.product.precioInicial >= this.cantidadPuja) {
            this.mostrarMensaje('error', 'La puja tiene que ser mayor que el precio base del producto.');
            return;
        } else if (!this.isBlindAuction && !this.isRaffle && this.product.precioFinal >= this.cantidadPuja) {
            this.mostrarMensaje('error', 'La puja tiene que ser mayor que el precio actual del producto.');
            return;
        } else if (this.isRaffle && this.cantidadPuja > this.user.puntos) {
            this.mostrarMensaje('error', 'No tienes suficientes puntos para participar.');
            return;
        } else if (!this.isRaffle && this.cantidadPuja > this.user.saldo) {
            this.mostrarMensaje('error', 'Saldo insuficiente.');
            return;
        }

        if (this.isRaffle) {
            const nuevaPujaSorteo: PujaSorteoCreateDTO = {
                puntos: this.cantidadPuja,
                fecha: hoyISO(),
                sorteoId: this.productId,
                pujadorId: this.user.usuarioID
            };
            this.productoService.crearPujaSorteo(nuevaPujaSorteo).subscribe({
                next: () => {
                    this.authService.loadUserProfile(this.user.usuarioID).subscribe({
                        next: (usuarioActualizado) => {
                            this.user = usuarioActualizado;
                            this.sesion.guardarPerfilUsuario(usuarioActualizado);
                        },
                        error: (err) => console.error('Error al actualizar el usuario:', err)
                    });
                    this.ngOnInit();
                    this.mostrarMensaje('exito', 'Has entrado en el sorteo correctamente.');
                },
                error: (err) => this.mostrarMensaje('error', this.textoDeError(err, 'No se ha podido realizar la puja. Inténtalo de nuevo.'))
            });
        } else {
            const nuevaPuja: PujaCreateDTO = {
                importe: this.cantidadPuja,
                fecha: hoyISO(),
                subastaId: this.productId,
                pujadorId: this.user.usuarioID
            };
            this.productoService.crearPuja(nuevaPuja).subscribe({
                next: () => {
                    this.ngOnInit();
                    this.mostrarMensaje('exito', 'Tu puja se ha registrado correctamente.');
                },
                error: (err) => this.mostrarMensaje('error', this.textoDeError(err, 'No se ha podido realizar la puja. Inténtalo de nuevo.'))
            });
        }
    }

    setupCountdown(endDateString: string) {
        // El servidor solo permite finalizarlo a partir de su hora de cierre,
        // así que se espera unos segundos de margen.
        const endDate = cierreMs(endDateString);

        this.detenerCuentaAtras();
        const interval = setInterval(() => {
            const now = ahoraMs();
            const distance = endDate - now;
            if (distance <= 0) {
                this.detenerCuentaAtras();
                this.alLlegarACero();
                return;
            }
            const days = Math.floor(distance / (1000 * 60 * 60 * 24));
            const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((distance % (1000 * 60)) / 1000);
            this.countdown = `${days}d ${hours}h ${minutes}m ${seconds}s`;
        }, 1000);
        this.countdownInterval = interval;
    }

    /**
     * Una puja de última hora prolonga el cierre de una subasta (anti-sniping) y esta ficha puede
     * tener el plazo desactualizado: antes de darla por terminada se consulta el plazo vigente.
     */
    private alLlegarACero() {
        if (this.tipo !== 'subasta') {
            this.cerrarCuentaAtras();
            return;
        }
        this.productoService.getSubastaPorId(this.productId).subscribe({
            next: (subasta) => {
                this.product = subasta;
                if (cierreMs(subasta.fechaFinal) > ahoraMs()) {
                    this.anunciar('Se ha prolongado el plazo porque entró una puja en los últimos minutos.');
                    this.setupCountdown(subasta.fechaFinal);
                } else {
                    this.cerrarCuentaAtras();
                }
            },
            error: () => this.cerrarCuentaAtras()
        });
    }

    private cerrarCuentaAtras() {
        const estabaEnCurso = this.countdown !== '' && this.countdown !== 'Finalizada';
        this.countdown = "Finalizada";
        if (estabaEnCurso) this.anunciar(this.isRaffle ? 'El sorteo ha finalizado.' : 'La subasta ha finalizado.');
        if (!this.product.ganador) {
            switch (this.tipo) {
                case 'subasta':
                    this.productoService.finalizarSubasta(this.productId).subscribe({
                        next: () => this.authService.loadUserProfile(this.user.usuarioID).subscribe({
                            next: (userActualizado) => this.authService.setUser(userActualizado)
                        })
                    });
                    break;
                case 'sorteo':
                    this.productoService.finalizarSorteo(this.productId).subscribe((sorteoActualizado) => {
                        this.product = sorteoActualizado;
                        this.setGanadorSorteo();
                    });
                    break;
            }
        }
        if (this.product.ganador != null && this.tipo === 'sorteo') this.setGanadorSorteo();
    }

    ngOnDestroy() {
        // Sin esto, al salir de la ficha el intervalo seguiría vivo (y podría finalizar la subasta equivocada).
        this.detenerCuentaAtras();
    }

    private detenerCuentaAtras() {
        if (this.countdownInterval !== null) {
            clearInterval(this.countdownInterval);
            this.countdownInterval = null;
        }
    }

    setGanadorSorteo() {
        this.productoService.getPerfilPublicoPorId(this.product.ganador).subscribe((profile) => {
            this.ganadorSorteo = profile.privacidadAnonimoPujas ? 'Anónimo' : profile.nombreUsuario;
        });
    }

    openExpertForm() {
        this.modalExperto()?.nativeElement.showModal();
    }

    editExpertForm(comment: any) {
        this.editandoComentario = true;
        this.comentarioAEditar = comment;
        this.comentarioEditado = { comment: comment.comentario, estimated_price: comment.precioEstimado };
        this.modalExperto()?.nativeElement.showModal();
    }

    closeExpertForm() {
        this.modalExperto()?.nativeElement.close();
    }

    // Se ejecuta al cerrarse el diálogo (botones, Escape o código): deja el formulario limpio.
    alCerrarModal() {
        this.errorFormulario = '';
        this.editandoComentario = false;
        this.comentarioEditado = { comment: '', estimated_price: 0 };
    }

    handleFormSubmit() {
        this.errorFormulario = '';
        if (!this.comentarioEditado.comment || this.comentarioEditado.estimated_price <= 0) {
            this.errorFormulario = 'Debes completar todos los campos requeridos.';
            return;
        }
        if (this.editandoComentario) this.saveEdit();
        else this.submitReview();
    }

    submitReview() {
        const productId = Number(this.route.snapshot.paramMap.get('id'));
        const nuevoComentario = {
            comentario: this.comentarioEditado.comment,
            precioEstimado: this.comentarioEditado.estimated_price,
            subastaID: productId,
            trabajadorId: this.expert.trabajadorID
        };

        this.commentService.createComment(nuevoComentario).subscribe({
            next: (res) => {
                this.comments.push(res);
                this.closeExpertForm();
            },
            error: (err) => {
                console.error('Error al crear comentario:', err);
                this.errorFormulario = this.textoDeError(err, 'No se ha podido publicar la valoración. Inténtalo de nuevo.');
            }
        });
    }

    saveEdit(): void {
        if (!this.editandoComentario) return;
        const productId = Number(this.route.snapshot.paramMap.get('id'));
        const updatedComentario = {
            comentario: this.comentarioEditado.comment,
            precioEstimado: this.comentarioEditado.estimated_price,
            subastaID: productId,
            trabajadorId: this.expert.trabajadorID
        };

        this.commentService.editComment(this.comentarioAEditar.comentarioID, updatedComentario).subscribe({
            next: () => {
                this.closeExpertForm();
                this.ngOnInit();
            },
            error: (err) => {
                console.error('Error al editar comentario:', err);
                this.errorFormulario = this.textoDeError(err, 'No se han podido guardar los cambios. Inténtalo de nuevo.');
            }
        });
    }

    cancelEdit(): void { this.closeExpertForm(); }
}
