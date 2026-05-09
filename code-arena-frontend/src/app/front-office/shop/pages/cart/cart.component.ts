import { Component, OnInit }   from '@angular/core';
import { CommonModule }          from '@angular/common';
import { Router }                from '@angular/router';
import { CartService }           from '../../services/cart.service';
import { ShopService }           from '../../services/shop.service';
import { CartItem }              from '../../models/cart.model';
import { CheckoutRequest }       from '../../models/order.model';
import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs/operators';
import confetti                  from 'canvas-confetti';
import { FormsModule } from '@angular/forms';
import { PaymentModalComponent } from '../payment-modal/payment-modal.component';
import { GeminiService } from '../../services/gemini.service';
@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, FormsModule, PaymentModalComponent],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css',
})
export class CartComponent implements OnInit {

  Math = Math;
  cartItems: CartItem[] = [];
  total = 0;
  isCheckingOut = false;
  checkoutSuccess = false;

  // ── VALIDATION ERRORS ────────────────────────
  checkoutError: string = '';

  // ── PAYMENT MODAL ─────────────────────────────
  showPaymentModal = false;

  // ── COUPON ────────────────────────────────────
  couponCode: string = '';
  couponApplied: boolean = false;
  couponDiscount: number = 0;
  couponMessage: string = '';
  couponError: string = '';
  couponLoading: boolean = false;

  // ── LOYALTY POINTS ────────────────────────────
  loyaltyPoints: number = 0;
  loyaltyRedeemableValue: number = 0;
  loyaltyApplied: boolean = false;
  loyaltyDiscount: number = 0;
  loyaltyMessage: string = '';
  participantId: string = '';

  constructor(
    private cartService: CartService,
    private shopService: ShopService,
    private router: Router,
    private auth: AuthService,
    private geminiService: GeminiService
  ) {}

  //adding ai property 
  // ── AI MESSAGE ────────────────────────────────
aiOrderMessage: string = '';
isLoadingAiMessage: boolean = false;
//
 ngOnInit(): void {
  this.cartService.cartItems$.subscribe(items => {
    this.cartItems = items;
    this.total = this.cartService.getTotal();
  });

  // Load loyalty points — backend reads user from JWT automatically
  this.loadLoyaltyPoints();
}


loadLoyaltyPoints(): void {
  this.shopService.getLoyaltyPoints().subscribe({
    next: (res) => {
      this.loyaltyPoints = res.data.points;
      this.loyaltyRedeemableValue = res.data.redeemableValue;
    },
    error: (err) => console.error('Failed to load loyalty points', err)
  });
}

  removeItem(productId: string): void {
    this.cartService.removeFromCart(productId);
    this.checkoutError = '';
  }

  increaseQty(productId: string, currentQty: number): void {
    this.cartService.updateQuantity(productId, currentQty + 1);
  }

  decreaseQty(productId: string, currentQty: number): void {
    this.cartService.updateQuantity(productId, currentQty - 1);
  }

  clearCart(): void {
    this.cartService.clearCart();
    this.checkoutError = '';
  }

  continueShopping(): void {
    this.router.navigate(['/shop']);
  }

  // ── DISCOUNT ALGORITHM ────────────────────────
  // Returns discount rate based on quantity
  getDiscountRate(quantity: number): number {
    if (quantity >= 5) return 0.20;
    if (quantity >= 3) return 0.10;
    if (quantity >= 2) return 0.05;
    return 0;
  }

  // Returns discounted unit price
  getDiscountedPrice(price: number, quantity: number): number {
    return price * (1 - this.getDiscountRate(quantity));
  }

  // Returns discount label string
  getDiscountLabel(quantity: number): string {
    if (quantity >= 5) return '20% OFF';
    if (quantity >= 3) return '10% OFF';
    if (quantity >= 2) return '5% OFF';
    return '';
  }

  // Total with discounts applied
  getDiscountedTotal(): number {
    return this.cartItems.reduce((total, item) => {
      return total + this.getDiscountedPrice(item.product.price, item.quantity) * item.quantity;
    }, 0);
  }

  // Total savings amount
  getTotalSavings(): number {
    return this.cartItems.reduce((savings, item) => {
      return savings + (item.product.price - this.getDiscountedPrice(item.product.price, item.quantity)) * item.quantity;
    }, 0);
  }

  // ── CHECKOUT WITH VALIDATION ──────────────────
  // Opens payment modal instead of directly checking out
  // Order is only created AFTER payment is confirmed by Stripe
  checkout(): void {
    this.checkoutError = '';

    if (this.cartItems.length === 0) {
      this.checkoutError = 'Your cart is empty!';
      return;
    }

    const outOfStock = this.cartItems.find(
      item => item.quantity > item.product.stock
    );
    if (outOfStock) {
      this.checkoutError = `"${outOfStock.product.name}" only has ${outOfStock.product.stock} in stock.`;
      return;
    }

    // ── OPEN PAYMENT MODAL ────────────────────────
    // Validation passed — show Stripe payment form
    this.showPaymentModal = true;
  }

