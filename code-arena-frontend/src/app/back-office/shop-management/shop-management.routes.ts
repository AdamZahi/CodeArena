import { Routes } from '@angular/router';

export const SHOP_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/product-list/product-list.component')
      .then(m => m.ProductListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./pages/product-form/product-form.component')
      .then(m => m.ProductFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./pages/product-form/product-form.component')
      .then(m => m.ProductFormComponent)
  },
  {
    path: 'orders',
    loadComponent: () => import('./pages/order-list/order-list.component')
      .then(m => m.OrderListComponent)
  },
  {
  path: 'dashboard',
  loadComponent: () => import('./pages/shop-dashboard/shop-dashboard.component')
    .then(m => m.ShopDashboardComponent)
},
  {
    // ── ECO ALERTS: AI-powered sustainability monitoring ──
    // Admin sees products with eco score ≤ 4
    // Linked to SDG 12 — Responsible Consumption
    path: 'eco-alerts',
    loadComponent: () => import('./pages/eco-alerts/eco-alerts.component')
      .then(m => m.EcoAlertsComponent)
  }

];