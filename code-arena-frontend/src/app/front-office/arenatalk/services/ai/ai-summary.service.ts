import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AiSummaryService {

  private apiUrl = 'https://api.groq.com/openai/v1/chat/completions';
  private apiKey = environment.groqApiKey;

  summarize(messages: { sender: string; content: string }[]): Observable<string> {
    const formatted = messages.map(m => `${m.sender}: ${m.content}`).join('\n');

    const body = {
      model: 'llama-3.3-70b-versatile',
      messages: [
        {
          role: 'system',
          content: 'You are a community chat assistant. Summarize chat messages in 3-5 bullet points. Be concise and focus on main topics, decisions, and action items.'
        },
        {
          role: 'user',
          content: `Summarize these chat messages in bullet points format using • symbol:\n\n${formatted}`
        }
      ],
      max_tokens: 500,
      temperature: 0.3
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
        const text = data.choices?.[0]?.message?.content ?? 'Could not generate summary.';
        observer.next(text);
        observer.complete();
      })
      .catch(err => observer.error(err));
    });
  }
}