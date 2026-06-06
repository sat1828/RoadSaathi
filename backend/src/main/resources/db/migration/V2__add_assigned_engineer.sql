ALTER TABLE hazard_reports ADD COLUMN assigned_engineer_id UUID REFERENCES users(id);
CREATE INDEX idx_hazard_engineer ON hazard_reports(assigned_engineer_id);
