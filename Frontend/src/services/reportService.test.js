import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '@/services/api';
import reportService from './reportService';

describe('reportService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Generates reports by posting the exact DTO to the backend.
  it('generate posts report request dto', async () => {
    const dto = { reportType: 'PROFIT_LOSS', startDate: '2026-01-01', endDate: '2026-12-31', format: 'EXCEL' };
    api.post.mockResolvedValueOnce({ data: { reportId: 1 } });

    await expect(reportService.generate(dto)).resolves.toEqual({ reportId: 1 });
    expect(api.post).toHaveBeenCalledWith('/api/reports/generate', dto);
  });

  // Previews report data without mutating report records.
  it('preview posts preview request dto', async () => {
    const dto = { reportType: 'CASH_FLOW', startDate: '2026-01-01', endDate: '2026-01-31' };
    api.post.mockResolvedValueOnce({ data: { sections: [] } });

    await expect(reportService.preview(dto)).resolves.toEqual({ sections: [] });
    expect(api.post).toHaveBeenCalledWith('/api/reports/preview', dto);
  });

  // Downloads report files as blobs.
  it('download requests blob response', async () => {
    const blob = new Blob(['xlsx']);
    api.get.mockResolvedValueOnce({ data: blob });

    await expect(reportService.download(3)).resolves.toBe(blob);
    expect(api.get).toHaveBeenCalledWith('/api/reports/3/download', { responseType: 'blob' });
  });

  // Requests customer statement with date range params.
  it('getStatement sends customer id and date params', async () => {
    api.get.mockResolvedValueOnce({ data: [{ balance: 100 }] });

    await expect(reportService.getStatement(4, '2026-01-01', '2026-01-31')).resolves.toEqual([{ balance: 100 }]);
    expect(api.get).toHaveBeenCalledWith('/api/reports/statement/4', {
      params: { startDate: '2026-01-01', endDate: '2026-01-31' },
    });
  });

  // Covers standard report read and lifecycle endpoints.
  it('calls list, get, remove, and restore endpoints', async () => {
    api.get.mockResolvedValueOnce({ data: [{ reportId: 1 }] });
    api.get.mockResolvedValueOnce({ data: { reportId: 1 } });
    api.delete.mockResolvedValueOnce({ data: undefined });
    api.put.mockResolvedValueOnce({ data: { reportId: 1, isDeleted: false } });

    await expect(reportService.list()).resolves.toEqual([{ reportId: 1 }]);
    await expect(reportService.get(1)).resolves.toEqual({ reportId: 1 });
    await expect(reportService.remove(1)).resolves.toBeUndefined();
    await expect(reportService.restore(1)).resolves.toEqual({ reportId: 1, isDeleted: false });

    expect(api.get).toHaveBeenCalledWith('/api/reports');
    expect(api.get).toHaveBeenCalledWith('/api/reports/1');
    expect(api.delete).toHaveBeenCalledWith('/api/reports/1');
    expect(api.put).toHaveBeenCalledWith('/api/reports/1/restore');
  });

  // Covers reconciliation and statement PDF endpoint params.
  it('calls reconciliation and statement pdf endpoints', async () => {
    const blob = new Blob(['pdf']);
    api.get.mockResolvedValueOnce({ data: { status: 'MATCHED' } });
    api.get.mockResolvedValueOnce({ data: blob });

    await expect(reportService.getReconciliation(4, '2026-01-01', '2026-01-31')).resolves.toEqual({ status: 'MATCHED' });
    await expect(reportService.downloadStatementPdf(4, '2026-01-01', '2026-01-31')).resolves.toBe(blob);

    expect(api.get).toHaveBeenCalledWith('/api/reports/reconciliation/4', {
      params: { periodStart: '2026-01-01', periodEnd: '2026-01-31' },
    });
    expect(api.get).toHaveBeenCalledWith('/api/reports/statement/4/pdf', {
      params: { startDate: '2026-01-01', endDate: '2026-01-31' },
      responseType: 'blob',
    });
  });
});
