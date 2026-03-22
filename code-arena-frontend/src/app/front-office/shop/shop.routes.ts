import { Routes } from '@angular/router';

export const SHOP_ROUTES: Routes = [
  { 
    path: '', 
    loadComponent: () => import('./pages/shop-home/shop-home.component')
      .then((m) => m.ShopHomeComponent) 
  },
  { 
    path: 'item/:id', 
    loadComponent: () => import('./pages/item-detail/item-detail.component')
      .then((m) => m.ItemDetailComponent) 
  },
  { 
    path: 'inventory', 
    loadComponent: () => import('./pages/my-inventory/my-inventory.component')
      .then((m) => m.MyInventoryComponent) 
  },
  { 
    path: 'cart', 
    loadComponent: () => import('./pages/cart/cart.component')
      .then((m) => m.CartComponent) 
  }
];