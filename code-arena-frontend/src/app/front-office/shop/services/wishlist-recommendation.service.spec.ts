import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WishlistService } from './wishlist.service';
import { RecommendationService } from './recommendation.service';
import { Product } from '../models/product.model';

// ─────────────────────────────────────────────────────────────────────────────
// WISHLIST SERVICE TESTS
// ─────────────────────────────────────────────────────────────────────────────

describe('WishlistService', () => {
  let service: WishlistService;

  const mockProduct: Product = {
    id: 'prod-1',
    name: 'CodeArena Hoodie',
    description: 'Premium hoodie',
    price: 39.99,
    stock: 50,
    imageUrl: 'https://example.com/hoodie.jpg',
    category: 'HOODIE' as any
  };

  const mockProduct2: Product = {
    id: 'prod-2',
    name: 'CodeArena Mousepad',
    description: 'XL mousepad',
    price: 24.99,
    stock: 100,
    imageUrl: 'https://example.com/mousepad.jpg',
    category: 'MOUSEPAD' as any
  };

  beforeEach(() => {
    localStorage.clear(); // fresh wishlist each test
    TestBed.configureTestingModule({
      providers: [WishlistService]
    });
    service = TestBed.inject(WishlistService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── toggle ────────────────────────────────────

  it('toggle — adds product to wishlist when not present', () => {
    service.toggle(mockProduct);
    expect(service.isWishlisted('prod-1')).toBeTrue();
  });

  it('toggle — removes product from wishlist when already present', () => {
    service.toggle(mockProduct);
    service.toggle(mockProduct);
    expect(service.isWishlisted('prod-1')).toBeFalse();
  });

  it('toggle — can hold multiple products', () => {
    service.toggle(mockProduct);
    service.toggle(mockProduct2);
    expect(service.getCount()).toBe(2);
  });

  // ── isWishlisted ──────────────────────────────

  it('isWishlisted — returns false for product not in wishlist', () => {
    expect(service.isWishlisted('non-existent-id')).toBeFalse();
  });

  it('isWishlisted — returns true for product in wishlist', () => {
    service.toggle(mockProduct);
    expect(service.isWishlisted(mockProduct.id)).toBeTrue();
  });

  // ── getCount ──────────────────────────────────

  it('getCount — returns 0 for empty wishlist', () => {
    expect(service.getCount()).toBe(0);
  });

  it('getCount — returns correct count after adding items', () => {
    service.toggle(mockProduct);
    service.toggle(mockProduct2);
    expect(service.getCount()).toBe(2);
  });

  it('getCount — decreases after removing item', () => {
    service.toggle(mockProduct);
    service.toggle(mockProduct2);
    service.toggle(mockProduct); // remove
    expect(service.getCount()).toBe(1);
  });

  // ── getItems ──────────────────────────────────

  it('getItems — returns empty array for empty wishlist', () => {
    expect(service.getItems()).toEqual([]);
  });

  it('getItems — returns all wishlisted products', () => {
    service.toggle(mockProduct);
    service.toggle(mockProduct2);
    const items = service.getItems();
    expect(items.length).toBe(2);
    expect(items.map(p => p.id)).toContain('prod-1');
    expect(items.map(p => p.id)).toContain('prod-2');
  });

  // ── clear ─────────────────────────────────────

  it('clear — empties the wishlist', () => {
    service.toggle(mockProduct);
    service.toggle(mockProduct2);
    service.clear();
    expect(service.getCount()).toBe(0);
    expect(service.getItems()).toEqual([]);
  });

  // ── persistence ───────────────────────────────

  it('persists wishlist to localStorage', () => {
    service.toggle(mockProduct);
    const stored = localStorage.getItem('codearena_wishlist');
    expect(stored).toBeTruthy();
    const parsed = JSON.parse(stored!);
    expect(parsed[0].id).toBe('prod-1');
  });
});


// ─────────────────────────────────────────────────────────────────────────────
// RECOMMENDATION SERVICE TESTS
// ─────────────────────────────────────────────────────────────────────────────

describe('RecommendationService', () => {
  let service: RecommendationService;
  let httpMock: HttpTestingController;

const AI_URL = 'http://localhost:8080/api/shop';

  const mockProducts: Product[] = [
    { id: 'p1', name: 'CodeArena Hoodie', description: '', price: 39.99, stock: 50, imageUrl: '', category: 'HOODIE' as any },
    { id: 'p2', name: 'CodeArena Mousepad', description: '', price: 24.99, stock: 100, imageUrl: '', category: 'MOUSEPAD' as any },
    { id: 'p3', name: 'Dark Mode Mug', description: '', price: 17.99, stock: 30, imageUrl: '', category: 'MUG' as any },
  ];

  const mockOrders = [
    { productId: 'p1', productName: 'CodeArena Hoodie', category: 'HOODIE', quantity: 2 }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RecommendationService]
    });
    service = TestBed.inject(RecommendationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ── getRecommendations ────────────────────────

  it('getRecommendations — calls Flask API with correct body', () => {
    const participantId = 'google-oauth2|123';

    service.getRecommendations(participantId, mockOrders, mockProducts).subscribe();

    const req = httpMock.expectOne(`${AI_URL}/recommendations`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.participantId).toBe(participantId);
    expect(req.request.body.userOrders.length).toBe(1);
    expect(req.request.body.allProducts.length).toBe(3);
    req.flush({ success: true, recommendations: [] });
  });

  it('getRecommendations — returns recommendations from Flask', () => {
    const mockResponse = {
      success: true,
      recommendations: [
        { id: 'p2', score: 0.85, reason: 'Based on your HOODIE purchases' },
        { id: 'p3', score: 0.60, reason: 'Based on your HOODIE purchases' }
      ]
    };

    service.getRecommendations('user-123', mockOrders, mockProducts).subscribe(recs => {
      expect(recs.length).toBe(2);
      expect(recs[0].id).toBe('p2');
      expect(recs[1].id).toBe('p3');
    });

    const req = httpMock.expectOne(`${AI_URL}/recommendations`);
    req.flush(mockResponse);
  });

  it('getRecommendations — returns empty array when Flask is down', () => {
    service.getRecommendations('user-123', mockOrders, mockProducts).subscribe(recs => {
      expect(recs).toEqual([]);
    });

    const req = httpMock.expectOne(`${AI_URL}/recommendations`);
    req.error(new ErrorEvent('Network error'));
  });

  it('getRecommendations — sends mapped product fields to Flask', () => {
    service.getRecommendations('user-123', mockOrders, mockProducts).subscribe();

    const req = httpMock.expectOne(`${AI_URL}/recommendations`);
    const sentProduct = req.request.body.allProducts[0];

    // Verify only necessary fields are sent (not full Angular Product object)
    expect(sentProduct.id).toBeDefined();
    expect(sentProduct.name).toBeDefined();
    expect(sentProduct.category).toBeDefined();
    expect(sentProduct.price).toBeDefined();
    expect(sentProduct.stock).toBeDefined();

    req.flush({ success: true, recommendations: [] });
  });

  it('getRecommendations — respects custom limit parameter', () => {
    service.getRecommendations('user-123', mockOrders, mockProducts, 2).subscribe();

    const req = httpMock.expectOne(`${AI_URL}/recommendations`);
    expect(req.request.body.limit).toBe(2);
    req.flush({ success: true, recommendations: [] });
  });
});
