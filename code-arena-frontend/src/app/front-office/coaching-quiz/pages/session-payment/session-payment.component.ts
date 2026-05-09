import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { CoachingService } from '../../services/coaching.service';
import { AlertService } from '../../services/alert.service';
import { FormsModule } from '@angular/forms';
import { CoachingNavbarComponent } from '../../components/coaching-navbar/coaching-navbar.component';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-session-payment',
  standalone: true,
  imports: [CommonModule, FormsModule, CoachingNavbarComponent],
  template: `
    <app-coaching-navbar></app-coaching-navbar>
    <div class="lc-container">
      <div class="payment-wrapper" *ngIf="session">
        <div class="payment-card">
          <div class="card-left">
             <div class="brand">CODE<span class="accent">ARENA</span>_PAY</div>
             <h2 class="glitch-title">NEURAL_<span>LINK_SYNC</span></h2>
             <p class="session-info">ESTABLISHING CONNECTION WITH {{ session.language }} MENTOR...</p>
             
             <div class="amount-calc">
                <div class="row"><span>BASE_FEE</span> <span>25.00 EUR</span></div>
                <div class="row"><span>PLATFORM_TAX</span> <span>4.99 EUR</span></div>
                <div class="row total"><span>TOTAL_UPLINK</span> <span>29.99 EUR</span></div>
             </div>
             
             <div class="secure-badge">
               🔒 ENCRYPTED_TUNNEL_VIA_<b>STRIPE</b>
             </div>
          </div>
          
          <div class="card-right">
             <form (submit)="processPayment($event)">
               <div class="form-group">
                 <label>IDENT_EMAIL</label>
                 <input type="email" placeholder="operator@codearena.net" [(ngModel)]="email" name="email" required>
               </div>
               
               <div class="form-group">
                 <label>CARD_DATA_STREAM</label>
                 <div id="card-element" class="card-input-box"></div>
                 <div class="error-msg" *ngIf="cardError">{{ cardError }}</div>
               </div>
               
               <div class="form-group">
                 <label>OPERATOR_NAME</label>
                 <input type="text" placeholder="J. DOE" [(ngModel)]="cardholderName" name="cardholder" required>
               </div>

               <button type="submit" class="action-neon-btn pay" [disabled]="processing || !stripe">
                 <span *ngIf="!processing">EXECUTE_TRANSACTION</span>
                 <span *ngIf="processing" class="spinner-small"></span>
               </button>
               
               <p class="legal">SECURE_HANDSHAKE: DATA NEVER TOUCHAL LOCAL CACHE. TEST_ENVIRONMENT ACTIVE.</p>
             </form>
          </div>
        </div>
        
        <div class="back-link" (click)="goBack()">[ RETURN_TO_DOJO ]</div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      --neon: #8b5cf6;
      --neon2: #06b6d4;
      --neon3: #10b981;
      --dark: #0a0a0f;
      --card: #0d0d15;
      --border: #1a1a2e;
      --text: #e2e8f0;
      --muted: #64748b;
    }

    @keyframes scan-line { 0% { transform: translateY(-100%); } 100% { transform: translateY(100vh); } }

    .lc-container {
      min-height: 100vh; background: var(--dark); font-family: 'Rajdhani', sans-serif; color: var(--text);
      position: relative; overflow: hidden; padding-bottom: 4rem;
      display: flex; align-items: center; justify-content: center;
    }
    .lc-container::before {
      content: ''; position: fixed; top: 0; left: 0; width: 100%; height: 2px;
      background: linear-gradient(90deg, transparent, var(--neon), transparent);
      animation: scan-line 4s linear infinite; opacity: 0.1; pointer-events: none; z-index: 999;
    }

    .payment-wrapper { max-width: 1000px; width: 100%; animation: fadeIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); padding: 2rem; position: relative; z-index: 10; }
    
    .payment-card { 
      display: flex; background: var(--card); border: 1px solid var(--border); overflow: hidden; 
      clip-path: polygon(30px 0%, 100% 0%, 100% calc(100% - 30px), calc(100% - 30px) 100%, 0% 100%, 0% 30px);
    }
    
    .card-left { flex: 1; padding: 3rem; background: rgba(139, 92, 246, 0.03); border-right: 1px solid var(--border); display: flex; flex-direction: column; }
    .brand { font-family: 'Orbitron', monospace; font-size: 1rem; color: var(--text); margin-bottom: 3rem; letter-spacing: 2px; }
    .brand .accent { color: var(--neon2); }
    
    .glitch-title { font-family: 'Orbitron', sans-serif; font-size: 2rem; margin: 0 0 1rem; letter-spacing: 4px; }
    .glitch-title span { color: var(--neon); text-shadow: 0 0 10px var(--neon); }
    .session-info { color: var(--muted); font-size: 0.9rem; margin-bottom: auto; font-family: 'Orbitron', monospace; letter-spacing: 1px; }
    
    .amount-calc { margin-top: 3rem; display: flex; flex-direction: column; gap: 1rem; border-top: 1px solid var(--border); padding-top: 2.5rem;}
    .row { display: flex; justify-content: space-between; color: var(--muted); font-size: 0.85rem; font-family: 'Orbitron', monospace; }
    .total { color: var(--text); font-size: 1.2rem; font-weight: 700; border-top: 1px solid var(--border); padding-top: 1.5rem; }
    .total span:last-child { color: var(--neon2); text-shadow: 0 0 10px var(--neon2); }
    
    .secure-badge { margin-top: 3rem; font-size: 0.7rem; color: #444; font-family: 'Orbitron', monospace; letter-spacing: 1.5px; }

    .card-right { flex: 1.2; padding: 3rem; background: var(--card); }
    
    form { display: flex; flex-direction: column; gap: 2rem; }
    .form-group label { display: block; font-family: 'Orbitron', monospace; font-size: 9px; color: var(--muted); margin-bottom: 0.8rem; letter-spacing: 2px; }
    input { 
      width: 100%; background: rgba(0,0,0,0.4); border: 1px solid var(--border); padding: 1rem; 
      color: var(--text); font-size: 0.95rem; outline: none; transition: all 0.3s;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }
    input:focus { border-color: var(--neon2); box-shadow: 0 0 15px rgba(6, 182, 212, 0.1); }
    
    .card-input-box { 
      border: 1px solid var(--border); background: rgba(0,0,0,0.4); padding: 1.2rem;
      clip-path: polygon(10px 0%, 100% 0%, calc(100% - 10px) 100%, 0% 100%);
    }
    .error-msg { color: #ef4444; font-size: 11px; margin-top: 0.8rem; font-family: 'Orbitron', monospace; }
    
    .action-neon-btn.pay { 
      background: var(--neon); color: #fff; border: none; padding: 1.2rem; font-weight: 900; 
      cursor: pointer; transition: all 0.3s; font-size: 12px; font-family: 'Orbitron', monospace;
      display: flex; align-items: center; justify-content: center; letter-spacing: 2px;
      clip-path: polygon(15px 0%, 100% 0%, calc(100% - 15px) 100%, 0% 100%);
    }
    .action-neon-btn.pay:hover { background: var(--neon2); box-shadow: 0 0 25px var(--neon2); transform: scale(1.02); }
    .action-neon-btn.pay:disabled { opacity: 0.3; cursor: not-allowed; }
    
    .spinner-small { width: 20px; height: 20px; border: 2px solid rgba(0,0,0,0.1); border-top-color: #fff; border-radius: 50%; animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    
    .legal { font-size: 10px; color: #444; line-height: 1.6; text-align: center; font-family: 'Rajdhani', sans-serif; }
    .back-link { margin-top: 2.5rem; color: var(--muted); font-size: 11px; cursor: pointer; transition: color 0.3s; font-family: 'Orbitron', monospace; letter-spacing: 2px; }
    .back-link:hover { color: var(--neon2); text-shadow: 0 0 10px var(--neon2); }

    @keyframes fadeIn { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }
  `]
})

