import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { AuthUserSyncService } from './core/auth/auth-user-sync.service';
import { provideRouter } from '@angular/router';

describe('AppComponent', () => {
  let authUserSyncServiceMock: jasmine.SpyObj<AuthUserSyncService>;

  beforeEach(async () => {
    authUserSyncServiceMock = jasmine.createSpyObj('AuthUserSyncService', ['keepAlive']);

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        { provide: AuthUserSyncService, useValue: authUserSyncServiceMock }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should call authUserSync.keepAlive on init', () => {
    TestBed.createComponent(AppComponent);
    expect(authUserSyncServiceMock.keepAlive).toHaveBeenCalled();
  });
});
