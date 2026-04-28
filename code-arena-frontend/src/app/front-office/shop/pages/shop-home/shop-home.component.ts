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
import { RecommendationService } from '../../services/recommendation.service';

@Component({
  selector: 'app-shop-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './shop-home.component.html',
  styleUrl: './shop-home.component.css'
})
export class ShopHomeComponent implements OnInit {

  // All products from the service (current page)
  products: Product[] = [];

  // ALL products across all pages — used for recommendations
  allProducts: Product[] = [];

  // Filtered products shown on screen
  filteredProducts: Product[] = [];

  // AI recommendations
  recommendations: Product[] = [];

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

  // Toasts and animation state for cart/wishlist actions
  cartToast: string = '';
  wishlistToast: string = '';
  animatingProductId: string = '';

  // ── DYNAMIC PRICING ───────────────────────────
  dynamicPrices: { [productId: string]: PriceUpdate } = {};

  // ── ECO SCORES ────────────────────────────────
  ecoScores: { [productId: string]: EcoScore } = {};
  ecoTooltipId: string = '';

  constructor(
    private shopService: ShopService,
    private cartService: CartService,
    public wishlistService: WishlistService,
    private soundService: SoundService,
    private notificationService: NotificationService,
    private auth: AuthService,
    private ecoScoreService: EcoScoreService,
    private recommendationService: RecommendationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProducts();

    this.cartService.cartItems$.subscribe(() => {
      this.cartCount = this.cartService.getItemCount();
    });

    this.auth.user$.pipe(take(1)).subscribe(user => {
      if (user?.sub) {
        this.notificationService.connect(user.sub);
      }
    });

    this.notificationService.priceUpdates$.subscribe(updates => {
      if (!updates) return;
      updates.forEach(update => {
        this.dynamicPrices[update.productId] = update;
      });
    });

    // ── LOYALTY MILESTONE TOAST ───────────────────
this.notificationService.milestone$.subscribe(milestone => {
  if (!milestone) return;
  this.milestoneToast = milestone.message;
  this.milestoneCode = milestone.couponCode;
  setTimeout(() => {
    this.milestoneToast = '';
    this.milestoneCode = '';
  }, 10000); // show for 10 seconds
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

        // ── ECO SCORES: batch load for current page ───
        this.ecoScoreService.loadAllScores(
          this.products.map(p => ({ id: p.id, name: p.name, category: p.category }))
        ).subscribe(scores => {
          this.ecoScores = { ...this.ecoScores, ...scores };
        });

        // ── LOAD ALL PRODUCTS ONCE for recommendations ─
        // Only fetch all products on first page load
        if (this.allProducts.length === 0) {
          this.shopService.getAllProducts().subscribe({
            next: (allRes) => {
              this.allProducts = allRes.data || [];
              this.loadRecommendations();
            },
            error: () => {
              // Fallback to current page if getAllProducts fails
              this.allProducts = [...this.products];
              this.loadRecommendations();
            }
          });
        }
      },
      error: (err) => {
        console.error('Failed to load products', err);
        this.isLoading = false;
      }
    });
  }

  // ── FILTERS ───────────────────────────────────────────────────────

  filterByCategory(category: string): void {
    this.selectedCategory = category;
    this.applyFilters();
  }

  onSearch(): void {
    this.applyFilters();
  }

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

  isLowStock(stock: number): boolean {
    return stock > 0 && stock <= 10;
  }

  // ── CART ──────────────────────────────────────────────────────────

  addToCart(product: Product): void {
    if (product.stock === 0) return;
    this.cartService.addToCart(product, 1);
    this.soundService.playCartPop();
    this.animatingProductId = product.id;
    setTimeout(() => this.animatingProductId = '', 600);
    this.cartToast = `✅ ${product.name} added to cart!`;
    setTimeout(() => this.cartToast = '', 3000);
  }

  goToCart(): void {
    this.router.navigate(['/shop/cart']);
  }

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

  // ── WISHLIST ──────────────────────────────────────────────────────

  toggleWishlist(product: Product): void {
    this.wishlistService.toggle(product);
    const isNowWishlisted = this.wishlistService.isWishlisted(product.id);
    if (isNowWishlisted) {
      this.soundService.playWishlistPop();
      this.wishlistToast = `❤️ ${product.name} added to wishlist!`;
      setTimeout(() => this.wishlistToast = '', 3000);
    }
    // Reload recommendations when wishlist changes
    this.loadRecommendations();
  }

  goToWishlist(): void {
    this.router.navigate(['/shop/wishlist']);
  }

  goToOrders(): void {
    this.router.navigate(['/shop/inventory']);
  }

  // ── DYNAMIC PRICING ───────────────────────────────────────────────

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

  // ── ECO SCORE ─────────────────────────────────────────────────────

  toggleEcoTooltip(productId: string): void {
    this.ecoTooltipId = this.ecoTooltipId === productId ? '' : productId;
  }

  // ── AI RECOMMENDATIONS ────────────────────────────────────────────
  // Combines order history + wishlist signals
  // Order history: quantity × 1 weight
  // Wishlist: quantity × 3 weight (stronger interest signal)
  // Sends all products to Flask AI model for scoring
  loadRecommendations(): void {
    this.auth.user$.pipe(take(1)).subscribe(user => {
      if (!user?.sub) return;

      this.shopService.getMyOrders().subscribe({
        next: (res) => {
          // Signal 1: Order history (weaker signal — already bought)
          const orderItems = (res.data || []).flatMap((order: any) =>
            (order.items || []).map((item: any) => ({
              productId: item.productId || item.product?.id,
              productName: item.productName || item.product?.name,
              category: item.category || item.product?.category,
              quantity: item.quantity
            }))
          );

          // Signal 2: Wishlist (stronger signal — user wants but hasn't bought)
          const wishlistSignals = this.wishlistService.getItems().map((p: Product) => ({
            productId: p.id,
            productName: p.name,
            category: p.category,
            quantity: 3  // 3x weight vs order history
          }));

          // Combine both signals
          const combined = [...orderItems, ...wishlistSignals];

          // Use allProducts so recommendations aren't limited to current page
          const productsToSearch = this.allProducts.length > 0
            ? this.allProducts
            : this.products;

          this.recommendationService.getRecommendations(
            user.sub!,
            combined,
            productsToSearch
          ).subscribe(recs => {
            this.recommendations = recs
              .map(r => productsToSearch.find(p => p.id === r.id))
              .filter((p): p is Product => !!p);
          });
        },
        error: () => {}
      });
    });
  }

  //properties for milestone notification
  milestoneToast: string = '';
milestoneCode: string = '';
}