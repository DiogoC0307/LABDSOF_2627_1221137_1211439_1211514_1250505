import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

interface Slot {
  id: string;
  role: string;
  status: string;
}

interface Piquete {
  id: string;
  dutyDate: string;
  slots: Slot[];
}

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly piquetes = signal<Piquete[]>([]);
  protected readonly error = signal<string | null>(null);
  protected dutyDate = new Date().toISOString().slice(0, 10);

  ngOnInit(): void {
    this.reload();
  }

  protected create(): void {
    this.error.set(null);
    this.http.post<Piquete>('/api/v1/piquetes', { dutyDate: this.dutyDate }).subscribe({
      next: () => this.reload(),
      error: () => this.error.set('Could not create the piquete.'),
    });
  }

  private reload(): void {
    this.http.get<Piquete[]>('/api/v1/piquetes').subscribe({
      next: (list) => this.piquetes.set(list),
      error: () => this.error.set('Could not load piquetes.'),
    });
  }
}
