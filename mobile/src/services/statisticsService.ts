import type { TagStatisticsResponse } from '../models';
import { apiRequest } from './http';

export function getTagStatistics(): Promise<TagStatisticsResponse[]> {
  return apiRequest<TagStatisticsResponse[]>('/api/v1/statistics/tags');
}
