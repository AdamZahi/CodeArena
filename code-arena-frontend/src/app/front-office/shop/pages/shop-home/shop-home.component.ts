import { Component, OnInit }       from '@angular/core';
import { CommonModule }              from '@angular/common';
import { FormsModule }               from '@angular/forms';
import { Router }                    from '@angular/router';
import { ShopService }               from '../../services/shop.service';
import { CartService }               from '../../services/cart.service';
import { Product, ProductCategory }  from '../../models/product.model';
import { WishlistService } from '../../services/wishlist.service';
import { SoundService } from '../../services/sound.service';
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
    private router: Router
  ) {}

  // Runs when the page loads
  ngOnInit(): void {
    this.loadProducts();

    // Subscribe to cart changes to update badge
    this.cartService.cartItems$.subscribe(() => {
      this.cartCount = this.cartService.getItemCount();
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
}