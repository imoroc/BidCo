// admin-dashboard.component.ts
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FooterComponent } from "../../../components/footer/footer.component";
import { AuthService }  from '../../../services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
  standalone: true,
  imports: [FooterComponent, RouterModule],
})
export class AdminDashboardComponent {
  constructor(private router: Router, private auth: AuthService) {}

  goToSorteo() {
    this.router.navigateByUrl('/admin/sorteo');
  }
  goToExpertos() {
    this.router.navigateByUrl('/admin/expertos');
  }
  cerrarSesion() {
    this.auth.logout();
    this.router.navigateByUrl('/');
  }
}