  // ── HANDLE PAYMENT SUCCESS ────────────────────
  // Called by payment modal after Stripe confirms payment
  // Only now do we create the actual order in our DB
  onPaymentSuccess(paymentIntentId: string): void {
    this.showPaymentModal = false;
    this.isCheckingOut = true;

    this.auth.user$.pipe(take(1)).subscribe(user => {
      const participantId = user?.sub;

      if (!participantId) {
        this.checkoutError = 'Not authenticated. Please log in.';
        this.isCheckingOut = false;
        return;
      }

      const request: CheckoutRequest = {
        participantId,
        couponCode: this.couponApplied ? this.couponCode : undefined,
        items: this.cartItems.map(item => ({
          productId: item.product.id,
          quantity: item.quantity
        }))
      };

      this.shopService.checkout(request).subscribe({
        next: () => this.onSuccess(),
        error: (err) => {
          console.error('Order creation error:', err);
          this.checkoutError = 'Payment successful but order creation failed. Contact support.';
          this.isCheckingOut = false;
        }
      });
    });
  }

  // ── CLOSE PAYMENT MODAL ───────────────────────
  // Called when user clicks X on the payment modal
  onPaymentModalClose(): void {
    this.showPaymentModal = false;
  }

  // ── SUCCESS + CONFETTI ───────────────────────
  //ai order message generation is also handled here after successful checkout
onSuccess(): void {
  this.isCheckingOut = false;
  this.checkoutSuccess = true;
  this.checkoutError = '';

  // 🎉 Launch confetti
  this.launchConfetti();

  // ── GENERATE AI MESSAGE ───────────────────────
  // Get product names for the AI prompt
  const itemNames = this.cartItems.map(item => item.product.name);
  const total = this.getFinalTotal();
  this.isLoadingAiMessage = true;

  this.geminiService.generateOrderMessage(itemNames, total).subscribe({
    next: (message) => {
      this.aiOrderMessage = message;
      this.isLoadingAiMessage = false;
    },
    error: () => {
      // If AI fails, show a default message
      this.aiOrderMessage = '⚔ WARRIOR! Your order is confirmed and on its way. May your code compile on the first try! 🚀';
      this.isLoadingAiMessage = false;
    }
  });

  setTimeout(() => {
    this.cartService.clearCart();
    this.router.navigate(['/shop/inventory']);
  }, 20000); // extended to 20s so user can read the AI message
}

  launchConfetti(): void {
    // Center burst
    confetti({
      particleCount: 180,
      spread: 90,
      origin: { y: 0.5 },
      colors: ['#8b5cf6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#ffffff']
    });

    // Left cannon
    setTimeout(() => {
      confetti({
        particleCount: 100,
        angle: 60,
        spread: 60,
        origin: { x: 0, y: 0.6 },
        colors: ['#8b5cf6', '#06b6d4', '#10b981']
      });
    }, 300);

    // Right cannon
    setTimeout(() => {
      confetti({
        particleCount: 100,
        angle: 120,
        spread: 60,
        origin: { x: 1, y: 0.6 },
        colors: ['#8b5cf6', '#06b6d4', '#10b981']
      });
    }, 600);

    // Final shower
    setTimeout(() => {
      confetti({
        particleCount: 60,
        spread: 120,
        origin: { y: 0.3 },
        colors: ['#f59e0b', '#ef4444', '#ffffff']
      });
    }, 1000);
  }

  // ── APPLY COUPON ──────────────────────────────
  applyCoupon(): void {
    if (!this.couponCode.trim()) return;
    this.couponLoading = true;
    this.couponError = '';
    this.couponMessage = '';

    this.shopService.validateCoupon(this.couponCode).subscribe({
      next: (res) => {
        this.couponLoading = false;
        if (res.data.valid) {
          this.couponApplied = true;
          this.couponDiscount = res.data.discountRate;
          this.couponMessage = res.data.message;
        } else {
          this.couponApplied = false;
          this.couponDiscount = 0;
          this.couponError = '❌ ' + res.data.message;
        }
      },
      error: () => {
        this.couponLoading = false;
        this.couponError = '❌ Failed to validate coupon.';
      }
    });
  }

  removeCoupon(): void {
    this.couponCode = '';
    this.couponApplied = false;
    this.couponDiscount = 0;
    this.couponMessage = '';
    this.couponError = '';
  }

  // ── FINAL TOTAL WITH COUPON + LOYALTY ─────────
  getFinalTotal(): number {
    let total = this.getDiscountedTotal();
    if (this.couponApplied) total = total * (1 - this.couponDiscount);
    if (this.loyaltyApplied) total = total - this.loyaltyDiscount;
    return Math.max(0, total);
  }

  getCouponSavings(): number {
    return this.getDiscountedTotal() - this.getFinalTotal();
  }

  // ── REDEEM LOYALTY POINTS ─────────────────────
applyLoyaltyPoints(): void {
  if (!this.loyaltyRedeemableValue || this.loyaltyApplied) return;
  const pointsToRedeem = Math.floor(this.loyaltyPoints / 100) * 100;

  this.shopService.redeemPoints(pointsToRedeem).subscribe({
    // Only send points — participantId removed
    next: (res) => {
      this.loyaltyApplied = true;
      this.loyaltyDiscount = res.data.discount;
      this.loyaltyPoints = res.data.remainingPoints;
      this.loyaltyRedeemableValue = 0;
      this.loyaltyMessage = `✅ ${pointsToRedeem} points redeemed for $${res.data.discount.toFixed(2)} off!`;
    },
    error: (err) => console.error('Redeem failed', err)
  });
}


  removeLoyaltyDiscount(): void {
    this.loyaltyApplied = false;
    this.loyaltyDiscount = 0;
    this.loyaltyMessage = '';
    this.loadLoyaltyPoints();
  }
}