import { Directive, ElementRef, Input, OnChanges } from '@angular/core';

@Directive({
  selector: '[appVideoStream]',
  standalone: true
})
export class VideoStreamDirective implements OnChanges {
  @Input() appVideoStream: MediaStream | null = null;

  constructor(private el: ElementRef<HTMLVideoElement>) {}

  ngOnChanges(): void {
    if (this.appVideoStream) {
      this.el.nativeElement.srcObject = this.appVideoStream;
    } else {
      this.el.nativeElement.srcObject = null;
    }
  }
}