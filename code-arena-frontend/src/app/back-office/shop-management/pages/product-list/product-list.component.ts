import { Component, OnInit }      from '@angular/core';
import { CommonModule }             from '@angular/common';
import { Router }                   from '@angular/router';
import { ShopService }              from '../../../../front-office/shop/services/shop.service';
import { Product }                  from '../../../../front-office/shop/models/product.model';
import { environment } from '../../../../../environments/environment';
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit {

  products: Product[] = [];
  isLoading = true;
  deleteConfirmId: string | null = null;

  constructor(
    private shopService: ShopService,
    public router: Router  // make public so HTML can access

  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

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
      this.totalPages = res.data.totalPages;
      this.totalItems = res.data.totalItems;
      this.pages = Array.from({ length: this.totalPages }, (_, i) => i);
      this.isLoading = false;
    },
    error: () => { this.isLoading = false; }
  });
}


  // Navigate to create form
  createProduct(): void {
    this.router.navigate(['/admin/shop/new']);
  }

  // Navigate to edit form
  editProduct(id: string): void {
    this.router.navigate(['/admin/shop/edit', id]);
  }

  // Show delete confirmation
  confirmDelete(id: string): void {
    this.deleteConfirmId = id;
  }

  // Cancel delete
  cancelDelete(): void {
    this.deleteConfirmId = null;
  }

  // Execute delete
  deleteProduct(id: string): void {
    this.shopService.deleteProduct(id).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== id);
        this.deleteConfirmId = null;
      },
      error: (err) => {
        console.error('Delete failed', err);
        // Remove from local list anyway (mock mode)
        this.products = this.products.filter(p => p.id !== id);
        this.deleteConfirmId = null;
      }
    });
  }

  // Stock status helper
  getStockStatus(stock: number): string {
    if (stock === 0) return 'out';
    if (stock <= 10) return 'low';
    return 'ok';
  }
  exportProducts(): void {
  window.open(`${environment.apiBaseUrl}/api/shop/export/products`, '_blank');
}


// ── ADD THESE PROPERTIES to the class ────────────────────────────
currentPage = 0;
pageSize = 8;
totalPages = 0;
totalItems = 0;
pages: number[] = [];

// ── PAGINATION METHODS ────────────────────────────────────────────
goToPage(page: number): void {
  if (page < 0 || page >= this.totalPages) return;
  this.currentPage = page;
  this.loadProducts();
}

nextPage(): void { this.goToPage(this.currentPage + 1); }
prevPage(): void { this.goToPage(this.currentPage - 1); }
}