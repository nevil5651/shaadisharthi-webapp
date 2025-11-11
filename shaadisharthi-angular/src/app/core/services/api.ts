import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  //private baseUrl = environment.apiUrl;
  private baseUrl = `${environment.apiUrl}`; 

  constructor(private http: HttpClient) { }

  get<T>(endpoint:string, options?: object) {
    // The `options` object can contain parameters, headers, etc.
    // This now correctly passes them to the underlying HttpClient.
    return this.http.get<T>(`${this.baseUrl}/${endpoint}`, options);
  }

  post<T>(endpoint: string, body: any) {
    return this.http.post<T>(`${this.baseUrl}/${endpoint}`, body);
  }

  put<T>(endpoint: string, body: any) {
    return this.http.put<T>(`${this.baseUrl}/${endpoint}`, body);
  }

  delete<T>(endpoint: string) {
    return this.http.delete<T>(`${this.baseUrl}/${endpoint}`);
  }

  // delete<T>(endpoint: string) {
  //   return this.http.delete<T>(`${this.baseUrl}/${endpoint}`, this.getHeaders());
  // }

  // private getHeaders() {
  //   const token = localStorage.getItem('auth_token');
  //   return {
  //     headers: new HttpHeaders({
  //       'Content-Type': 'application/json',
  //       'Authorization': `Bearer ${token}`
  //     })
  //   };
  // }
}