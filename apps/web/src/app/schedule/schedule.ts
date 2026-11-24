import { ChangeDetectionStrategy, Component } from '@angular/core';
import { WeekSchedule } from './week-schedule';

/**
 * The Schedule page: the week's schedule (SCRUM-65). It replaced today's piquete, the piquete list and the create
 * form (SCRUM-46, SCRUM-63); the week view keeps the last loaded week readable offline (RNF06).
 */
@Component({
  selector: 'app-schedule',
  imports: [WeekSchedule],
  templateUrl: './schedule.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Schedule {}
