CREATE TABLE deadline_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL,
    version TEXT NOT NULL,
    office_code TEXT,
    chapter TEXT CHECK (chapter IN ('I', 'II', 'BOTH')),
    trigger_type TEXT NOT NULL CHECK (trigger_type IN ('EARLIEST_PRIORITY', 'FIRST_FILING', 'ISR_TRANSMITTAL', 'EARLIEST_PRIORITY_OR_IFD')),
    calculation_type TEXT NOT NULL CHECK (calculation_type IN ('ADD_MONTHS', 'ADD_DAYS', 'LATER_OF', 'EARLIEST_DATE', 'CUSTOM')),
    parameters_json JSONB NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    source_authority TEXT NOT NULL,
    source_reference TEXT NOT NULL,
    source_url TEXT,
    verified_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (code, office_code, version)
);

CREATE TABLE calculator_calculations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_code TEXT NOT NULL,
    rule_version TEXT NOT NULL,
    office_code TEXT,
    inputs JSONB NOT NULL,
    trigger_date DATE NOT NULL,
    calculated_date DATE NOT NULL,
    estimated BOOLEAN NOT NULL DEFAULT FALSE,
    trace JSONB NOT NULL,
    source_authority TEXT NOT NULL,
    source_reference TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Non-office rule families (Engineering Spec section 2)
INSERT INTO deadline_rules (code, version, trigger_type, calculation_type, parameters_json, effective_from, source_authority, source_reference, source_url, verified_at, active) VALUES
('PRIORITY_PERIOD', '2026.08', 'FIRST_FILING', 'ADD_MONTHS', '{"months": 12}', '2026-08-01', 'WIPO', 'PCT Rule 2.4', 'https://www.wipo.int/en/web/pct-system/texts/rules/r2', NOW(), TRUE),
('PCT_PUBLICATION', '2026.08', 'EARLIEST_PRIORITY', 'ADD_MONTHS', '{"months": 18}', '2026-08-01', 'WIPO', 'Receiving Office Guidelines 337', 'https://www.wipo.int/en/web/pct-system/texts/ro/ro337', NOW(), TRUE),
('PCT_ART19', '2026.08', 'EARLIEST_PRIORITY', 'LATER_OF', '{"priorityMonths": 16, "isrMonths": 2}', '2026-08-01', 'WIPO', 'PCT Rule 46.1', 'https://www.wipo.int/en/web/pct-system/texts/rules/r46', NOW(), TRUE),
('PCT_DEMAND', '2026.08', 'EARLIEST_PRIORITY', 'LATER_OF', '{"priorityMonths": 22, "isrMonths": 3}', '2026-08-01', 'WIPO', 'PCT Rule 54bis.1', 'https://www.wipo.int/ja/web/pct-system/texts/rules/r54bis', NOW(), TRUE);

-- PCT_NATIONAL_PHASE office configuration (Engineering Spec section 3)
INSERT INTO deadline_rules (code, version, office_code, chapter, trigger_type, calculation_type, parameters_json, effective_from, source_authority, source_reference, source_url, verified_at, active) VALUES
('PCT_NATIONAL_PHASE', '2026.08', 'EP', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 31}', '2026-08-01', 'EPO', 'EPC Rule 159(1)', 'https://www.epo.org/en/legal/epc/2020/r159.html', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'US', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 30}', '2026-08-01', 'USPTO', 'PCT Art. 22/39; 35 USC 371; 37 CFR 1.495', 'https://www.uspto.gov/web/offices/pac/mpep/s1893.html', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'JP', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 30}', '2026-08-01', 'WIPO', 'National phase table / JPO guidance', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'CN', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 30, "warningNote": "WIPO table lists a 32-month exceptional path for CN under certain conditions; this exception is not modeled and only the base 30-month result is returned"}', '2026-08-01', 'WIPO', 'National phase table (CN footnote)', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'IN', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 31}', '2026-08-01', 'WIPO', 'National phase table; Indian Rule 20', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'GB', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 31}', '2026-08-01', 'WIPO', 'National phase table / UKIPO guidance', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'DE', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 31}', '2026-08-01', 'WIPO', 'National phase table', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'AU', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 31}', '2026-08-01', 'WIPO', 'National phase table / IP Australia guidance', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'KR', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 31}', '2026-08-01', 'WIPO', 'National phase table / KIPO guidance', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'CA', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 30}', '2026-08-01', 'WIPO', 'National phase table / CIPO guidance', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'BR', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 30}', '2026-08-01', 'WIPO', 'National phase table', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE),
('PCT_NATIONAL_PHASE', '2026.08', 'MX', 'BOTH', 'EARLIEST_PRIORITY_OR_IFD', 'ADD_MONTHS', '{"months": 30}', '2026-08-01', 'WIPO', 'National phase table', 'https://www.wipo.int/en/web/pct-system/texts/time_limits', NOW(), TRUE);
