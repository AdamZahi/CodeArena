import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="footer">
      <div class="footer-line"></div>
      <div class="footer-content">
        <div class="footer-left">
          <span class="platform-name">CODEARENA v2.0</span>
          <span class="status-indicator">SYSTEM ONLINE</span>
        </div>
        <div class="footer-center">
          <span class="tagline">BUILD. COMPETE. IMPROVE.</span>
        </div>
        <div class="footer-right">
          <span class="timestamp">{{ today | date:'yyyy.MM.dd HH:mm' }}</span>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      margin-top: auto;
      background: #0d0d15;
      position: relative;
      padding: 20px 40px;
      border-top: 1px solid #1a1a2e;
      font-family: 'Orbitron', monospace;
    }

    .footer-line {
      position: absolute;
      top: -1px;
      left: 0;
      width: 100%;
      height: 1px;
      background: linear-gradient(90deg, transparent, #06b6d4, #8b5cf6, transparent);
      opacity: 0.4;
    }

    .footer-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      max-width: 1400px;
      margin: 0 auto;
    }

    .footer-left {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .platform-name {
      font-size: 11px;
      font-weight: 700;
      color: #8b5cf6;
      letter-spacing: 2px;
    }

    .status-indicator {
      font-size: 8px;
      color: #10b981;
      letter-spacing: 1px;
    }

    .footer-center {
      text-align: center;
    }

    .tagline {
      font-size: 10px;
      color: #64748b;
      letter-spacing: 4px;
    }

    .footer-right {
      text-align: right;
    }

    .timestamp {
      font-size: 9px;
      color: #475569;
      letter-spacing: 1px;
    }
  `]
})
export class FooterComponent {
  today: Date = new Date();
}