export class SessionPaymentComponent implements OnInit {
  sessionId: string | null = null;
  session: any = null;
  processing = false;
  email: string = '';
  cardholderName: string = '';

  stripe: any;
  card: any;
  cardError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private coachingService: CoachingService,
    private http: HttpClient,
    private alertService: AlertService
  ) { }

  ngOnInit() {
    this.sessionId = this.route.snapshot.paramMap.get('sessionId');
    this.loadSession();
    setTimeout(() => this.initStripe(), 500);
  }

  loadSession() {
    this.coachingService.getDashboard().subscribe(dash => {
      this.session = dash.upcomingSessions.find(s => s.id === this.sessionId);
    });
  }

  initStripe() {
    // CLÉ PUBLIQUE PLACEHOLDER - À REMPLACER PAR LA VRAIE pk_test_...
    const stripeKey = 'pk_test_51QwnFBQo8eHPYc0v04VP2JyrN9TFqSFAeFs8erzKsOZahQInhF5PdtQewLQnHrsslCFZRgotwE2C3FkTaXFmgrUK00HhB47nqu';

    if (!(window as any).Stripe) {
      console.error('Stripe.js not loaded');
      return;
    }
    this.stripe = (window as any).Stripe(stripeKey);
    const elements = this.stripe.elements();

    const style = {
      base: {
        color: '#ffffff',
        fontFamily: 'Inter, sans-serif',
        fontSmoothing: 'antialiased',
        fontSize: '16px',
        '::placeholder': { color: '#555555' }
      },
      invalid: { color: '#d03c3c', iconColor: '#d03c3c' }
    };

    this.card = elements.create('card', { style });
    this.card.mount('#card-element');
    this.card.on('change', (event: any) => {
      this.cardError = event.error ? event.error.message : null;
    });
  }

  processPayment(event: Event) {
    event.preventDefault();
    this.processing = true;

    // 1. Appel au Backend pour créer le PaymentIntent
    const paymentData = {
      sessionId: this.session.id,
      sessionTitle: this.session.title,
      amount: 29.99,
      currency: 'EUR'
    };

    this.http.post<any>('http://localhost:8080/api/coaching/payments/create-payment-intent', paymentData)
      .subscribe({
        next: (resp) => {
          // 2. Confirmation avec Stripe sur le frontend
          this.stripe.confirmCardPayment(resp.clientSecret, {
            payment_method: {
              card: this.card,
              billing_details: {
                name: this.cardholderName,
                email: this.email
              }
            }
          }).then((result: any) => {
            if (result.error) {
              this.cardError = result.error.message;
              this.processing = false;
            } else if (result.paymentIntent.status === 'succeeded') {
              const msg = 'PAIEMENT RÉEL RÉUSSI ! La transaction apparaîtra dans votre dashboard Stripe.';
              this.alertService.success(msg, 'TRANSACTION_COMPLETE');
              this.router.navigate(['/coaching-quiz/my-training']);
            }
          });
        },
        error: (err) => {
          this.processing = false;
          this.alertService.error('Erreur Backend : Vérifiez votre STRIPE_SECRET_KEY dans application.yml', 'GATEWAY_ERROR');
        }
      });
  }

  goBack() {
    this.router.navigate(['/coaching-quiz/my-training']);
  }
}
