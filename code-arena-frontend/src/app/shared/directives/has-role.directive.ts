import { Directive, Input, TemplateRef, ViewContainerRef } from '@angular/core';

@Directive({
  selector: '[hasRole]',
  standalone: true
})
export class HasRoleDirective {
  @Input() set hasRole(role: string) {
    // TODO: Integrate with KeycloakService and conditionally render.
    this.viewContainer.clear();
    if (role) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }

  constructor(private readonly templateRef: TemplateRef<unknown>, private readonly viewContainer: ViewContainerRef) {}
}
