import { Component, OnInit }           from '@angular/core';
import { CommonModule }                  from '@angular/common';
import { FormsModule }                   from '@angular/forms';
import { ActivatedRoute, Router }        from '@angular/router';
import { ShopService }                   from '../../../../front-office/shop/services/shop.service';
import { Product, ProductCategory }      from '../../../../front-office/shop/models/product.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent implements OnInit {

  isEditMode = false;
  editId: string | null = null;
  isLoading = false;
  isSaving = false;
  saveSuccess = false;

  categories = Object.values(ProductCategory);

  formData: Partial<Product> = {
    name: '',
    description: '',
    price: 0,
    stock: 0,
    imageUrl: '',
    category: ProductCategory.OTHER
  };

  // ── VALIDATION ERRORS ────────────────────────
  errors: { [key: string]: string } = {};

  constructor(
    private shopService: ShopService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.editId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.editId;
    if (this.isEditMode && this.editId) {
      this.loadProduct(this.editId);
    }
  }

  loadProduct(id: string): void {
    this.isLoading = true;
    this.shopService.getProductById(id).subscribe({
      next: (res) => {
        this.formData = { ...res.data };
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  // ── VALIDATE ─────────────────────────────────
  validate(): boolean {
    this.errors = {};

    if (!this.formData.name || this.formData.name.trim() === '') {
      this.errors['name'] = 'Product name is required.';
    } else if (this.formData.name.trim().length < 3) {
      this.errors['name'] = 'Name must be at least 3 characters.';
    }

    if (!this.formData.description || this.formData.description.trim() === '') {
      this.errors['description'] = 'Description is required.';
    } else if (this.formData.description.trim().length < 10) {
      this.errors['description'] = 'Description must be at least 10 characters.';
    }

    if (this.formData.price === undefined || this.formData.price === null) {
      this.errors['price'] = 'Price is required.';
    } else if (this.formData.price <= 0) {
      this.errors['price'] = 'Price must be greater than 0.';
    } else if (this.formData.price > 9999) {
      this.errors['price'] = 'Price cannot exceed $9,999.';
    }

    if (this.formData.stock === undefined || this.formData.stock === null) {
      this.errors['stock'] = 'Stock is required.';
    } else if (this.formData.stock < 0) {
      this.errors['stock'] = 'Stock cannot be negative.';
    }

    if (!this.formData.category) {
      this.errors['category'] = 'Please select a category.';
    }

    if (this.formData.imageUrl && this.formData.imageUrl.trim() !== '') {
      const urlPattern = /^https?:\/\/.+/;
      if (!urlPattern.test(this.formData.imageUrl)) {
        this.errors['imageUrl'] = 'Image URL must start with http:// or https://';
      }
    }

    return Object.keys(this.errors).length === 0;
  }

  // Clear error when user types
  clearError(field: string): void {
    if (this.errors[field]) {
      delete this.errors[field];
    }
  }

  onSubmit(): void {
    if (!this.validate()) return;

    this.isSaving = true;

    if (this.isEditMode && this.editId) {
      this.shopService.updateProduct(this.editId, this.formData).subscribe({
        next: () => this.onSuccess(),
        error: () => this.onSuccess()
      });
    } else {
      this.shopService.createProduct(this.formData).subscribe({
        next: () => this.onSuccess(),
        error: () => this.onSuccess()
      });
    }
  }

  onSuccess(): void {
    this.isSaving = false;
    this.saveSuccess = true;
    setTimeout(() => this.router.navigate(['/admin/shop']), 1500);
  }

  goBack(): void {
    this.router.navigate(['/admin/shop']);
  }

  hasErrors(): boolean {
    return Object.keys(this.errors).length > 0;
  }
}