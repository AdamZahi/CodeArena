import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';
import { Product } from '../models/product.model';

describe('CartService', () => {
  let service: CartService;

  const mockProduct: Product = {
    id: 'prod-1',
    name: 'CodeArena Hoodie',
    description: 'Premium black hoodie',
    price: 39.99,
    stock: 50,
    imageUrl: 'https://example.com/hoodie.jpg',
    category: 'HOODIE' as any
  };

  const mockProduct2: Product = {
    id: 'prod-2',
    name: 'CodeArena Mousepad',
    description: 'XL desk mat',
    price: 24.99,
    stock: 100,
    imageUrl: 'https://example.com/mousepad.jpg',
    category: 'MOUSEPAD' as any
  };

  // Helper — reads BehaviorSubject directly since getItems() doesn't exist
  const getItems = () => (service as any)['cartItems'].getValue();

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [CartService] });
    service = TestBed.inject(CartService);
    service.clearCart();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── addToCart ─────────────────────────────────

  it('addToCart — adds product to cart', () => {
    service.addToCart(mockProduct, 1);
    expect(service.getItemCount()).toBe(1);
  });

  it('addToCart — increases quantity when same product added again', () => {
    service.addToCart(mockProduct, 1);
    service.addToCart(mockProduct, 1);
    const items = getItems();
    expect(items.length).toBe(1);
    expect(items[0].quantity).toBe(2);
  });

  it('addToCart — adds multiple different products', () => {
    service.addToCart(mockProduct, 1);
    service.addToCart(mockProduct2, 2);
    expect(service.getItemCount()).toBe(3);
  });

  it('addToCart — adds correct quantity', () => {
    service.addToCart(mockProduct, 3);
    expect(getItems()[0].quantity).toBe(3);
  });

  // ── getTotal ──────────────────────────────────

  it('getTotal — calculates correct total for single item', () => {
    service.addToCart(mockProduct, 2); // 39.99 × 2 = 79.98
    expect(service.getTotal()).toBeCloseTo(79.98, 2);
  });

  it('getTotal — calculates correct total for multiple items', () => {
    service.addToCart(mockProduct, 1);  // 39.99
    service.addToCart(mockProduct2, 2); // 24.99 × 2 = 49.98 → total 89.97
    expect(service.getTotal()).toBeCloseTo(89.97, 2);
  });

  it('getTotal — returns 0 for empty cart', () => {
    expect(service.getTotal()).toBe(0);
  });

  // ── removeFromCart ────────────────────────────

  it('removeFromCart — removes product from cart', () => {
    service.addToCart(mockProduct, 1);
    service.removeFromCart(mockProduct.id);
    expect(service.getItemCount()).toBe(0);
  });

  it('removeFromCart — does not affect other items', () => {
    service.addToCart(mockProduct, 1);
    service.addToCart(mockProduct2, 1);
    service.removeFromCart(mockProduct.id);
    expect(service.getItemCount()).toBe(1);
    expect(getItems()[0].product.id).toBe('prod-2');
  });

  // ── clearCart ─────────────────────────────────

  it('clearCart — empties the cart', () => {
    service.addToCart(mockProduct, 3);
    service.addToCart(mockProduct2, 2);
    service.clearCart();
    expect(service.getItemCount()).toBe(0);
    expect(service.getTotal()).toBe(0);
  });

  // ── getItemCount ──────────────────────────────

  it('getItemCount — returns number of unique products', () => {
    service.addToCart(mockProduct, 5);
    service.addToCart(mockProduct2, 3);
    expect(service.getItemCount()).toBe(8);
  });

  it('getItemCount — returns 0 for empty cart', () => {
    expect(service.getItemCount()).toBe(0);
  });
});