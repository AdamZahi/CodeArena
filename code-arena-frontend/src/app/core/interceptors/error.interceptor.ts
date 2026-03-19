import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error) => {
      // TODO: Handle 401 re-login, 403 forbidden redirect, 500 toast notifications.
      return throwError(() => error);
    })
  );
};
