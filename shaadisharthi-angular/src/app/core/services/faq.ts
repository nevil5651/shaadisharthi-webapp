import { Injectable, inject } from '@angular/core';
import { ApiService } from './api';
import { Observable } from 'rxjs';

export interface Faq {
  id: number;
  question: string;
  answer: string;
}

export interface FaqQuery {
  subject: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class FaqService {
  private apiService = inject(ApiService);


  /**
   * Submits a new user query to the backend servlet.
   * @param query The query object containing the subject and message from the form.
   */
  addQuery(query: FaqQuery): Observable<any> {
    // This will send a POST request to your backend endpoint (e.g., 'support/query').
    return this.apiService.post('ServiceProvider/AddSupportQuery', query);
  }
}
