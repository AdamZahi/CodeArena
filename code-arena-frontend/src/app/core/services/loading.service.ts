import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  readonly isLoading$ = new BehaviorSubject<boolean>(false);

  setLoading(loading: boolean): void {
    // TODO: Replace boolean with request counter if needed.
    this.isLoading$.next(loading);
  }
}
