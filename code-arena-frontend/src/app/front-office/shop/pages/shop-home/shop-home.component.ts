import { Component, OnInit }       from '@angular/core';
import { CommonModule }              from '@angular/common';
import { FormsModule }               from '@angular/forms';
import { Router }                    from '@angular/router';
import { ShopService }               from '../../services/shop.service';
import { CartService }               from '../../services/cart.service';
import { Product, ProductCategory }  from '../../models/product.model';
import { WishlistService } from '../../services/wishlist.service';
import { SoundService } from '../../services/sound.service';
import { NotificationService, PriceUpdate } from '../../services/notification.service';
import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs/operators';
import { EcoScoreService, EcoScore } from '../../services/eco-score.service';
@Component({
  selector: 'app-shop-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './shop-home.component.html',
  styleUrl: './shop-home.component.css'
})
export class ShopHomeComponent implements OnInit {

  // All products from the service
  products: Product[] = [];

  // Filtered products shown on screen
  filteredProducts: Product[] = [];

  // Selected category filter (empty = show all)
  selectedCategory: string = '';

  // Search text
  searchText: string = '';

  // Sort option
  sortBy: string = '';

  // All available categories for the filter buttons
  categories = Object.values(ProductCategory);

  // Cart item count for the badge
  cartCount = 0;

  // Loading state
  isLoading = true;

  // Modal
  selectedProduct: Product | null = null;

  // ── PAGINATION ────────────────────────────────────────────────────
  currentPage = 0;
  pageSize = 8;
  totalPages = 0;
  totalItems = 0;
  pages: number[] = [];

  constructor(
    private shopService: ShopService,
    private cartService: CartService,
    public wishlistService: WishlistService,
    private soundService: SoundService,
    private notificationService: NotificationService,
    private auth: AuthService,
    private ecoScoreService: EcoScoreService,
    private router: Router

  ) {}

  // Runs when the page loads
ngOnInit(): void {
  this.loadProducts();

  // Subscribe to cart changes to update badge
  this.cartService.cartItems$.subscribe(() => {
    this.cartCount = this.cartService.getItemCount();
  });

  // ── ENSURE WEBSOCKET IS CONNECTED ────────────
  // Safe to call multiple times — has internal guard
  this.auth.user$.pipe(take(1)).subscribe(user => {
    if (user?.sub) {
      this.notificationService.connect(user.sub);
    }
  });

  // ── SUBSCRIBE TO PRICE UPDATES ────────────────
  this.notificationService.priceUpdates$.subscribe(updates => {
    if (!updates) return;
    updates.forEach(update => {
      this.dynamicPrices[update.productId] = update;
    });
  });
}

  // ── LOAD PRODUCTS (paginated) ─────────────────────────────────────
  loadProducts(): void {
    this.isLoading = true;
    this.shopService.getProductsPaginated(
      this.currentPage,
      this.pageSize,
      'name',
      'asc'
    ).subscribe({
next: (res) => {
  this.products = res.data.products;
  this.filteredProducts = [...this.products];
  this.totalPages = res.data.totalPages;
  this.totalItems = res.data.totalItems;
  this.pages = Array.from({ length: this.totalPages }, (_, i) => i);
  this.isLoading = false;

  // ── LOAD ECO SCORES FOR ALL PRODUCTS ─────────
  // Generate AI eco scores after products load
  // Uses caching so Gemini is only called once per product
 // ── LOAD ECO SCORES WITH DELAY ────────────────
// Space out API calls to avoid Gemini rate limit (15 req/min free tier)
// 5 second delay between each product = safe for free tier
this.products.forEach((product, index) => {
  this.loadEcoScore(product, index * 8000);
});
},
      error: (err) => {
        console.error('Failed to load products', err);
        this.isLoading = false;
      }
    });
  }

  // ── FILTERS ───────────────────────────────────────────────────────

  // Filter by category
  filterByCategory(category: string): void {
    this.selectedCategory = category;
    this.applyFilters();
  }

  // Search by name
  onSearch(): void {
    this.applyFilters();
  }

  // Apply both filters + sort together
  applyFilters(): void {
    let filtered = this.products.filter(product => {
      const matchesCategory = this.selectedCategory
        ? product.category === this.selectedCategory
        : true;
      const matchesSearch = this.searchText
        ? product.name.toLowerCase().includes(this.searchText.toLowerCase())
        : true;
      return matchesCategory && matchesSearch;
    });

    // Apply sort
    if (this.sortBy === 'price-asc') {
      filtered.sort((a, b) => a.price - b.price);
    } else if (this.sortBy === 'price-desc') {
      filtered.sort((a, b) => b.price - a.price);
    } else if (this.sortBy === 'stock-asc') {
      filtered.sort((a, b) => a.stock - b.stock);
    } else if (this.sortBy === 'name-asc') {
      filtered.sort((a, b) => a.name.localeCompare(b.name));
    }

    this.filteredProducts = filtered;
  }

