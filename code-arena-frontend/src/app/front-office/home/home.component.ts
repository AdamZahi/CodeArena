import { Component, OnInit, OnDestroy, HostListener, AfterViewInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, AfterViewInit, OnDestroy {

  /* ── Auth0 ── */
  private readonly auth = inject(AuthService);
  logout(): void {
    void this.auth.logout({
      logoutParams: { returnTo: window.location.origin, client_id: environment.auth0ClientId }
    });
  }

  /* ── State ── */
  sidebarOpen  = false;
  navScrolled  = false;
  navHidden    = false;
  heroLoaded   = false;
  scrollProgress = 0;
  private lastY = 0;
  private rafId: number | null = null;
  private particles: P[] = [];
  private canvas!: HTMLCanvasElement;
  private ctx!: CanvasRenderingContext2D;

  /* ── Data ── */
  stats = [
    { display: '12K+', label: 'Coders' },
    { display: '3K+',  label: 'Challenges' },
    { display: '500+', label: 'Battles/day' },
    { display: '8',    label: 'Modules' },
  ];

  steps = [
    { title: 'Create Profile',   desc: 'Sign up and set your skill level. Customize your arena identity.' },
    { title: 'Pick a Challenge', desc: 'Browse coding problems across all difficulty levels and languages.' },
    { title: 'Battle Others',    desc: 'Enter 1v1 rooms and beat opponents in real-time timed duels.' },
    { title: 'Earn & Level Up',  desc: 'Gain XP, climb rankings, unlock merch, events, and coaching.' },
  ];

  modules = [
    { num:'MODULE 01', name:'Challenges',         desc:'Solve curated problems, submit code, get auto-evaluated and earn XP.',                    color:'#38bdf8', route:'/challenge' },
    { num:'MODULE 02', name:'Battles',            desc:'1v1 real-time coding duels. Pick a theme, enter a room, prove you are the best.',         color:'#818cf8', route:'/battle' },
    { num:'MODULE 03', name:'Rewards & Profile',  desc:'Customize your profile, track your ranking, unlock exclusive rewards by performance.',     color:'#f59e0b', route:'/reward-profile' },
    { num:'MODULE 04', name:'Shop',               desc:'Exclusive CodeArena merch — hoodies, mugs, stickers. Rep your arena status in real life.', color:'#34d399', route:'/shop' },
    { num:'MODULE 05', name:'Arena Hubs',         desc:'Voice rooms, text channels, fan support — a Discord-like community built right in.',       color:'#f472b6', route:'/' },
    { num:'MODULE 06', name:'Events',             desc:'Real-life hackathons and competitions. Top rankers get free entry.',                        color:'#60a5fa', route:'/event' },
    { num:'MODULE 07', name:'Coaching & Quizzes', desc:'Book 1-on-1 sessions with expert coaches or join free open sessions to get started.',      color:'#4ade80', route:'/coaching-quiz' },
    { num:'MODULE 00', name:'User Management',    desc:'Secure accounts for participants, organisateurs and admins. The backbone of the arena.',    color:'#94a3b8', route:'/' },
  ];

  aboutTags = [
    { label: 'Challenges', color: '#38bdf8' },
    { label: 'Battles',    color: '#818cf8' },
    { label: 'Rewards',    color: '#f59e0b' },
    { label: 'Shop',       color: '#34d399' },
    { label: 'Events',     color: '#60a5fa' },
    { label: 'Coaching',   color: '#4ade80' },
  ];

  perks = [
    { text: 'Gamified learning that keeps you coming back',   color: '#38bdf8' },
    { text: 'Real competition with a real community',         color: '#818cf8' },
    { text: 'For all ages — especially the next gen of devs', color: '#f59e0b' },
    { text: 'Built with Spring Boot and Angular',             color: '#34d399' },
  ];

  /* ── Lifecycle ── */
  ngOnInit(): void {
    setTimeout(() => { this.heroLoaded = true; }, 120);
  }

  ngAfterViewInit(): void {
    this.initCanvas();
    this.setupReveal();
  }

  ngOnDestroy(): void {
    if (this.rafId) cancelAnimationFrame(this.rafId);
    document.body.style.overflow = '';
  }

  /* ── Scroll ── */
  @HostListener('window:scroll')
  onScroll(): void {
    const y = window.scrollY;
    const max = document.documentElement.scrollHeight - window.innerHeight;
    this.navScrolled  = y > 40;
    this.navHidden    = y > this.lastY && y > 180;
    this.scrollProgress = Math.min((y / max) * 100, 100);
    this.lastY = y;
  }

  @HostListener('document:keydown.escape')
  onEsc(): void { this.closeSidebar(); }

  /* ── Sidebar ── */
  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
    document.body.style.overflow = this.sidebarOpen ? 'hidden' : '';
  }
  closeSidebar(): void {
    this.sidebarOpen = false;
    document.body.style.overflow = '';
  }

  /* ── Scroll Reveal ── */
  private setupReveal(): void {
    const els = document.querySelectorAll('.section, .mod, .step-item');
    const obs = new IntersectionObserver(entries => {
      entries.forEach(e => {
        if (e.isIntersecting) {
          (e.target as HTMLElement).style.opacity = '1';
          (e.target as HTMLElement).style.transform = 'translateY(0)';
          obs.unobserve(e.target);
        }
      });
    }, { threshold: 0.07 });
    els.forEach((el, i) => {
      const h = el as HTMLElement;
      h.style.opacity = '0';
      h.style.transform = 'translateY(24px)';
      h.style.transition = `opacity .65s ease ${(i % 5) * 0.07}s, transform .65s ease ${(i % 5) * 0.07}s`;
      obs.observe(el);
    });
  }

  /* ── Particles ── */
  private initCanvas(): void {
    this.canvas = document.getElementById('ca') as HTMLCanvasElement;
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext('2d')!;
    this.resize();
    window.addEventListener('resize', () => this.resize());
    this.spawn();
    this.tick();
  }

  private resize(): void {
    this.canvas.width  = window.innerWidth;
    this.canvas.height = window.innerHeight;
  }

  private spawn(): void {
    const n = Math.min(Math.floor(window.innerWidth / 18), 55);
    const cols = ['#38bdf8','#818cf8','#a78bfa','#6366f1','#7dd3fc'];
    this.particles = Array.from({ length: n }, () => ({
      x: Math.random() * window.innerWidth,
      y: Math.random() * window.innerHeight,
      vx: (Math.random() - .5) * .3,
      vy: (Math.random() - .5) * .3,
      r: Math.random() * 1.8 + .3,
      a: Math.random() * .4 + .1,
      c: cols[Math.floor(Math.random() * cols.length)],
      p: Math.random() * Math.PI * 2,
    }));
  }

  private tick(): void {
    const loop = () => {
      const { width: W, height: H } = this.canvas;
      this.ctx.clearRect(0, 0, W, H);
      const ps = this.particles;
      for (const p of ps) {
        p.x += p.vx; p.y += p.vy; p.p += .011;
        if (p.x < 0) p.x = W; if (p.x > W) p.x = 0;
        if (p.y < 0) p.y = H; if (p.y > H) p.y = 0;
        const a = p.a * (.6 + .4 * Math.sin(p.p));
        this.ctx.beginPath();
        this.ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        this.ctx.fillStyle = p.c + Math.floor(a * 255).toString(16).padStart(2, '0');
        this.ctx.fill();
      }
      for (let i = 0; i < ps.length; i++) {
        for (let j = i + 1; j < ps.length; j++) {
          const dx = ps[i].x - ps[j].x, dy = ps[i].y - ps[j].y;
          const d = Math.sqrt(dx * dx + dy * dy);
          if (d < 110) {
            this.ctx.beginPath();
            this.ctx.moveTo(ps[i].x, ps[i].y);
            this.ctx.lineTo(ps[j].x, ps[j].y);
            this.ctx.strokeStyle = `rgba(99,102,241,${.05 * (1 - d / 110)})`;
            this.ctx.lineWidth = .4;
            this.ctx.stroke();
          }
        }
      }
      this.rafId = requestAnimationFrame(loop);
    };
    loop();
  }
}

interface P { x:number; y:number; vx:number; vy:number; r:number; a:number; c:string; p:number; }