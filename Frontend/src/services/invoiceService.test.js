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
import invoiceService from './invoiceService';

describe('invoiceService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Reads paged backend responses and returns their content array.
  it('list unwraps Spring page content', async () => {
    api.get.mockResolvedValueOnce({ data: { content: [{ invoiceId: 1 }] } });

    await expect(invoiceService.list({ page: 0 })).resolves.toEqual([{ invoiceId: 1 }]);
    expect(api.get).toHaveBeenCalledWith('/api/invoices', { params: { page: 0 } });
  });

  // Sends cancel reason as query params while keeping an empty body.
  it('cancel sends reason as request params', async () => {
    api.put.mockResolvedValueOnce({ data: { cancelled: true } });

    await expect(invoiceService.cancel(7, 'Musteri iptali')).resolves.toEqual({ cancelled: true });
    expect(api.put).toHaveBeenCalledWith('/api/invoices/7/cancel', null, { params: { reason: 'Musteri iptali' } });
  });

  // Requests binary PDF data with blob response type.
  it('downloadPdf requests blob response', async () => {
    const blob = new Blob(['pdf']);
    api.get.mockResolvedValueOnce({ data: blob });

    await expect(invoiceService.downloadPdf(9)).resolves.toBe(blob);
    expect(api.get).toHaveBeenCalledWith('/api/invoices/9/pdf', { responseType: 'blob' });
  });

  // Creates e-invoice profile through query params expected by the backend.
  it('saveGibProfile sends profile values as params', async () => {
    api.post.mockResolvedValueOnce({ data: { ok: true } });

    await expect(invoiceService.saveGibProfile('alias', 'secret', 'A')).resolves.toEqual({ ok: true });
    expect(api.post).toHaveBeenCalledWith('/api/invoices/gib-profile', null, {
      params: { alias: 'alias', password: 'secret', defaultSeriesCode: 'A' },
    });
  });

  // Covers standard invoice CRUD endpoint contracts.
  it('calls standard CRUD endpoints', async () => {
    api.get.mockResolvedValueOnce({ data: [{ invoiceId: 1 }] });
    api.get.mockResolvedValueOnce({ data: { invoiceId: 1 } });
    api.post.mockResolvedValueOnce({ data: { invoiceId: 2 } });
    api.put.mockResolvedValueOnce({ data: { invoiceId: 2 } });
    api.delete.mockResolvedValueOnce({ data: undefined });

    await expect(invoiceService.list()).resolves.toEqual([{ invoiceId: 1 }]);
    await expect(invoiceService.get(1)).resolves.toEqual({ invoiceId: 1 });
    await expect(invoiceService.create({ invoiceNumber: 'FTR-1' })).resolves.toEqual({ invoiceId: 2 });
    await expect(invoiceService.update(2, { invoiceNumber: 'FTR-2' })).resolves.toEqual({ invoiceId: 2 });
    await expect(invoiceService.remove(2)).resolves.toBeUndefined();

    expect(api.get).toHaveBeenNthCalledWith(1, '/api/invoices', { params: undefined });
    expect(api.get).toHaveBeenNthCalledWith(2, '/api/invoices/1');
    expect(api.post).toHaveBeenCalledWith('/api/invoices', { invoiceNumber: 'FTR-1' });
    expect(api.put).toHaveBeenCalledWith('/api/invoices/2', { invoiceNumber: 'FTR-2' });
    expect(api.delete).toHaveBeenCalledWith('/api/invoices/2');
  });

  // Covers invoice workflow endpoint contracts.
  it('calls workflow endpoints for restore, confirm, return, and payment status', async () => {
    api.put.mockResolvedValue({ data: { ok: true } });
    api.post.mockResolvedValueOnce({ data: { invoiceId: 8 } });

    await expect(invoiceService.restore(3)).resolves.toEqual({ ok: true });
    await expect(invoiceService.confirm(3)).resolves.toEqual({ ok: true });
    await expect(invoiceService.createReturn(3, { invoiceNumber: 'RET-1' })).resolves.toEqual({ invoiceId: 8 });
    await expect(invoiceService.setPayment(3, 'paid')).resolves.toEqual({ ok: true });

    expect(api.put).toHaveBeenCalledWith('/api/invoices/3/restore');
    expect(api.put).toHaveBeenCalledWith('/api/invoices/3/confirm');
    expect(api.post).toHaveBeenCalledWith('/api/invoices/3/return', { invoiceNumber: 'RET-1' });
    expect(api.put).toHaveBeenCalledWith('/api/invoices/3/payment-status', null, { params: { status: 'paid' } });
  });

  // Covers lookup and series endpoint contracts.
  it('calls customer and series endpoints', async () => {
    api.get.mockResolvedValueOnce({ data: [{ invoiceId: 1 }] });
    api.get.mockResolvedValueOnce({ data: [{ seriesCode: 'A' }] });
    api.post.mockResolvedValueOnce({ data: { seriesCode: 'A' } });
    api.get.mockResolvedValueOnce({ data: 'SAT-000001' });

    await expect(invoiceService.byCustomer(10)).resolves.toEqual([{ invoiceId: 1 }]);
    await expect(invoiceService.getSeries()).resolves.toEqual([{ seriesCode: 'A' }]);
    await expect(invoiceService.createSeries({ seriesCode: 'A' })).resolves.toEqual({ seriesCode: 'A' });
    await expect(invoiceService.nextNumber('sale', 'A')).resolves.toEqual('SAT-000001');

    expect(api.get).toHaveBeenCalledWith('/api/invoices/customer/10');
    expect(api.get).toHaveBeenCalledWith('/api/invoices/series');
    expect(api.post).toHaveBeenCalledWith('/api/invoices/series', { seriesCode: 'A' });
    expect(api.get).toHaveBeenCalledWith('/api/invoices/series/next-number', {
      params: { invoiceType: 'sale', seriesCode: 'A' },
    });
  });

  // Covers late fee and GIB operational endpoint contracts.
  it('calls late fee and GIB endpoints', async () => {
    api.get.mockResolvedValue({ data: { ok: true } });
    api.put.mockResolvedValue({ data: { ok: true } });
    api.post.mockResolvedValue({ data: { ok: true } });

    await invoiceService.getLateFee(3);
    await invoiceService.getLateFeeConfig();
    await invoiceService.updateLateFeeConfig(2.5, 7);
    await invoiceService.sendToGib(3);
    await invoiceService.getGibLogs(3);
    await invoiceService.checkGibStatus(3);
    await invoiceService.getGibProfile();

    expect(api.get).toHaveBeenCalledWith('/api/invoices/3/late-fee');
    expect(api.get).toHaveBeenCalledWith('/api/invoices/late-fee-config');
    expect(api.put).toHaveBeenCalledWith('/api/invoices/late-fee-config', null, {
      params: { monthlyRate: 2.5, gracePeriodDays: 7 },
    });
    expect(api.post).toHaveBeenCalledWith('/api/invoices/3/send-to-gib');
    expect(api.get).toHaveBeenCalledWith('/api/invoices/3/gib-logs');
    expect(api.put).toHaveBeenCalledWith('/api/invoices/3/gib-status');
    expect(api.get).toHaveBeenCalledWith('/api/invoices/gib-profile');
  });
});
