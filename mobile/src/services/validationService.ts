import type { ValidationRulesResponse } from '../models';
import { apiRequest } from './http';

export function getValidationRules(): Promise<ValidationRulesResponse> {
  return apiRequest<ValidationRulesResponse>('/api/v1/config/validation');
}
