import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FooterComponent } from '../../../components/footer/footer.component';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
  standalone: true,
  imports: [FooterComponent, RouterLink, RouterLinkActive, RouterOutlet],
})
export class AdminDashboardComponent {
  constructor(private router: Router, private auth: AuthService) {}

  cerrarSesion() {
    this.auth.logout();
    this.router.navigateByUrl('/');
  }
}
