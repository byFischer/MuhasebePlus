-- Invoice: compound indexes for common filter combinations
-- findByPaymentStatusAndCompanyCompanyIdAndIsDeletedFalse (dashboard + invoice list)
CREATE INDEX IF NOT EXISTS idx_invoice_company_payment_status
    ON invoice(company_id, payment_status)
    WHERE is_deleted = false;

-- findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse
CREATE INDEX IF NOT EXISTS idx_invoice_company_type
    ON invoice(company_id, invoice_type)
    WHERE is_deleted = false;

-- findByDueDateBeforeAndCompanyCompanyId (overdue invoice check)
CREATE INDEX IF NOT EXISTS idx_invoice_company_due_date
    ON invoice(company_id, due_date)
    WHERE is_deleted = false;

-- Cleanup scheduler: findByIsDeletedTrueAndDeletedAtBefore
CREATE INDEX IF NOT EXISTS idx_invoice_deleted_at
    ON invoice(deleted_at)
    WHERE is_deleted = true;

-- Transaction: compound index for date range queries
-- findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalse (GelirGider page)
CREATE INDEX IF NOT EXISTS idx_transaction_company_date
    ON transaction(company_id, transaction_date DESC)
    WHERE is_deleted = false;

-- findByTransactionTypeAndCompanyCompanyIdAndIsDeletedFalse
CREATE INDEX IF NOT EXISTS idx_transaction_company_type
    ON transaction(company_id, transaction_type)
    WHERE is_deleted = false;

-- Cleanup scheduler: findByIsDeletedTrueAndDeletedAtBefore
CREATE INDEX IF NOT EXISTS idx_transaction_deleted_at
    ON transaction(deleted_at)
    WHERE is_deleted = true;

-- Report: ordered by generated_at (RaporPage loads most recent first)
CREATE INDEX IF NOT EXISTS idx_report_company_generated_at
    ON report(company_id, generated_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_report_deleted_at
    ON report(deleted_at)
    WHERE is_deleted = true;

-- Customer: cleanup + note query
CREATE INDEX IF NOT EXISTS idx_customer_deleted_at
    ON customer(deleted_at)
    WHERE is_deleted = true;

CREATE INDEX IF NOT EXISTS idx_customer_note_customer_id
    ON customer_note(customer_id, company_id);

-- Product cleanup
CREATE INDEX IF NOT EXISTS idx_product_deleted_at
    ON product(deleted_at)
    WHERE is_deleted = true;

-- Stock movement: compound for product+company+date (stok hareketi sayfası)
CREATE INDEX IF NOT EXISTS idx_stock_movement_product_company_date
    ON stock_movement(product_id, company_id, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_stock_movement_deleted_at
    ON stock_movement(deleted_at)
    WHERE is_deleted = true;

-- Invoice payment cleanup
CREATE INDEX IF NOT EXISTS idx_invoice_payment_deleted_at
    ON invoice_payment(deleted_at)
    WHERE is_deleted = true;

-- Invoice line item: compound for invoice+company
CREATE INDEX IF NOT EXISTS idx_invoice_line_item_invoice_company
    ON invoice_line_item(invoice_id, company_id)
    WHERE is_deleted = false;
