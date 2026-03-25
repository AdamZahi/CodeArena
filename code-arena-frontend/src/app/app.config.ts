import { APP_INITIALIZER, ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';
import { KeycloakService } from './core/auth/keycloak.service';

function initKeycloak(keycloakService: KeycloakService): () => Promise<boolean> {
  return () => keycloakService.init();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor, errorInterceptor, loadingInterceptor])),
    provideAnimations(),
    /*
    {
      provide: APP_INITIALIZER,
      multi: true,
      deps: [KeycloakService],
      useFactory: initKeycloak
    }
    */
  ]
};
