import { Routes } from '@angular/router';
import { FoShellComponent } from './fo-shell.component';

export const FO_ROUTES: Routes = [
  {
    path: '',
    component: FoShellComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./home/home.component').then((m) => m.HomeComponent)
      },
      {
        path: 'challenge',
        loadChildren: () => import('./challenge/challenge.routes').then((m) => m.CHALLENGE_ROUTES)
      },
      {
        path: 'battle',
        loadChildren: () => import('./battle/battle.routes').then((m) => m.BATTLE_ROUTES)
      },
      {
        path: 'reward-profile',
        loadChildren: () => import('./reward-profile/reward-profile.routes').then((m) => m.REWARD_PROFILE_ROUTES)
      },
      {
        path: 'shop',
        loadChildren: () => import('./shop/shop.routes').then((m) => m.SHOP_ROUTES)
      },
      {
        path: 'support',
        loadChildren: () => import('./support/support.routes').then((m) => m.SUPPORT_ROUTES)
      },
      {
        path: 'event',
        loadChildren: () => import('./event/event.routes').then((m) => m.EVENT_ROUTES)
      },
      {
        path: 'coaching-quiz',
        loadChildren: () => import('./coaching-quiz/coaching.routes').then((m) => m.COACHING_ROUTES)
      },
      {
        path: 'profile',
        loadComponent: () => import('./reward-profile/pages/my-profile/my-profile.component').then((m) => m.MyProfileComponent)
      }
    ]
  }
];
