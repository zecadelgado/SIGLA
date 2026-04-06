DROP TABLE IF EXISTS inventory_item;

CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    location VARCHAR(160) NOT NULL,
    cnpj VARCHAR(32) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    notes VARCHAR(2000)
);

CREATE TABLE IF NOT EXISTS customer_contacts (
    customer_id VARCHAR(64) NOT NULL,
    contact_name VARCHAR(120) NOT NULL,
    contact_role VARCHAR(80) NOT NULL,
    contact_value VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS employees (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    role VARCHAR(80) NOT NULL,
    contact VARCHAR(120) NOT NULL,
    status VARCHAR(24) NOT NULL
);

CREATE TABLE IF NOT EXISTS contracts (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    type VARCHAR(24) NOT NULL,
    service_frequency VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    renewal_rule VARCHAR(24) NOT NULL,
    alert_days_before_end INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS visit_schedules (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    contract_id VARCHAR(64),
    type VARCHAR(24) NOT NULL,
    scheduled_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL,
    notes VARCHAR(2000)
);

CREATE TABLE IF NOT EXISTS provided_services (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    contract_id VARCHAR(64),
    schedule_id VARCHAR(64),
    employee_id VARCHAR(64) NOT NULL,
    execution_date DATE NOT NULL,
    description VARCHAR(2000) NOT NULL,
    amount_charged NUMERIC(12,2) NOT NULL,
    payment_status VARCHAR(24) NOT NULL,
    signature_type VARCHAR(24) NOT NULL,
    signature_path VARCHAR(255),
    notes VARCHAR(2000)
);

CREATE TABLE IF NOT EXISTS service_attachments (
    service_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS certificates (
    id VARCHAR(64) PRIMARY KEY,
    service_provided_id VARCHAR(64) NOT NULL,
    issued_on DATE NOT NULL,
    valid_until DATE NOT NULL,
    status VARCHAR(24) NOT NULL,
    renewal_alert_days INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS financial_entries (
    id VARCHAR(64) PRIMARY KEY,
    entry_type VARCHAR(24) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    entry_date DATE NOT NULL,
    customer_id VARCHAR(64),
    service_provided_id VARCHAR(64),
    status VARCHAR(24) NOT NULL
);

CREATE TABLE IF NOT EXISTS financial_expenses (
    id VARCHAR(64) PRIMARY KEY,
    category VARCHAR(24) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    expense_date DATE NOT NULL,
    responsible VARCHAR(120) NOT NULL,
    notes VARCHAR(2000)
);

CREATE TABLE IF NOT EXISTS installment_plans (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    total_installments INTEGER NOT NULL,
    paid_installments INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL,
    next_due_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_items (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    quantity INTEGER NOT NULL,
    unit VARCHAR(24) NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_movements (
    item_id VARCHAR(64) NOT NULL,
    movement_id VARCHAR(64) NOT NULL,
    movement_type VARCHAR(24) NOT NULL,
    amount INTEGER NOT NULL,
    occurred_on DATE NOT NULL,
    handled_by VARCHAR(120),
    purchased_by VARCHAR(120),
    stored_by VARCHAR(120),
    notes VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS leads (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    contact VARCHAR(120) NOT NULL,
    origin VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL
);

CREATE TABLE IF NOT EXISTS lead_interactions (
    lead_id VARCHAR(64) NOT NULL,
    interaction_date DATE NOT NULL,
    channel VARCHAR(80) NOT NULL,
    notes VARCHAR(500) NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(80) PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    related_entity_id VARCHAR(64) NOT NULL,
    trigger_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL
);
