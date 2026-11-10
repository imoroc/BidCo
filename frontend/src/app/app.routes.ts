import { Routes } from '@angular/router';

import { AdminGuard } from './services/admin.guard';
import { userGuard } from './services/user.guard';

// Todas las vistas se cargan bajo demanda para reducir el paquete inicial.
const cargarProducto = () => import('./views/product/product.component').then(m => m.ProductComponent);

export const routes: Routes = [
  { path: '', redirectTo: '/auction', pathMatch: 'full' },
  { path: 'auction', title: 'Subastas · BidCo', data: { descripcion: 'Subastas normales de BidCo: puja por artículos de segunda mano y de colección y sigue la cuenta atrás.' }, loadComponent: () => import('./views/auction/auction.component').then(m => m.AuctionComponent) },
  { path: 'blind-auctions', title: 'Subastas a ciegas · BidCo', data: { descripcion: 'Subastas a ciegas de BidCo: envía tu puja sin ver las demás y gana quien más ofrezca al cierre.' }, loadComponent: () => import('./views/blind-auction/blind-auction.component').then(m => m.BlindAuctionComponent) },
  { path: 'raffle', title: 'Sorteos · BidCo', data: { descripcion: 'Sorteos de BidCo: participa con tus puntos y consigue premios al azar.' }, loadComponent: () => import('./views/raffle/raffle.component').then(m => m.RaffleComponent) },
  { path: 'register', title: 'Crear cuenta · BidCo', data: { noindex: true }, loadComponent: () => import('./views/register/register.component').then(m => m.RegisterComponent) },
  { path: 'login', title: 'Iniciar sesión · BidCo', data: { noindex: true }, loadComponent: () => import('./views/login/login.component').then(m => m.LoginComponent) },
  { path: 'expert', title: 'Acceso de expertos · BidCo', data: { noindex: true }, loadComponent: () => import('./views/expert-login/expert-login.component').then(m => m.ExpertLoginComponent) },
  { path: 'auction/product/:id', title: 'Producto · BidCo', loadComponent: cargarProducto, data: { tipo: 'subasta' } },
  { path: 'blind-auctions/product/:id', title: 'Producto · BidCo', loadComponent: cargarProducto, data: { tipo: 'subasta' } },
  { path: 'raffle/product/:id', title: 'Producto · BidCo', loadComponent: cargarProducto, data: { tipo: 'sorteo' } },
  { path: 'search/product/:id', title: 'Producto · BidCo', loadComponent: cargarProducto, data: { tipo: 'subasta' } },
  { path: 'search', title: 'Búsqueda · BidCo', data: { descripcion: 'Busca subastas y sorteos en BidCo por nombre, categoría y precio.' }, loadComponent: () => import('./views/search/search.component').then(m => m.SearchComponent) },
  { path: 'create-auction', title: 'Crear subasta · BidCo', data: { noindex: true }, canActivate: [userGuard], loadComponent: () => import('./views/create-auction/create-auction.component').then(m => m.CreateAuctionComponent) },
  { path: 'my-bids', title: 'Mis pujas · BidCo', data: { noindex: true }, canActivate: [userGuard], loadComponent: () => import('./views/my-bids/my-bids.component').then(m => m.MyBidsComponent) },
  { path: 'my-auctions', title: 'Mis subastas · BidCo', data: { noindex: true }, canActivate: [userGuard], loadComponent: () => import('./views/my-auctions/my-auctions.component').then(m => m.MyAuctionsComponent) },
  { path: 'forgot-password', title: 'Recuperar contraseña · BidCo', data: { noindex: true }, loadComponent: () => import('./components/forgot-password/forgot-password').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', title: 'Nueva contraseña · BidCo', data: { noindex: true }, loadComponent: () => import('./views/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
  { path: 'admin/login', title: 'Acceso de administración · BidCo', data: { noindex: true }, loadComponent: () => import('./views/admin/admin-login/admin-login.component').then(m => m.AdminLoginComponent) },
  {
    path: 'admin',
    title: 'Administración · BidCo',
    data: { noindex: true },
    canActivate: [AdminGuard],
    loadComponent: () => import('./views/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'sorteo' },
      { path: 'sorteo', loadComponent: () => import('./views/admin/admin-sorteo/admin-sorteo.component').then(m => m.AdminSorteoComponent) },
      { path: 'expertos', loadComponent: () => import('./views/admin/admin-expert/admin-expert.component').then(m => m.AdminExpertComponent) }
    ]
  },
  {
    path: 'profile',
    title: 'Mi perfil · BidCo',
    data: { noindex: true },
    canActivate: [userGuard],
    loadComponent: () => import('./views/profile/profile.component').then(m => m.ProfileComponent),
    children: [
      { path: '', redirectTo: 'mis-datos', pathMatch: 'full' },
      { path: 'mis-datos', loadComponent: () => import('./views/profile/profile-datos/profile-datos.component').then(m => m.ProfileDatosComponent) },
      { path: 'privacidad', loadComponent: () => import('./views/profile/profile-privacidad/profile-privacidad.component').then(m => m.ProfilePrivacidadComponent) },
      { path: 'estadisticas', loadComponent: () => import('./views/profile/profile-estadisticas/profile-estadisticas.component').then(m => m.ProfileEstadisticasComponent) }
    ]
  },
  { path: 'ayuda', title: 'Ayuda · BidCo', data: { descripcion: 'Preguntas frecuentes sobre cómo pujar, crear subastas y participar en sorteos en BidCo.' }, loadComponent: () => import('./views/ayuda/ayuda.component').then(m => m.AyudaComponent) },
  { path: 'contacto', title: 'Contacto · BidCo', data: { descripcion: 'Contacta con el equipo de BidCo para resolver dudas o incidencias.' }, loadComponent: () => import('./views/contacto/contacto.component').then(m => m.ContactoComponent) },
  { path: 'terminos', title: 'Términos de uso · BidCo', data: { descripcion: 'Términos de uso de BidCo: condiciones de las subastas, los sorteos y las cuentas.' }, loadComponent: () => import('./views/terminos/terminos.component').then(m => m.TerminosComponent) },
  { path: 'privacidad', title: 'Política de privacidad · BidCo', data: { descripcion: 'Política de privacidad de BidCo: qué datos tratamos y cómo ejercer tus derechos.' }, loadComponent: () => import('./views/privacidad/privacidad.component').then(m => m.PrivacidadComponent) },
  { path: '**', title: 'Página no encontrada · BidCo', data: { noindex: true }, loadComponent: () => import('./views/no-encontrada/no-encontrada.component').then(m => m.NoEncontradaComponent) }
];
