import { Routes } from '@angular/router';

export const BATTLE_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/battle-lobby/battle-lobby.component').then((m) => m.BattleLobbyComponent) },
  { path: 'lobby/:roomId', loadComponent: () => import('./pages/battle-lobby/battle-lobby.component').then((m) => m.BattleLobbyComponent) },
  { path: 'room/:roomId', loadComponent: () => import('./pages/battle-room/battle-room.component').then((m) => m.BattleRoomComponent) },
  { path: 'result/:roomId', loadComponent: () => import('./pages/battle-result/battle-result.component').then((m) => m.BattleResultComponent) }
];
