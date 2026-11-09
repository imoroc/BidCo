import { ChangeDetectionStrategy, AfterViewInit, Component, OnInit, OnDestroy } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { Subject, takeUntil } from 'rxjs';

Chart.register(...registerables);

@Component({
  selector: 'app-profile-estadisticas',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  templateUrl: './profile-estadisticas.component.html',
  styleUrls: ['./profile-estadisticas.component.css']
})
export class ProfileEstadisticasComponent implements OnInit, AfterViewInit, OnDestroy {
  participatedBids = 0;
  wonBids = 0;
  createdBids = 0;
  participatedDraws = 0;
  wonDraws = 0;
  createdDraws = 0;

  private destroy$ = new Subject<void>();
  private chart!: Chart;

  constructor(
    private userService: UserService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  ngAfterViewInit(): void {
    this.initChart();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.chart) this.chart.destroy();
  }

  private loadStats(): void {
    const userId = this.authService.currentUser.value?.usuarioID;
    if (userId) {
      this.userService.getUserStats(userId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (stats) => {
            this.participatedBids = stats.participatedBids;
            this.wonBids = stats.wonBids;
            this.createdBids = stats.createdBids;
            this.participatedDraws = stats.participatedDraws;
            this.wonDraws = stats.wonDraws;
            this.createdDraws = stats.createdDraws;
            this.updateChart();
          }
        });
    }
  }

  private initChart(): void {
    const ctx = document.getElementById('barChart') as HTMLCanvasElement;
    // Los textos y rejillas del gráfico siguen el tema (claro u oscuro)
    const estilos = getComputedStyle(document.documentElement);
    Chart.defaults.color = estilos.getPropertyValue('--color-texto-suave').trim() || '#5f6368';
    Chart.defaults.borderColor = estilos.getPropertyValue('--color-borde-suave').trim() || '#e8eaed';
    this.chart = new Chart(ctx, {
      type: 'bar',
      data: this.getChartData(),
      options: this.getChartOptions()
    });
  }

  private updateChart(): void {
    if (this.chart) {
      this.chart.data.datasets[0].data = [this.participatedBids, this.participatedDraws];
      this.chart.data.datasets[1].data = [this.wonBids, this.wonDraws];
      this.chart.data.datasets[2].data = [this.createdBids, this.createdDraws];
      this.chart.update();
    }
  }

  private getChartData(): any {
    return {
      labels: ['Subastas', 'Sorteos'],
      datasets: [{
        label: 'Participadas',
        data: [this.participatedBids, this.participatedDraws],
        backgroundColor: 'rgba(54, 162, 235, 0.6)',
        borderColor: 'rgba(54, 162, 235, 1)',
        borderWidth: 1
      }, {
        label: 'Ganadas',
        data: [this.wonBids, this.wonDraws],
        backgroundColor: 'rgba(75, 192, 192, 0.6)',
        borderColor: 'rgba(75, 192, 192, 1)',
        borderWidth: 1
      }, {
        label: 'Creadas',
        data: [this.createdBids, this.createdDraws],
        backgroundColor: 'rgba(153, 102, 255, 0.6)',
        borderColor: 'rgba(153, 102, 255, 1)',
        borderWidth: 1
      }]
    };
  }

  private getChartOptions(): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            precision: 0
          }
        }
      }
    };
  }
}