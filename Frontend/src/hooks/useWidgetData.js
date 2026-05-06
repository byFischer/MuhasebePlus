import { useQuery } from '@tanstack/react-query';
import dashboardService from '@/services/dashboardService';

export function useWidgetData(layoutId, widgetId, enabled = true) {
  return useQuery({
    queryKey: ['widgetData', layoutId, widgetId],
    queryFn: () => dashboardService.getWidgetData(layoutId, widgetId),
    enabled: !!layoutId && !!widgetId && enabled,
    staleTime: 2 * 60 * 1000,
  });
}
