import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { WishlistService } from '../../services/wishlist.service';
import { CartService } from '../../services/cart.service';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './wishlist.component.html',
  styleUrl: './wishlist.component.css'
})
export class WishlistComponent implements OnInit {

  items: Product[] = [];

  constructor(
    private wishlistService: WishlistService,
    private cartService: CartService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.wishlistService.items$.subscribe(items => {
      this.items = items;
    });
  }

  removeFromWishlist(product: Product): void {
    this.wishlistService.toggle(product);
  }

  addToCart(product: Product): void {
    if (product.stock === 0) return;
    this.cartService.addToCart(product, 1);
    this.router.navigate(['/shop/cart']);
  }

  goToShop(): void {
    this.router.navigate(['/shop']);
  }

  clearWishlist(): void {
    this.wishlistService.clear();
  }
}