import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class WishlistService {

  private readonly STORAGE_KEY = 'codearena_wishlist';

  private wishlist$ = new BehaviorSubject<Product[]>(this.loadFromStorage());

  items$ = this.wishlist$.asObservable();

  // ── ADD / REMOVE ─────────────────────────────
  toggle(product: Product): void {
    const current = this.wishlist$.getValue();
    const exists = current.find(p => p.id === product.id);

    if (exists) {
      const updated = current.filter(p => p.id !== product.id);
      this.wishlist$.next(updated);
      this.saveToStorage(updated);
    } else {
      const updated = [...current, product];
      this.wishlist$.next(updated);
      this.saveToStorage(updated);
    }
  }

  // ── CHECK IF WISHLISTED ──────────────────────
  isWishlisted(productId: string): boolean {
    return this.wishlist$.getValue().some(p => p.id === productId);
  }

  // ── GET COUNT ────────────────────────────────
  getCount(): number {
    return this.wishlist$.getValue().length;
  }

  // ── CLEAR ────────────────────────────────────
  clear(): void {
    this.wishlist$.next([]);
    localStorage.removeItem(this.STORAGE_KEY);
  }

  // ── STORAGE ──────────────────────────────────
  private saveToStorage(products: Product[]): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(products));
  }

  private loadFromStorage(): Product[] {
    try {
      const data = localStorage.getItem(this.STORAGE_KEY);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  }
  // ── GET ALL ITEMS ─────────────────────────────
getItems(): Product[] {
  return this.wishlist$.getValue();
}
}
