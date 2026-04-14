import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AiSmartReplyService {

  private apiUrl = 'https://api.groq.com/openai/v1/chat/completions';
  private apiKey = environment.groqApiKey;

  getSuggestions(recentMessages: { sender: string; content: string }[]): Observable<string[]> {
    const formatted = recentMessages
      .slice(-5)
      .map(m => `${m.sender}: ${m.content}`)
      .join('\n');

    const body = {
      model: 'llama-3.3-70b-versatile',
      messages: [
        {
          role: 'system',
          content: 'You are a chat assistant. Suggest short reply options. Always respond with a valid JSON array of exactly 3 strings. No explanation, no markdown, just the JSON array.'
        },
        {
          role: 'user',
          content: `Based on this conversation, suggest exactly 3 short replies (max 8 words each). Return ONLY a JSON array like: ["reply1", "reply2", "reply3"]\n\nConversation:\n${formatted}`
        }
      ],
      max_tokens: 150,
      temperature: 0.7
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
        const text = data.choices?.[0]?.message?.content ?? '[]';
        try {
          const clean = text.replace(/```json|```/g, '').trim();
          observer.next(JSON.parse(clean));
        } catch {
          observer.next(['Sounds good!', 'Can you explain more?', 'I agree 👍']);
        }
        observer.complete();
      })
      .catch(err => observer.error(err));
    });
  }
}