  onSortChange(): void {
    this.applyFilters();
  }

  // Low stock helper
  isLowStock(stock: number): boolean {
    return stock > 0 && stock <= 10;
  }

  // ── CART ──────────────────────────────────────────────────────────

  // Add product to cart
addToCart(product: Product): void {
  if (product.stock === 0) return;
  this.cartService.addToCart(product, 1);

  // ── SOUND + ANIMATION + TOAST ─────────────────
  this.soundService.playCartPop();
  this.animatingProductId = product.id;
  setTimeout(() => this.animatingProductId = '', 600);

  this.cartToast = `✅ ${product.name} added to cart!`;
  setTimeout(() => this.cartToast = '', 3000);
}

  // Go to cart
  goToCart(): void {
    this.router.navigate(['/shop/cart']);
  }

  // Clear category filter
  clearFilter(): void {
    this.selectedCategory = '';
    this.applyFilters();
  }

  // ── MODAL ─────────────────────────────────────────────────────────

  viewProduct(product: Product): void {
    this.openModal(product);
  }

  openModal(product: Product): void {
    this.selectedProduct = product;
  }

  closeModal(): void {
    this.selectedProduct = null;
  }

  // Image error fallback
  onImageError(event: any): void {
    event.target.src = 'https://via.placeholder.com/400x300/0d0d15/8b5cf6?text=CODEARENA';
  }

  // ── PAGINATION ────────────────────────────────────────────────────

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  nextPage(): void { this.goToPage(this.currentPage + 1); }
  prevPage(): void { this.goToPage(this.currentPage - 1); }

  min(a: number, b: number): number {
    return Math.min(a, b);
  }
toggleWishlist(product: Product): void {
  this.wishlistService.toggle(product);
  const isNowWishlisted = this.wishlistService.isWishlisted(product.id);

  // ── SOUND + TOAST ─────────────────────────────
  if (isNowWishlisted) {
    this.soundService.playWishlistPop();
    this.wishlistToast = `❤️ ${product.name} added to wishlist!`;
    setTimeout(() => this.wishlistToast = '', 3000);
  }
}

goToWishlist(): void {
  this.router.navigate(['/shop/wishlist']);
}
// Toasts and animation state for cart/wishlist actions
cartToast: string = '';
wishlistToast: string = '';
animatingProductId: string = '';

goToOrders(): void {
  this.router.navigate(['/shop/inventory']);
}
// ── DYNAMIC PRICING ───────────────────────────
dynamicPrices: { [productId: string]: PriceUpdate } = {};
// ── GET DYNAMIC PRICE ─────────────────────────
getDynamicPrice(product: Product): number {
  const update = this.dynamicPrices[product.id];
  return update ? update.dynamicPrice : product.price;
}

getDynamicIndicator(product: Product): string {
  const update = this.dynamicPrices[product.id];
  return update ? update.indicator : '';
}

isPriceChanged(product: Product): boolean {
  const update = this.dynamicPrices[product.id];
  return update ? update.changed : false;
}
// ── ECO SCORE property───────────────────────────────
// ── ECO SCORES ────────────────────────────────
// Stores eco scores per product ID
// Loaded lazily when products appear on screen
ecoScores: { [productId: string]: EcoScore } = {};
ecoTooltipId: string = ''; // which product's tooltip is showing
// ── LOAD ECO SCORE ────────────────────────────
// Called when product card is visible
// Generates AI eco score for each product
// ── LOAD ECO SCORE ────────────────────────────
// delayMs: how long to wait before calling Gemini
// This prevents hitting the rate limit when loading multiple products
loadEcoScore(product: Product, delayMs: number = 0): void {
  if (this.ecoScores[product.id]) return; // already loaded — use cache

  this.ecoScoreService.getScore(product.id, product.name, product.category, delayMs)
    .subscribe(score => {
      const newScores = { ...this.ecoScores };
      newScores[product.id] = score;
      this.ecoScores = newScores; // triggers Angular change detection
    });
}

// ── TOGGLE ECO TOOLTIP ────────────────────────
toggleEcoTooltip(productId: string): void {
  this.ecoTooltipId = this.ecoTooltipId === productId ? '' : productId;
}
}