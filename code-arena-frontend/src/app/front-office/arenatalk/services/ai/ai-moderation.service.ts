import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface ModerationResult {
  safe: boolean;
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class AiModerationService {

  private apiUrl = 'https://api.groq.com/openai/v1/chat/completions';
  private apiKey = environment.groqApiKey;

  moderate(content: string): Observable<ModerationResult> {
    const body = {
      model: 'llama-3.3-70b-versatile',
      messages: [
        {
          role: 'system',
          content: 'You are a content moderation assistant. Analyze messages for toxicity, hate speech, spam, or offensive content. Always respond with valid JSON only.'
        },
        {
          role: 'user',
          content: `Is this message toxic, offensive, or spam? Respond ONLY with JSON: {"safe": true, "reason": ""} or {"safe": false, "reason": "brief reason"}\n\nMessage: "${content}"`
        }
      ],
      max_tokens: 100,
      temperature: 0.1
    };

    return new Observable(observer => {
      fetch(this.apiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`
        },
        body: JSON.stringify(body)
      })
      .then(res => res.json())
      .then(data => {
        const text = data.choices?.[0]?.message?.content ?? '{"safe": true, "reason": ""}';
        try {
          const clean = text.replace(/```json|```/g, '').trim();
          observer.next(JSON.parse(clean));
        } catch {
          observer.next({ safe: true, reason: '' });
        }
        observer.complete();
      })
      .catch(err => observer.error(err));
    });
  }
}