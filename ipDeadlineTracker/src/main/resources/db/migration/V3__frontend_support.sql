ALTER TABLE users ADD COLUMN display_name TEXT;

ALTER TABLE matters ADD COLUMN docket_number TEXT;

ALTER TABLE organizations ADD COLUMN reminder_offsets_days TEXT;

ALTER TABLE deadlines DROP CONSTRAINT IF EXISTS deadlines_status_check;
ALTER TABLE deadlines ADD CONSTRAINT deadlines_status_check CHECK (status IN ('OPEN', 'COMPLETED', 'MISSED', 'ARCHIVED'));

ALTER TABLE deadlines ADD COLUMN not_done_reason TEXT
    CHECK (not_done_reason IN ('WAITING_ON_CLIENT', 'WAITING_ON_OFFICE', 'NEED_MORE_TIME', 'INTERNAL_REVIEW', 'OTHER'));
