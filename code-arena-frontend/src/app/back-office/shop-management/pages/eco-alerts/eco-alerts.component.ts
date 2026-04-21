import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { RouterLink } from '@angular/router';

interface FlaggedProduct {
  id: string;
  name: string;
  category: string;
  ecoScore: number;
  stock: number;
  price: number;
}

@Component({
  selector: 'app-eco-alerts',
  standalone: true,
  imports: [CommonModule,RouterLink],
  template: `
    <div class="eco-alerts-container">

      <!-- HEADER -->
      <div class="eco-header">
        <div class="eco-header-left">
          <span class="eco-icon">🌍</span>
          <div>
            <h2 class="eco-title">ECO SUSTAINABILITY ALERTS</h2>
            <p class="eco-sub">AI-flagged products needing sustainable sourcing review</p>
          </div>
        </div>
<div class="eco-header-right" style="display:flex; gap:8px;">
          <button class="refresh-btn" routerLink="/admin/shop/dashboard">← SHOP</button>
          <button class="refresh-btn" (click)="loadAlerts()">↻ REFRESH</button>
        </div>
      </div>

      <!-- LOADING -->
      <div class="eco-loading" *ngIf="isLoading">Scanning products...</div>

      <!-- ALL GOOD -->
      <div class="eco-all-good" *ngIf="!isLoading && flaggedCount === 0">
        <span class="all-good-icon">✅</span>
        <p>All products meet eco standards!</p>
        <span class="all-good-sub">No products with ECO score ≤ 4</span>
      </div>

      <!-- ALERT SUMMARY -->
      <div class="alert-summary" *ngIf="!isLoading && flaggedCount > 0">
        <div class="summary-card red">
          <span class="summary-number">{{ flaggedCount }}</span>
          <span class="summary-label">FLAGGED PRODUCTS</span>
        </div>
        <div class="summary-card amber">
          <span class="summary-number">ECO ≤ 4</span>
          <span class="summary-label">THRESHOLD</span>
        </div>
        <div class="summary-card green">
          <span class="summary-number">SDG 12</span>
          <span class="summary-label">LINKED GOAL</span>
        </div>
      </div>

      <!-- FLAGGED PRODUCTS LIST -->
      <div class="flagged-list" *ngIf="!isLoading && flaggedCount > 0">
        <div class="flagged-card" *ngFor="let product of flaggedProducts">

          <div class="flagged-left">
            <div class="eco-score-circle"
                 [style.borderColor]="getScoreColor(product.ecoScore)"
                 [style.color]="getScoreColor(product.ecoScore)">
              {{ product.ecoScore }}/10
            </div>
            <div class="flagged-info">
              <span class="flagged-name">{{ product.name }}</span>
              <span class="flagged-cat">{{ product.category }}</span>
            </div>
          </div>

          <div class="flagged-right">
            <span class="flagged-stock">{{ product.stock }} in stock</span>
            <span class="flagged-price">{{ product.price | currency }}</span>
            <div class="suggestion">
              💡 {{ getSuggestion(product.category) }}
            </div>
          </div>

        </div>
      </div>

      <!-- SDG 12 NOTE -->
      <div class="sdg-note" *ngIf="!isLoading">
        <span class="sdg-badge">🌱 SDG 12</span>
        <p>These products have high carbon footprint scores based on our AI model
           trained on real lifecycle carbon data. Consider switching to recycled,
           organic, or biodegradable alternatives to improve your shop's
           sustainability profile.</p>
      </div>

    </div>
  `,
  styles: [`
    .eco-alerts-container {
      padding: 24px;
      font-family: 'Rajdhani', sans-serif;
    }

    .eco-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      padding: 20px;
      background: linear-gradient(135deg, rgba(239,68,68,0.08), rgba(245,158,11,0.05));
      border: 1px solid rgba(239,68,68,0.2);
      border-radius: 12px;
    }

    .eco-header-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .eco-icon { font-size: 36px; }

    .eco-title {
      font-family: 'Orbitron', monospace;
      font-size: 16px;
      font-weight: 900;
      color: #ef4444;
      letter-spacing: 2px;
      margin: 0 0 4px;
    }

    .eco-sub {
      font-size: 12px;
      color: #64748b;
      margin: 0;
    }

    .refresh-btn {
      background: rgba(139,92,246,0.1);
      border: 1px solid rgba(139,92,246,0.3);
      color: #8b5cf6;
      font-family: 'Orbitron', monospace;
      font-size: 11px;
      padding: 8px 16px;
      border-radius: 8px;
      cursor: pointer;
      letter-spacing: 1px;
      transition: all 0.2s;
    }

    .refresh-btn:hover { background: rgba(139,92,246,0.2); }

    .eco-loading {
      text-align: center;
      padding: 40px;
      color: #64748b;
      font-family: 'Orbitron', monospace;
      font-size: 12px;
      letter-spacing: 2px;
    }

    .eco-all-good {
      text-align: center;
      padding: 48px;
      background: rgba(16,185,129,0.05);
      border: 1px solid rgba(16,185,129,0.2);
      border-radius: 12px;
      margin-bottom: 24px;
    }

    .all-good-icon { font-size: 48px; display: block; margin-bottom: 12px; }

    .eco-all-good p {
      font-family: 'Orbitron', monospace;
      color: #10b981;
      font-size: 14px;
      margin: 0 0 8px;
    }

    .all-good-sub { font-size: 12px; color: #64748b; }

    .alert-summary {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 24px;
    }

    .summary-card {
      padding: 20px;
      border-radius: 12px;
      text-align: center;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .summary-card.red   { background: rgba(239,68,68,0.1);  border: 1px solid rgba(239,68,68,0.2); }
    .summary-card.amber { background: rgba(245,158,11,0.1); border: 1px solid rgba(245,158,11,0.2); }
    .summary-card.green { background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.2); }

    .summary-number {
      font-family: 'Orbitron', monospace;
      font-size: 28px;
      font-weight: 900;
      color: #e2e8f0;
    }

    .summary-label {
      font-size: 10px;
      color: #64748b;
      letter-spacing: 2px;
    }

    .flagged-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }

    .flagged-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: rgba(239,68,68,0.05);
      border: 1px solid rgba(239,68,68,0.15);
      border-left: 3px solid #ef4444;
      border-radius: 10px;
      padding: 16px 20px;
      transition: border-color 0.2s;
    }

    .flagged-card:hover { border-color: rgba(239,68,68,0.4); }

    .flagged-left { display: flex; align-items: center; gap: 16px; }

    .eco-score-circle {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      border: 2px solid;
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: 'Orbitron', monospace;
      font-size: 11px;
      font-weight: 700;
      flex-shrink: 0;
    }

    .flagged-name {
      font-weight: 600;
      color: #e2e8f0;
      font-size: 14px;
      display: block;
      margin-bottom: 2px;
    }

    .flagged-cat {
      font-size: 11px;
      color: #64748b;
      letter-spacing: 1px;
    }

    .flagged-right {
      text-align: right;
      display: flex;
      flex-direction: column;
      gap: 4px;
      align-items: flex-end;
    }

    .flagged-stock { font-size: 12px; color: #64748b; }
    .flagged-price { font-family: 'Orbitron', monospace; font-size: 13px; color: #8b5cf6; }

    .suggestion {
      font-size: 11px;
      color: #f59e0b;
      background: rgba(245,158,11,0.1);
      border-radius: 6px;
      padding: 3px 8px;
      max-width: 220px;
      text-align: right;
    }

    .sdg-note {
      background: rgba(16,185,129,0.05);
      border: 1px solid rgba(16,185,129,0.15);
      border-radius: 12px;
      padding: 16px 20px;
      display: flex;
      gap: 16px;
      align-items: flex-start;
    }

    .sdg-badge {
      background: rgba(16,185,129,0.15);
      border: 1px solid rgba(16,185,129,0.3);
      color: #10b981;
      font-family: 'Orbitron', monospace;
      font-size: 10px;
      padding: 4px 10px;
      border-radius: 20px;
      white-space: nowrap;
      letter-spacing: 1px;
    }

    .sdg-note p {
      font-size: 12px;
      color: #64748b;
      margin: 0;
      line-height: 1.6;
    }
  `]
})
export class EcoAlertsComponent implements OnInit {

