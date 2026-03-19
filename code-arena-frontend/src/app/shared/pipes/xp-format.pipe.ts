import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'xpFormat',
  standalone: true
})
export class XpFormatPipe implements PipeTransform {
  transform(value: number): string {
    // TODO: Implement XP compact number formatting.
    return `${value} XP`;
  }
}
