import { Injectable } from '@angular/core';
import { environment } from '../../../../../environments/environment';

export interface CorrectionResult {
  original: string;
  corrected: string;
  hasChanges: boolean;
}

@Injectable({ providedIn: 'root' })
export class MessageAutocorrectService {

  private readonly groqApiUrl = 'https://api.groq.com/openai/v1/chat/completions';
  private readonly model = 'llama-3.1-8b-instant';

  async correctMessage(text: string): Promise<CorrectionResult> {
    if (!text || text.trim().length < 3) {
      return { original: text, corrected: text, hasChanges: false };
    }

    try {
      const response = await fetch(this.groqApiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${environment.groqApiKey}`
        },
        body: JSON.stringify({
          model: this.model,
          max_tokens: 200,
          messages: [
            {
              role: 'system',
              content: `You are a real-time chat message corrector. Fix spelling, grammar, and punctuation errors. Keep the same language. Return ONLY the corrected message, nothing else. If already correct, return as-is.`
            },
            {
              role: 'user',
              content: text
            }
          ]
        })
      });

      if (!response.ok) return { original: text, corrected: text, hasChanges: false };

      const data = await response.json();
      const corrected = data.choices?.[0]?.message?.content?.trim() || text;
      const hasChanges = corrected.toLowerCase() !== text.toLowerCase();

      return { original: text, corrected, hasChanges };

    } catch (error) {
      console.error('Autocorrect error:', error);
      return { original: text, corrected: text, hasChanges: false };
    }
  }
}