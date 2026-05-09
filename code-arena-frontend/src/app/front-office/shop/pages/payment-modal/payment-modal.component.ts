import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { loadStripe, Stripe, StripeElements, StripeCardElement } from '@stripe/stripe-js';
import { ShopService } from '../../services/shop.service';
import { Component, OnInit, OnDestroy, AfterViewInit, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-payment-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payment-modal.component.html',
  styleUrl: './payment-modal.component.css'
})
export class PaymentModalComponent implements OnInit, AfterViewInit, OnDestroy {

  // ── INPUTS ────────────────────────────────────
  // Total amount to charge (in dollars) — passed from cart component
  @Input() amount: number = 0;
  // Number of items in cart — shown in modal header
  @Input() itemCount: number = 0;

  // ── OUTPUTS ───────────────────────────────────
  // Fired when payment succeeds — parent (cart) creates the order in DB
  @Output() paymentSuccess = new EventEmitter<string>();
  // Fired when user clicks X — parent hides this modal
  @Output() closeModal = new EventEmitter<void>();

  // ── STRIPE OBJECTS ────────────────────────────
  // stripe: main Stripe instance used to confirm payments
  // elements: factory for creating UI components like card input
  // cardElement: the actual card input field rendered by Stripe
  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private cardElement: StripeCardElement | null = null;

  // ── STATE FLAGS ───────────────────────────────
  isLoading = true;          // true while Stripe.js is initializing
  isProcessing = false;      // true while payment request is in progress
  isSuccess = false;         // true after payment is confirmed by Stripe
  errorMessage = '';         // validation or payment error shown to user
  cardholderName = '';       // value of the cardholder name input
  isCardComplete = false;    // true when Stripe card element is fully filled

  // ── CARD FLIP ANIMATION ───────────────────────
  // When true, the card preview flips to show the back (CVV side)
  isCardFlipped = false;

  // ── INTERNAL FLAG ─────────────────────────────
  // Tracks whether Stripe card element has been created and is ready to mount
  // We need this because mounting happens after the DOM is ready (AfterViewInit)
  private stripeReady = false;

  constructor(private shopService: ShopService) {}

  // ── LIFECYCLE: ON INIT ────────────────────────
  // Called when component is created
  // We start Stripe initialization here (fetches key from backend)
  async ngOnInit(): Promise<void> {
    await this.initializeStripe();
  }

  // ── LIFECYCLE: AFTER VIEW INIT ────────────────
  // Called AFTER Angular has rendered the template to the DOM
  // This is the safe place to mount Stripe card element
  // because #stripe-card-element div is guaranteed to exist here
  ngAfterViewInit(): void {
    // If Stripe finished loading before the view was ready, mount now
    if (this.stripeReady) {
      this.mountCard();
    }
    // Otherwise mountCard() will be called at the end of initializeStripe()
  }

  // ── INITIALIZE STRIPE ─────────────────────────
  // Step 1: Get publishable key from our backend (/api/shop/payment/config)
  // Step 2: Load Stripe.js library with that key
  // Step 3: Create Stripe Elements with our dark gamer theme
  // Step 4: Create card element (but don't mount yet — wait for DOM)
  async initializeStripe(): Promise<void> {
    try {
      // Step 1: Fetch publishable key from backend
      // We never expose secret key to frontend — only publishable key
const configRes = await this.shopService.getPaymentConfig().toPromise();
const publishableKey = configRes!.data.publishableKey;

      // Step 2: Load Stripe.js — this loads Stripe's secure JS library
      this.stripe = await loadStripe(publishableKey);
      if (!this.stripe) throw new Error('Stripe failed to load');

      // Step 3: Create Elements instance with our custom dark theme
      // This matches our gamer aesthetic (purple, dark bg, cyan accents)
      this.elements = this.stripe.elements({
        appearance: {
          theme: 'night',
          variables: {
            colorPrimary: '#8b5cf6',       // purple
            colorBackground: '#0d0d15',    // dark background
            colorText: '#e2e8f0',          // light text
            colorDanger: '#ef4444',        // red for errors
            fontFamily: 'Rajdhani, sans-serif',
            borderRadius: '6px',
          }
        }
      });

      // Step 4: Create the card input element
      // This creates a secure iframe managed by Stripe
      // Card numbers never touch our code — Stripe handles them directly
      this.cardElement = this.elements.create('card', {
        style: {
          base: {
            color: '#e2e8f0',
            fontFamily: 'Rajdhani, sans-serif',
            fontSize: '16px',
            '::placeholder': { color: '#334155' },
          },
          invalid: { color: '#ef4444' }
        }
      });

      // Mark Stripe as ready
      this.stripeReady = true;
      this.isLoading = false;

      // Try to mount — if DOM is ready it mounts immediately
      // If not (ngAfterViewInit hasn't fired yet), mounting happens there
      this.mountCard();

    } catch (error) {
      console.error('Stripe init error:', error);
      this.errorMessage = 'Payment system failed to load. Please try again.';
      this.isLoading = false;
    }
  }

