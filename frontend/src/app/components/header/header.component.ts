import { ChangeDetectionStrategy, Component, HostListener, ElementRef } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';

import { FormsModule } from '@angular/forms';

import { AuthService } from '../../services/auth.service';
import { AuthExpertService } from '../../services/auth.expert.service';

@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    FormsModule
],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  isDropdownOpen = false;
  isUserMenuOpen = false;
  isExamplesDropdownOpen = false;
  isLoggedIn = false; // Control local del estado de login
  isExpert = false;
  searchTerm: string = '';

  constructor(
    private elRef: ElementRef,
    private authService: AuthService,
    private expertAuthService: AuthExpertService,
    private router: Router
  ) {}

  /** Salta la cabecera y deja el foco en el contenido principal (sin cambiar la URL). */
  saltarAlContenido(event: Event): void {
    event.preventDefault();
    const principal = document.querySelector<HTMLElement>('main');
    if (principal) {
      principal.setAttribute('tabindex', '-1');
      principal.focus();
      principal.scrollIntoView();
    }
  }

  ngOnInit() {
    this.authService.isLoggedIn$.subscribe((estado: boolean) => {
      this.isLoggedIn = estado;
    });

    //Si no hay sesion de usuario se comprueba si hay sesion de experto
    if(!this.isLoggedIn) {
        this.expertAuthService.isLoggedIn$.subscribe((estado) => {
            this.isLoggedIn = estado;
            this.isExpert = estado;
        });
    }
  }

  toggleDropdown(event: MouseEvent) {
    event.stopPropagation();
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  toggleUserMenu(event: MouseEvent) {
    event.stopPropagation();
    this.isUserMenuOpen = !this.isUserMenuOpen;
  }

  toggleDropdownExamples(event: MouseEvent) {
    event.stopPropagation();
    this.isExamplesDropdownOpen = !this.isExamplesDropdownOpen;
  }

  /** Escape cierra el menú abierto y devuelve el foco a su botón. */
  @HostListener('document:keydown.escape')
  onEscape() {
    const abierto = this.elRef.nativeElement.querySelector('[aria-expanded="true"]') as HTMLElement | null;
    this.isDropdownOpen = false;
    this.isUserMenuOpen = false;
    this.isExamplesDropdownOpen = false;
    abierto?.focus();
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent) {
    // Cierra ambos menús si se hace clic fuera
    if (!this.elRef.nativeElement.contains(event.target)) {
      this.isDropdownOpen = false;
      this.isUserMenuOpen = false;
      this.isExamplesDropdownOpen = false;
    }
  }
  
  logout() {
    this.authService.logout();
    this.expertAuthService.logout();
    this.router.navigateByUrl('/auction');
  }

  loginUser() {
    this.authService.setUserRole('user');
  }

  loginExpert() {
    this.authService.setUserRole('expert');
  }

    onSearch() {
        if (this.searchTerm && this.searchTerm.trim() !== '') {
            this.router.navigate(['/search'], { queryParams: { search: this.searchTerm.trim() } })
            this.searchTerm = '';
        }
    }
}