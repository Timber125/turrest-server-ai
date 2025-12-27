import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'lobby',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'lobby',
    loadComponent: () => import('./features/lobby/lobby-list/lobby-list.component').then(m => m.LobbyListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'lobby/room',
    loadComponent: () => import('./features/lobby/lobby-room/lobby-room.component').then(m => m.LobbyRoomComponent),
    canActivate: [authGuard]
  },
  {
    path: 'game',
    loadComponent: () => import('./features/game/game.component').then(m => m.GameComponent),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: 'lobby'
  }
];
