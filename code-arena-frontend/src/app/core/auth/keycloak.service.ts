import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private readonly keycloak = new Keycloak({
    url: environment.keycloak.url,
    realm: environment.keycloak.realm,
    clientId: environment.keycloak.clientId
  });

  readonly userProfile$ = new BehaviorSubject<Record<string, unknown> | null>(null);

  async init(): Promise<boolean> {
    // TODO: Configure onLoad/check-sso strategy and token refresh hooks.
    return this.keycloak.init({ onLoad: 'check-sso' });
  }

  async login(): Promise<void> {
    // TODO: Pass redirectUri and optional idpHint if needed.
    await this.keycloak.login();
  }

  async logout(): Promise<void> {
    // TODO: Wire post-logout redirect to login page.
    await this.keycloak.logout();
  }

  getToken(): string | undefined {
    // TODO: Refresh token when expired.
    return this.keycloak.token;
  }

  hasRole(role: string): boolean {
    // TODO: Support both realm and resource roles.
    return this.keycloak.hasRealmRole(role);
  }

  isAuthenticated(): boolean {
    return !!this.keycloak.authenticated;
  }
}