  // ── MOUNT CARD ELEMENT ────────────────────────
  // Mounts the Stripe card input into the #stripe-card-element div
  // Uses a retry mechanism in case the DOM isn't ready yet
  // This is the fix for: "The selector #stripe-card-element applies to no DOM elements"
  private mountCard(): void {
    const el = document.querySelector('#stripe-card-element');

    if (!el) {
      // DOM element not ready yet — retry after 200ms
      // This handles the race condition between Stripe loading and Angular rendering
      setTimeout(() => this.mountCard(), 200);
      return;
    }

    // DOM is ready — mount the Stripe card element
    this.cardElement?.mount('#stripe-card-element');

    // ── LISTEN FOR CARD CHANGES ───────────────────
    // Stripe fires this event whenever the user types in the card field
    // We use it to: track if card is complete + show inline errors
    this.cardElement?.on('change', (event) => {
      this.isCardComplete = event.complete;
      this.errorMessage = event.error ? event.error.message : '';
    });

    // ── LISTEN FOR FOCUS ──────────────────────────
    // Could be used to flip the card animation when CVV is focused
    // Currently placeholder — can be extended to detect CVV focus
    this.cardElement?.on('focus', () => {});
  }

  // ── PROCESS PAYMENT ───────────────────────────
  // Full payment flow:
  // 1. Validate form inputs (name + card complete)
  // 2. Call backend to create a PaymentIntent (gets clientSecret)
  // 3. Use Stripe to confirm the payment with the card details
  // 4. On success: emit event so cart can create the order in DB
  // 5. On failure: show error message to user
  async pay(): Promise<void> {

    // ── VALIDATION ────────────────────────────────
    if (!this.cardholderName.trim()) {
      this.errorMessage = 'Please enter the cardholder name.';
      return;
    }
    if (!this.isCardComplete) {
      this.errorMessage = 'Please complete your card details.';
      return;
    }
    if (!this.stripe || !this.cardElement) {
      this.errorMessage = 'Payment system not ready. Please refresh.';
      return;
    }

    this.isProcessing = true;
    this.errorMessage = '';

    try {
      // Step 1: Create payment intent on backend
      // Backend calls Stripe API with the amount and returns a clientSecret
      // clientSecret is a one-time token that authorizes this specific payment
const intentRes = await this.shopService.createPaymentIntent(this.amount).toPromise();
const clientSecret = intentRes!.data.clientSecret;

      // Step 2: Confirm payment with Stripe
      // This sends the card details directly to Stripe (never to our backend)
      // Stripe validates the card and charges it
      const result = await this.stripe.confirmCardPayment(clientSecret, {
        payment_method: {
          card: this.cardElement,
          billing_details: {
            name: this.cardholderName
          }
        }
      });

      if (result.error) {
        // ── PAYMENT FAILED ────────────────────────
        // Could be: invalid card, insufficient funds, expired card, etc.
        this.errorMessage = result.error.message || 'Payment failed. Please try again.';
        this.isProcessing = false;

      } else if (result.paymentIntent.status === 'succeeded') {
        // ── PAYMENT SUCCEEDED ─────────────────────
        // Show success animation for 2 seconds then notify parent
        this.isSuccess = true;
        this.isProcessing = false;

        // Wait 2s for success animation to play, then emit to parent
        // Parent (cart.component) will then create the order in our DB
        setTimeout(() => {
          this.paymentSuccess.emit(result.paymentIntent.id);
        }, 2000);
      }

    } catch (error) {
      console.error('Payment error:', error);
      this.errorMessage = 'An unexpected error occurred. Please try again.';
      this.isProcessing = false;
    }
  }

  // ── CLOSE MODAL ───────────────────────────────
  // Only allows closing if payment is not in progress
  // Prevents accidental modal close during payment processing
  close(): void {
    if (!this.isProcessing) {
      this.closeModal.emit();
    }
  }

  // ── LIFECYCLE: ON DESTROY ─────────────────────
  // Called when component is removed from DOM
  // We must destroy the Stripe card element to prevent memory leaks
  ngOnDestroy(): void {
    this.cardElement?.destroy();
  }
}