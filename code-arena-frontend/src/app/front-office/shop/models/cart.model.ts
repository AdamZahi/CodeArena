import { Product } from './product.model';

// ─── Cart Item Interface ──────────────────────────────────────────
// One item in the cart — a product + how many the participant wants
// NOT saved in DB — lives in Angular memory only
export interface CartItem {
  product:  Product;  // the full product object
  quantity: number;   // how many the participant added
}