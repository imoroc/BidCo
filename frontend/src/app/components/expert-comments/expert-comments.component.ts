import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, Output, EventEmitter, inject } from '@angular/core';


import { AuthExpertService } from '../../services/auth.expert.service';
import { ExpertService } from '../../services/expert.service';
import { SesionStore } from '../../services/sesion.store';

@Component({
  selector: 'app-expert-comments',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './expert-comments.component.html',
  styleUrls: ['./expert-comments.component.css']
})
export class ExpertCommentsComponent {
    private readonly sesion = inject(SesionStore);
    @Input() comment:any;
    expert: any;
    expertLogged: any;
    isExpert = false;
    @Output() editClicked = new EventEmitter<any>();

    constructor(private expertAuthService: AuthExpertService, private expertService: ExpertService) {}

    ngOnInit() {
        //Comprobamos si un experto ha iniciado sesion
        this.expertAuthService.isLoggedIn$.subscribe((estado) => {
            this.isExpert = estado;
            const storedUser = this.sesion.perfilExperto();
            if (storedUser) {
                this.expertLogged = JSON.parse(storedUser);
            }
        });

        this.expertService.getExpertoById(this.comment.trabajadorID).subscribe({
            next: (data) => {
                console.log(data)
                this.expert = data
            }
        })
    }

    onEdit() {
        this.editClicked.emit(this.comment);
    }
}