  flaggedProducts: FlaggedProduct[] = [];
  flaggedCount = 0;
  message = '';
  isLoading = true;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadAlerts();
  }

  loadAlerts(): void {
    this.isLoading = true;
    this.http.get<any>(`${environment.apiBaseUrl}/api/shop/products/eco-alerts`)
      .subscribe({
        next: (res) => {
          this.flaggedProducts = res.data?.products || res.products || [];
          this.flaggedCount    = res.data?.flaggedCount ?? res.flaggedCount ?? 0;
          this.message         = res.data?.message || res.message || '';
          this.isLoading = false;
        },
        error: () => { this.isLoading = false; }
      });
  }

  getScoreColor(score: number): string {
    if (score <= 2) return '#ef4444';
    if (score <= 4) return '#f97316';
    return '#f59e0b';
  }

  getSuggestion(category: string): string {
    const suggestions: { [key: string]: string } = {
      'STICKER':   'Switch to recycled paper or biodegradable stickers',
      'HOODIE':    'Source organic cotton or recycled fleece alternatives',
      'TSHIRT':    'Consider bamboo fiber or organic cotton materials',
      'MOUSEPAD':  'Replace foam base with natural rubber alternatives',
      'KEYBOARD':  'Look for keyboards with recycled plastic components',
      'MUG':       'Replace plastic mugs with ceramic or bamboo options',
      'NOTEBOOK':  'Source recycled paper or FSC-certified notebooks',
      'CAP':       'Switch to organic cotton or recycled polyester',
      'BACKPACK':  'Source recycled nylon or canvas alternatives',
      'POSTER':    'Use recycled paper and eco-friendly inks',
    };
    return suggestions[category] || 'Consider more sustainable sourcing options';
  }
}