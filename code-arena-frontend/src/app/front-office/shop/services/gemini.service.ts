import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class GeminiService {

  // ── GEMINI API CONFIG ─────────────────────────
  // Free tier: 15 requests/minute, 1 million tokens/day
  // We use gemini-1.5-flash — fastest and free
  private readonly API_KEY = 'AIzaSyCfOqWTvnx56lfAk-s0bMZoEEk9rzHMAVE';
  private readonly API_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${this.API_KEY}`;

  constructor(private http: HttpClient) {}

  // ── CORE METHOD ───────────────────────────────
  // Sends a prompt to Gemini and returns the text response
  private generate(prompt: string): Observable<string> {
    const body = {
      contents: [{
        parts: [{ text: prompt }]
      }]
    };

    return this.http.post<any>(this.API_URL, body).pipe(
      map(res => res.candidates[0].content.parts[0].text)
    );
  }

  // ── A: POST ORDER FUN MESSAGE ─────────────────
  // Called after successful order — generates a gamer-tone congratulations
  // based on what was purchased
  generateOrderMessage(items: string[], total: number): Observable<string> {
    const itemList = items.join(', ');
    const prompt = `
      You are the AI assistant for CodeArena, a gamified coding platform shop.
      A participant just placed an order for: ${itemList}
      Total spent: $${total.toFixed(2)}

      Generate a SHORT (max 3 sentences), fun, gamer-tone congratulations message.
      Use coding/gaming references. Be enthusiastic and encouraging.
      Include 1-2 emojis. Do NOT use markdown formatting.
      Start with something like "WARRIOR!" or "LEGEND!" or "CHAMPION!"
    `;
    return this.generate(prompt);
  }

  // ── B: SMART CART SUMMARY ─────────────────────
  // Called before checkout — summarizes cart in gamer tone
  generateCartSummary(items: string[], total: number): Observable<string> {
    const itemList = items.join(', ');
    const prompt = `
      You are the AI assistant for CodeArena, a gamified coding platform shop.
      A participant has these items in their cart: ${itemList}
      Total: $${total.toFixed(2)}

      Generate a SHORT (max 2 sentences) fun gamer-tone summary of their cart.
      Make it sound like they are gearing up for battle.
      Use coding/gaming references. Include 1-2 emojis. No markdown.
    `;
    return this.generate(prompt);
  }

  // ── C: PRODUCT RECOMMENDATIONS ────────────────
  // Called based on cart contents — suggests other products
  generateRecommendations(cartItems: string[], availableProducts: string[]): Observable<string> {
    const prompt = `
      You are the AI assistant for CodeArena shop.
      The participant has in their cart: ${cartItems.join(', ')}
      Available products in the shop: ${availableProducts.join(', ')}

      Recommend 2-3 products from the available list that complement their cart.
      Be brief and fun. Use gamer tone. No markdown. Max 2 sentences.
      Format: just list the product names separated by commas.
    `;
    return this.generate(prompt);
  }

  // ── D: AI PRODUCT DESCRIPTION ─────────────────
  // Called when viewing a product — generates "why you'll love this"
  generateProductDescription(productName: string, category: string): Observable<string> {
    const prompt = `
      You are the AI assistant for CodeArena, a gamified coding platform shop.
      Generate a SHORT (max 2 sentences) exciting "why you'll love this" description
      for: ${productName} (category: ${category})
      
      Use gamer/developer tone. Be enthusiastic. Include 1 emoji. No markdown.
    `;
    return this.generate(prompt);
  }

  // ── ECO SCORE ─────────────────────────────────
// Generates an eco sustainability score for a product
// Returns score (1-10) and a brief explanation
// Linked to SDG 12: Responsible Consumption & Production
generateEcoScore(productName: string, category: string): Observable<string> {
  const prompt = `
    You are a sustainability expert for CodeArena shop.
    Rate this product for environmental sustainability:
    Product: ${productName}
    Category: ${category}

    Respond with ONLY this exact JSON format, nothing else:
    {"score": 7, "label": "Good", "reason": "One short sentence explanation"}

    Score guide:
    1-3: Poor (single-use, non-recyclable, high waste)
    4-6: Average (standard materials, some environmental impact)
    7-8: Good (durable, reusable, low waste)
    9-10: Excellent (sustainable materials, minimal impact)

    Categories like KEYBOARD, MOUSEPAD score higher (durable electronics/accessories)
    Categories like STICKER score lower (single use)
    Categories like HOODIE, TSHIRT score medium (depends on use)
  `;
  return this.generate(prompt);
}

}